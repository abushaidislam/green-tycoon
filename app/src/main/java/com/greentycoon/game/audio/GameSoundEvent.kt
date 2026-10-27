package com.greentycoon.game.audio

sealed class GameSoundEvent {
    data object Water : GameSoundEvent()
    data object Fertilize : GameSoundEvent()
    data object Harvest : GameSoundEvent()
    data object CashEarned : GameSoundEvent()
    data object DoorKnock : GameSoundEvent()
    data object DoorOpen : GameSoundEvent()
    data object DoorClose : GameSoundEvent()
}
