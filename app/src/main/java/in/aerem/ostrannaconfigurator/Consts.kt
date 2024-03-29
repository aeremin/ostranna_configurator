package `in`.aerem.ostrannaconfigurator

import java.util.UUID

val BATTERY_LEVEL_UUID: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
val OSTRANNA_UUID: UUID = UUID.fromString("8ec87060-8865-4eca-82e0-2ea8e45e8221")
val COLOR_UUID: UUID = UUID.fromString("8ec87065-8865-4eca-82e0-2ea8e45e8221")
val BEEP_UUID: UUID = UUID.fromString("8ec87062-8865-4eca-82e0-2ea8e45e8221")
val BLINK_UUID: UUID = UUID.fromString("8ec87063-8865-4eca-82e0-2ea8e45e8221")

// See https://github.com/FergusInLondon/ELK-BLEDOM/blob/master/PROTCOL.md#communication
val ELK_BLEDOM_SERVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
val ELK_BLEDOM_CHARACTERISTIC_UUID: UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")