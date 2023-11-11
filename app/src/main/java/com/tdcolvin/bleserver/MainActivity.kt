package com.tdcolvin.bleserver

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tdcolvin.bleserver.ui.theme.BLEServerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val allPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    }
    else {
        arrayOf(
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH
        )
    }

    private fun haveAllPermissions(context: Context): Boolean {
        return allPermissions
            .all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BLEServerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ServerScreen()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    fun ServerScreen(viewModel: ServerViewModel = viewModel()) {
        val context = LocalContext.current
        var allPermissionsGranted by remember {
            mutableStateOf (haveAllPermissions(context))
        }

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        Column {
            if (allPermissionsGranted) {
                ServerStatus(
                    serverRunning = uiState.serverRunning,
                    onStartServer = { viewModel.startServer() },
                    onStopServer = { viewModel.stopServer() }
                )
            }
            else {
                val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { granted ->
                    allPermissionsGranted = granted.values.all { it }
                }
                Button(onClick = { launcher.launch(allPermissions)}) {
                    Text("Grant Permission")
                }
            }
        }
    }

    @Composable
    fun ServerStatus(serverRunning: Boolean, onStartServer: () -> Unit, onStopServer: () -> Unit) {
        if (serverRunning) {
            Text("Server running")
            Button(onClick = onStopServer) {
                Text("Stop server")
            }
        }
        else {
            Text("Server not running")
            Button(onClick = onStartServer) {
                Text("Start server")
            }
        }
    }
}

class ServerViewModel(application: Application): AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ServerUIState())
    val uiState = _uiState.asStateFlow()

    private val server: BluetoothCTFServer = BluetoothCTFServer(application)

    @RequiresPermission(allOf = [PERMISSION_BLUETOOTH_ADVERTISE, PERMISSION_BLUETOOTH_CONNECT])
    fun startServer() {
        viewModelScope.launch {
            server.startServer()
            _uiState.update { it.copy(serverRunning = true) }
        }
    }
    @RequiresPermission(allOf = [PERMISSION_BLUETOOTH_ADVERTISE, PERMISSION_BLUETOOTH_CONNECT])
    fun stopServer() {
        viewModelScope.launch {
            server.stopServer()
            _uiState.update { it.copy(serverRunning = false) }
        }
    }
}

data class ServerUIState(
    val serverRunning: Boolean = false
)