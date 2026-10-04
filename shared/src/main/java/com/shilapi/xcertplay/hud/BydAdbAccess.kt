package com.shilapi.xcertplay.hud

import android.content.Context
import com.shilapi.xcertplay.adb.AdbKeys
import com.shilapi.xcertplay.adb.LocalAdb

/** What the settings page shows about the ADB link that DiPlay's optional BYD features need. */
object BydAdbAccess {
    enum class State { READY, NOT_APPROVED, ADB_OFF, PAIRING_ONLY }

    class Status(
        val state: State,
        val dashboardMode: BydClusterNaviMode? = null,
        val dashboardShowsMap: Boolean = true,
        val batteryPercent: Double? = null,
        val rangeKm: Int? = null,
    )

    /** Blocking: run off the main thread. [mayAsk] lets the car show its approval dialog for DiPlay's key. */
    fun check(context: Context, mayAsk: Boolean): Status {
        LocalAdb(AdbKeys.load(context)).use { adb ->
            val state = when (adb.connect(mayAsk)) {
                LocalAdb.Access.READY -> State.READY
                LocalAdb.Access.NOT_APPROVED -> State.NOT_APPROVED
                LocalAdb.Access.UNREACHABLE -> State.ADB_OFF
                LocalAdb.Access.UNSUPPORTED -> State.PAIRING_ONLY
            }
            if (state != State.READY) return Status(state)
            return readStatus(context, adb::shell)
        }
    }

    /** Read and publish the same battery data that the settings page reports as ready. */
    internal fun readStatus(context: Context, shell: (String) -> String?): Status {
        val mode = BydClusterNaviMode.parseRead(shell(BydClusterNaviMode.READ_COMMAND))
        val battery = BydBatteryStatus.read(context) { BydBattery.read(shell) }
        return Status(State.READY, mode, mode?.showsMap != false, battery?.percent, battery?.rangeKm)
    }
}
