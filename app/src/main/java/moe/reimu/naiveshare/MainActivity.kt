package moe.reimu.naiveshare

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import moe.reimu.naiveshare.services.GattServerService
import moe.reimu.naiveshare.ui.theme.NaiveShareTheme
import moe.reimu.naiveshare.utils.ServiceState
import moe.reimu.naiveshare.utils.getReceiverFlags

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        enableEdgeToEdge()
        setContent {
            NaiveShareTheme {
                MainActivityContent()
            }
        }
    }

    fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                this, Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        if (Build.VERSION.SDK_INT <= 32) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }

            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }


        if (Build.VERSION.SDK_INT >= 31) {
            for (perm in listOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )) {
                if (ContextCompat.checkSelfPermission(
                        this, perm
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsToRequest.add(perm)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), 0)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray, deviceId: Int
    ) {
        for ((name, status) in permissions.zip(grantResults.toList())) {
            if (status == PackageManager.PERMISSION_GRANTED) {
                continue
            }

            when (name) {
                Manifest.permission.ACCESS_FINE_LOCATION -> {
                    Toast.makeText(this, "Location permission not granted", Toast.LENGTH_LONG)
                        .show()
                }

                Manifest.permission.NEARBY_WIFI_DEVICES -> {
                    Toast.makeText(this, "Nearby permission not granted", Toast.LENGTH_LONG).show()
                }

                Manifest.permission.BLUETOOTH_ADVERTISE -> {
                    Toast.makeText(this, "Bluetooth advertise not granted", Toast.LENGTH_LONG)
                        .show()
                }

                Manifest.permission.BLUETOOTH_SCAN -> {
                    Toast.makeText(this, "Bluetooth scan not granted", Toast.LENGTH_LONG).show()
                }

                Manifest.permission.BLUETOOTH_CONNECT -> {
                    Toast.makeText(this, "Bluetooth connect not granted", Toast.LENGTH_LONG).show()
                }

                else -> {
                    throw RuntimeException("What?")
                }
            }

            finish()
            return
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivityContent() {
    var checked by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val context = LocalContext.current
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == ServiceState.ACTION_UPDATE_RECEIVER_STATE) {
                    checked = intent.getBooleanExtra("isRunning", false)
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(ServiceState.ACTION_UPDATE_RECEIVER_STATE),
            getReceiverFlags()
        )
        context.sendBroadcast(ServiceState.getQueryIntent())

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val iconMod = Modifier
        .size(48.dp)
        .padding(end = 16.dp)

    Scaffold(topBar = {
        TopAppBar(title = { Text(text = stringResource(R.string.app_name)) }, actions = {
            IconButton(onClick = { /* do something */ }) {
                Icon(
                    imageVector = Icons.Filled.Settings, contentDescription = "Settings"
                )
            }
        })
    }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            item {
                DefaultCard(onClick = {
                    Toast.makeText(context, R.string.not_impl, Toast.LENGTH_SHORT).show()
                }) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            modifier = iconMod,
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.send),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.send_desc),
                            )
                        }
                    }
                }

            }
            item {
                DefaultCard {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_bluetooth_searching),
                            contentDescription = null,
                            modifier = iconMod,
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.discoverable),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.discoverable_desc),
                            )
                        }
                        Spacer(modifier = Modifier.weight(1.0f))
                        Switch(checked = checked, onCheckedChange = {
                            if (it) {
                                GattServerService.start(context)
                            } else {
                                GattServerService.stop(context)
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun DefaultCard(
    modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ), modifier = modifier.fillMaxWidth(), content = content
    )
}

@Composable
fun DefaultCard(
    onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick, colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ), modifier = modifier.fillMaxWidth(), content = content
    )
}
