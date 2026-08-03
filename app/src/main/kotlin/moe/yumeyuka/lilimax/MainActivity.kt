package moe.yumeyuka.lilimax

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import moe.yumeyuka.lilimax.bluetooth.PermissionHelper
import moe.yumeyuka.lilimax.ui.LilimaxApp
import moe.yumeyuka.lilimax.ui.ViewModel

class MainActivity : ComponentActivity() {

    private lateinit var bluetoothManager: BluetoothManager
    private val viewModel: ViewModel by viewModels {
        ViewModel.Factory((application as App).bluetoothManager)
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val ok = grants.values.all { it }
            Toast.makeText(this, if (ok) "蓝牙权限已获取" else "缺少蓝牙权限", Toast.LENGTH_SHORT).show()
            if (ok) viewModel.connectToHeadset()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as App
        app.initBluetooth()
        bluetoothManager = app.bluetoothManager

        requestBluetoothPermissions()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LilimaxApp(viewModel)
                }
            }
        }
    }

    private fun requestBluetoothPermissions() {
        val missing = PermissionHelper.missingPermissions(this)
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}
