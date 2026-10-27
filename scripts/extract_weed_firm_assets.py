import os
import sys
import zipfile
import struct
import shutil

DEFAULT_APK_PATH = r"c:\Users\ASUS\Downloads\weed-firm.apk"
PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
DRAWABLE_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "res", "drawable")
RAW_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "res", "raw")
FONTS_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "fonts")


def parse_string_pool(data, offset):
    chunk_type, header_size, chunk_size = struct.unpack('<HHI', data[offset:offset+8])
    string_count, style_count, flags, strings_start, styles_start = struct.unpack('<IIIII', data[offset+8:offset+28])
    is_utf8 = bool(flags & (1 << 8))
    offsets = struct.unpack(f'<{string_count}I', data[offset+28:offset+28+string_count*4])
    pool_data = data[offset+strings_start:offset+chunk_size]
    strings = []
    for off in offsets:
        if is_utf8:
            p = off
            if pool_data[p] & 0x80:
                p += 2
            else:
                p += 1
            if pool_data[p] & 0x80:
                p += 2
            else:
                p += 1
            end = pool_data.find(b'\x00', p)
            s = pool_data[p:end].decode('utf-8', errors='replace')
        else:
            p = off
            u16len = struct.unpack('<H', pool_data[p:p+2])[0]
            p += 2
            s = pool_data[p:p+u16len*2].decode('utf-16le', errors='replace')
        strings.append(s)
    return strings, offset + chunk_size


def extract_assets(apk_path: str = None):
    if not apk_path:
        if len(sys.argv) > 1 and sys.argv[1]:
            apk_path = sys.argv[1]
        elif "WEED_FIRM_APK" in os.environ:
            apk_path = os.environ["WEED_FIRM_APK"]
        else:
            apk_path = DEFAULT_APK_PATH

    if not os.path.exists(apk_path):
        print(f"Error: APK not found at '{apk_path}'.", file=sys.stderr)
        sys.exit(1)

    os.makedirs(DRAWABLE_DIR, exist_ok=True)
    os.makedirs(RAW_DIR, exist_ok=True)
    os.makedirs(FONTS_DIR, exist_ok=True)

    # Clean up any previously extracted third-party ad mediation assets
    if os.path.exists(DRAWABLE_DIR):
        for fname in os.listdir(DRAWABLE_DIR):
            if fname.startswith('applovin_') or fname.startswith('googleg_'):
                try:
                    os.remove(os.path.join(DRAWABLE_DIR, fname))
                except OSError:
                    pass

    with zipfile.ZipFile(apk_path, 'r') as z:
        data = z.read('resources.arsc')

        global_strings, off = parse_string_pool(data, 12)
        pkg_type, pkg_hdr_sz, pkg_sz, pkg_id = struct.unpack('<HHI I', data[off:off+12])
        type_strings_off, last_pub_type, key_strings_off, last_pub_key = struct.unpack('<IIII', data[off+268:off+284])
        type_strings, _ = parse_string_pool(data, off + type_strings_off)
        key_strings, _ = parse_string_pool(data, off + key_strings_off)

        curr = off + pkg_hdr_sz
        resource_map = {}
        while curr < off + pkg_sz:
            ctype, chdr_sz, csz = struct.unpack('<HHI', data[curr:curr+8])
            if ctype == 0x0201:
                tid = data[curr+8]
                type_name = type_strings[tid - 1] if 0 < tid <= len(type_strings) else f'type_{tid}'
                entry_count = struct.unpack('<I', data[curr+12:curr+16])[0]
                entries_start = struct.unpack('<I', data[curr+16:curr+20])[0]
                entry_offsets = struct.unpack(f'<{entry_count}I', data[curr+chdr_sz:curr+chdr_sz+entry_count*4])
                for entry_idx, eoff in enumerate(entry_offsets):
                    if eoff != 0xFFFFFFFF:
                        epos = curr + entries_start + eoff
                        esz, flags, key_idx = struct.unpack('<HHI', data[epos:epos+8])
                        key_name = key_strings[key_idx] if key_idx < len(key_strings) else f'key_{key_idx}'
                        if not (flags & 0x0001):
                            v_size, v_res0, v_dataType, v_data = struct.unpack('<HBBI', data[epos+8:epos+16])
                            if v_dataType == 0x03 and v_data < len(global_strings):
                                val = global_strings[v_data]
                                resource_map[(type_name, key_name)] = val
            curr += csz

        # 1. Extract drawables
        extracted_drawables = 0
        for (t, k), path in resource_map.items():
            if t == 'drawable' and (path.endswith('.png') or path.endswith('.jpg')):
                res_name = k.lower().replace('-', '_').replace('.', '_')
                if (res_name.startswith('$') or
                        res_name.startswith('abc_') or
                        res_name.startswith('common_') or
                        res_name.startswith('applovin_') or
                        res_name.startswith('googleg_')):
                    continue
                ext = os.path.splitext(path)[1].lower()
                dest_file = os.path.join(DRAWABLE_DIR, f"{res_name}{ext}")
                try:
                    content = z.read(path)
                    with open(dest_file, 'wb') as f:
                        f.write(content)
                    extracted_drawables += 1
                except KeyError:
                    pass
        print(f"Extracted {extracted_drawables} drawables into {DRAWABLE_DIR}")

        # 2. Extract raw sounds
        extracted_sounds = 0
        for (t, k), path in resource_map.items():
            if t == 'raw' and path.endswith('.ogg'):
                res_name = k.lower().replace('-', '_').replace('.', '_')
                dest_file = os.path.join(RAW_DIR, f"{res_name}.ogg")
                try:
                    content = z.read(path)
                    with open(dest_file, 'wb') as f:
                        f.write(content)
                    extracted_sounds += 1
                except KeyError:
                    pass

        # Ensure aliases for audio mentioned in specs and Task 2 (water.ogg, ds.ogg)
        for alias_src, alias_dst in [('watering.ogg', 'water.ogg'), ('fertilize.ogg', 'ds.ogg')]:
            src = os.path.join(RAW_DIR, alias_src)
            dst = os.path.join(RAW_DIR, alias_dst)
            if os.path.exists(src):
                shutil.copy2(src, dst)
                extracted_sounds += 1

        # Ensure alias for cut shears badge
        cut_src = os.path.join(DRAWABLE_DIR, "cut_anim_0.png")
        cut_dst = os.path.join(DRAWABLE_DIR, "cut.png")
        if os.path.exists(cut_src):
            shutil.copy2(cut_src, cut_dst)

        print(f"Extracted {extracted_sounds} sounds into {RAW_DIR}")

        # 3. Extract fonts
        extracted_fonts = 0
        for entry in z.namelist():
            if entry.startswith('assets/fonts/') and entry.endswith('.ttf'):
                font_name = os.path.basename(entry)
                with open(os.path.join(FONTS_DIR, font_name), 'wb') as f:
                    f.write(z.read(entry))
                print(f"Extracted font: {font_name}")
                extracted_fonts += 1

    return {
        "drawables": extracted_drawables,
        "sounds": extracted_sounds,
        "fonts": extracted_fonts
    }


if __name__ == '__main__':
    extract_assets()
