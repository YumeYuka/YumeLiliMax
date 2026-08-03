package moe.yumeyuka.lilimax

import android.app.Application

class App : Application() {

    lateinit var bluetoothManager: BluetoothManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        initBluetooth()
    }

    fun initBluetooth() {
        if (!::bluetoothManager.isInitialized) {
            bluetoothManager = BluetoothManager(this)
        }
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
