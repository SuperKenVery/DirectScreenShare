package com.example.directscreenshare

/*
 * A screen that allows selecting a peer and connect to it.
 *
 * If we are sharing the screen, we should be the group owner. All viewers connect to thsi owner.
 * In this way, we could broadcast or multicast the video stream, without any forwarding.
 *
 * Also, using manager.createGroup allows legacy devices that doens't support Wi-Fi Direct to join.
 */

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import androidx.core.content.ContextCompat.registerReceiver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

private val TAG="SelectPeerScreen"

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun SelectPeerScreen(isSharingScreen: MutableState<Boolean>, navController: NavController) {
    val activity = LocalActivity.current ?: return
    if(!checkPermissions(activity)) {
        navController.popBackStack()
    }

    val manager = remember { activity.applicationContext.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager }
    val channel = remember { manager.initialize(activity, Looper.getMainLooper(), null) }
    val peers = remember { mutableStateListOf<WifiP2pDevice>() }

    val intentFilter = remember {
        IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
    }
    val viewerCannotBeGOAlert = remember { mutableStateOf(false) }
    val receiver = remember { WDBroadcastListener(
        activity, manager, channel, peers,
        { info ->
            if(isSharingScreen.value==false and info.isGroupOwner==true) {
                viewerCannotBeGOAlert.value = true
                // TODO: Disconnect? Don't know how to do it.
            }

            if(isSharingScreen.value) {
                navController.navigate(Route.ShareScreen.route)
            }else{
                navController.navigate(Route.ViewScreen.route)
            }
        }
    ) }


    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        registerReceiver(activity, receiver, intentFilter, RECEIVER_NOT_EXPORTED)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        activity.unregisterReceiver(receiver)
    }

    DisposableEffect(manager, channel) {
        manager.discoverPeers(channel, object: WifiP2pManager.ActionListener {
            override fun onSuccess() {            }
            override fun onFailure(p0: Int) {
                Toast.makeText(activity, "Failed to scan peers", Toast.LENGTH_SHORT).show()
            }
        })

        onDispose {  }
    }

    Scaffold(
        Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                if(isSharingScreen.value) {
                    Column {
                        Text("Select a viewer")
                        Text("To broadcast your screen, connect the first peer from here (rather than letting it connect you). ", style= MaterialTheme.typography.bodySmall)
                    }
                }else{
                    Column {
                        Text("Select the sharer")
                        Text("If you are the first viewer, don't connect from here, but let the screen sharer connect you.", style= MaterialTheme.typography.bodySmall)
                    }
                }
            })
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item { HorizontalDivider() }
            items(peers) { peer ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(peer.deviceName)
                    Spacer(Modifier.weight(1.0f))
                    Button({ connectDevice(peer, manager, channel, activity, isSharingScreen) }) {
                        Text("Connect")
                    }
                }
                HorizontalDivider()
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Text("Searching for peers...", color = MaterialTheme.colorScheme.secondary)
                }
                HorizontalDivider()


            }
        }
    }

    if(viewerCannotBeGOAlert.value) {
        AlertDialog(
            onDismissRequest = { viewerCannotBeGOAlert.value = false },
            title = { Text("Viewer cannot be group owner") },
            text = {
                Text("Please initiate connection from the screen sharer.")
            },
            confirmButton = {
                TextButton(onClick = {viewerCannotBeGOAlert.value = false}) { Text("OK") }
            }
        )
    }
}

class WDBroadcastListener(
    val activity: Activity,
    val manager: WifiP2pManager,
    val channel: Channel,
    val deviceList: SnapshotStateList<WifiP2pDevice>,
    val onConnection: (WifiP2pInfo) -> Unit,
): BroadcastReceiver() {
    val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val newDevices = peerList.deviceList
        if(newDevices!=deviceList){
            deviceList.clear()
            deviceList.addAll(newDevices)
        }
    }


    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                val enabled = state==WifiP2pManager.WIFI_P2P_STATE_ENABLED
                Log.i(TAG, "WiFi Direct enabled: $enabled")
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                manager.requestPeers(channel, peerListListener)
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                Log.i(TAG, "Connection state changed")

                val networkInfo: NetworkInfo? = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO) as? NetworkInfo
                if(networkInfo?.isConnected == true) {
                    manager.requestConnectionInfo(channel, object : WifiP2pManager.ConnectionInfoListener {
                        override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
                            val msg="Connected! GO: ${info?.isGroupOwner}, GOip: ${info?.groupOwnerAddress}"
                            Log.i(TAG, msg)

                            info?.let { it ->
                                onConnection(it)
                            }
                        }

                    })
                }
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                Log.i(TAG, "WiFi Direct device changed")
            }
        }


    }
}

@SuppressLint("MissingPermission")
fun connectDevice(device: WifiP2pDevice, manager: WifiP2pManager, channel: Channel, context: Context, isSharingScreen: MutableState<Boolean>) {
    val config = WifiP2pConfig().apply {
        deviceAddress = device.deviceAddress
        wps.setup = WpsInfo.PBC
    }

    val listener = object: WifiP2pManager.ActionListener {
        override fun onSuccess() {
            manager.requestGroupInfo(channel) { group ->
                if (group==null) return@requestGroupInfo
                val groupPassword = group.passphrase
                Log.i(TAG, "group password: ${groupPassword}")
            }
        }

        override fun onFailure(reason: Int) {
            val reason = when(reason) {
                WifiP2pManager.P2P_UNSUPPORTED -> "P2P Unsupported"
                WifiP2pManager.ERROR -> "Error"
                WifiP2pManager.BUSY -> "Busy"
                else -> "Unknown reason"
            }
            val msg = "Connection failed. Reason: $reason"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            Log.e(TAG, msg)
        }

    }

    if(isSharingScreen.value){
        manager.createGroup(channel, listener)
    }else{
        manager.connect(channel, config, listener)
    }
}

@Preview(device = Devices.PIXEL_7)
@Composable
fun preview() {
    val isSharingScreen = rememberSaveable { mutableStateOf<Boolean>(true) }
    val navController = rememberNavController()
    SelectPeerScreen(isSharingScreen, navController)
}
