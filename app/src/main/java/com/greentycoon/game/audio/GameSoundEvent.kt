package com.greentycoon.game.audio

sealed class GameSoundEvent {
    object Water : GameSoundEvent()
    object Fertilize : GameSoundEvent()
    object Harvest : GameSoundEvent()
    object CashEarned : GameSoundEvent()
    object DoorKnock : GameSoundEvent()
    object DoorOpen : GameSoundEvent()
    object DoorClose : GameSoundEvent()
}
