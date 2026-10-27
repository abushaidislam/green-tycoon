package com.greentycoon.game.ui

import com.greentycoon.game.audio.SoundManager
import com.greentycoon.game.model.GameState
import com.greentycoon.game.model.PlayerProfile
import com.greentycoon.game.model.StashInventory
import com.greentycoon.game.ui.components.formatCash
import com.greentycoon.game.ui.theme.CashDisplay
import com.greentycoon.game.ui.theme.ChalkBody
import com.greentycoon.game.ui.theme.ChalkdusterFontFamily
import com.greentycoon.game.ui.theme.TweedFontFamily
import com.greentycoon.game.ui.theme.TweedHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticTopHudTest {

    @Test
    fun testCashFormattingTabularCurrency() {
        assertEquals("$0.00", formatCash(0.0))
        assertEquals("$250.00", formatCash(250.0))
        assertEquals("$1,250.00", formatCash(1250.0))
        assertEquals("$10,000.50", formatCash(10000.50))
        assertEquals("$1,000,000.00", formatCash(1000000.0))
    }

    @Test
    fun testPlayerRespectAndXpCalculations() {
        val player = PlayerProfile(cash = 500.0, level = 2, currentXp = 50L, respectScore = 42)
        assertEquals(42, player.respect)
        assertEquals(42, player.respectScore)
        assertEquals(280L, player.xpForNextLevel) // 2 * 100 + 4 * 20 = 280
        val expectedFraction = 50f / 280f
        assertEquals(expectedFraction, player.xpProgressFraction, 0.001f)
    }

    @Test
    fun testStashInventorySumForTopHudPill() {
        val inventory = StashInventory(
            harvestedStock = mapOf(
                "sour_diesel" to 15,
                "northern_lights" to 30,
                "og_kush" to 5
            )
        )
        val stashSum = inventory.harvestedStock.values.sum()
        assertEquals(50, stashSum)
        assertEquals(inventory.totalWeedGrams, stashSum)
    }

    @Test
    fun testSoundManagerMuteToggleForTopHud() {
        SoundManager.setMuted(false)
        assertFalse(SoundManager.isMuted())

        val mutedAfterToggle = SoundManager.toggleMute()
        assertTrue(mutedAfterToggle)
        assertTrue(SoundManager.isMuted())

        val unmutedAfterToggle = SoundManager.toggleMute()
        assertFalse(unmutedAfterToggle)
        assertFalse(SoundManager.isMuted())
    }

    @Test
    fun testTypographyStylesConfigured() {
        assertNotNull(TweedFontFamily)
        assertNotNull(ChalkdusterFontFamily)
        assertNotNull(TweedHeader)
        assertNotNull(ChalkBody)
        assertNotNull(CashDisplay)
        assertEquals(TweedFontFamily, TweedHeader.fontFamily)
        assertEquals(ChalkdusterFontFamily, ChalkBody.fontFamily)
        assertEquals(TweedFontFamily, CashDisplay.fontFamily)
    }
}
