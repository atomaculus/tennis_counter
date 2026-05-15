package com.example.tenniscounter.mobile.garmin

import android.app.Activity
import android.content.Context
import android.util.Log
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.exception.InvalidStateException
import com.garmin.android.connectiq.exception.ServiceUnavailableException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GarminSdkState {
    UNINITIALIZED,
    INITIALIZING,
    READY,
    NOT_INSTALLED,
    SERVICE_NOT_FOUND,
    ERROR
}

data class GarminConnectionState(
    val sdkState: GarminSdkState = GarminSdkState.UNINITIALIZED,
    val knownDevices: List<GarminDeviceInfo> = emptyList(),
    val connectedDeviceCount: Int = 0
)

data class GarminDeviceInfo(
    val identifier: Long,
    val friendlyName: String,
    val status: IQDevice.IQDeviceStatus
)

class GarminConnectivityManager(private val appContext: Context) {

    private val connectIQ: ConnectIQ = ConnectIQ.getInstance(appContext, ConnectIQ.IQConnectType.WIRELESS)
    private val app: IQApp = IQApp(GarminConstants.APP_ID)
    private val router: GarminMessageRouter = GarminMessageRouter(appContext)

    private val _connectionState = MutableStateFlow(GarminConnectionState())
    val connectionState: StateFlow<GarminConnectionState> = _connectionState.asStateFlow()

    private val registeredDeviceEvents = mutableSetOf<Long>()
    private val registeredAppEvents = mutableSetOf<Long>()

    private val sdkListener = object : ConnectIQ.ConnectIQListener {
        override fun onSdkReady() {
            Log.i(TAG, "Connect IQ SDK ready for appId=${GarminConstants.APP_ID}")
            _connectionState.value = _connectionState.value.copy(sdkState = GarminSdkState.READY)
            attachToKnownDevices()
        }

        override fun onInitializeError(errStatus: ConnectIQ.IQSdkErrorStatus) {
            val mapped = when (errStatus) {
                ConnectIQ.IQSdkErrorStatus.GCM_NOT_INSTALLED -> GarminSdkState.NOT_INSTALLED
                ConnectIQ.IQSdkErrorStatus.SERVICE_ERROR -> GarminSdkState.SERVICE_NOT_FOUND
                else -> GarminSdkState.ERROR
            }
            Log.w(TAG, "Connect IQ SDK init error: $errStatus -> $mapped")
            _connectionState.value = _connectionState.value.copy(sdkState = mapped)
        }

        override fun onSdkShutDown() {
            Log.i(TAG, "Connect IQ SDK shut down")
            _connectionState.value = GarminConnectionState(sdkState = GarminSdkState.UNINITIALIZED)
            registeredDeviceEvents.clear()
            registeredAppEvents.clear()
        }
    }

    private val deviceEventListener = ConnectIQ.IQDeviceEventListener { device, status ->
        Log.d(TAG, "Device ${device.friendlyName} status=$status")
        registerDevice(device)
        refreshDeviceList()
    }

    private val appEventListener = ConnectIQ.IQApplicationEventListener { device, _, message, status ->
        if (status != ConnectIQ.IQMessageStatus.SUCCESS) {
            Log.w(TAG, "Message from ${device.friendlyName} arrived with status=$status")
            return@IQApplicationEventListener
        }
        for (item in message) {
            Log.d(
                TAG,
                "App event from ${device.friendlyName}: itemType=${item?.javaClass?.name} raw=$item"
            )
            val envelope = GarminPayloadCodec.asEnvelope(item)
            if (envelope == null) {
                Log.w(TAG, "Unrecognized message item type=${item?.javaClass?.simpleName}")
                continue
            }
            router.route(device, app, envelope)
        }
    }

    fun initialize(hostContext: Context? = null) {
        if (_connectionState.value.sdkState == GarminSdkState.INITIALIZING ||
            _connectionState.value.sdkState == GarminSdkState.READY) {
            return
        }
        val initContext = hostContext ?: appContext
        val showSdkErrors = hostContext is Activity && !hostContext.isFinishing && !hostContext.isDestroyed
        Log.i(TAG, "Initializing Connect IQ SDK for appId=${GarminConstants.APP_ID}")
        _connectionState.value = _connectionState.value.copy(sdkState = GarminSdkState.INITIALIZING)
        runCatching {
            connectIQ.initialize(initContext, showSdkErrors, sdkListener)
        }.onFailure {
            Log.e(TAG, "Connect IQ SDK initialize() threw", it)
            _connectionState.value = _connectionState.value.copy(sdkState = GarminSdkState.ERROR)
        }
    }

    fun shutdown() {
        runCatching { connectIQ.shutdown(appContext) }
            .onFailure { Log.w(TAG, "Connect IQ SDK shutdown failed", it) }
        registeredDeviceEvents.clear()
        registeredAppEvents.clear()
        _connectionState.value = GarminConnectionState(sdkState = GarminSdkState.UNINITIALIZED)
    }

    fun connectedDevices(): List<IQDevice> {
        if (_connectionState.value.sdkState != GarminSdkState.READY) return emptyList()
        return runCatching { connectIQ.connectedDevices ?: emptyList() }
            .getOrElse {
                Log.w(TAG, "connectedDevices threw", it)
                emptyList()
            }
    }

    fun sendMessage(message: Any, listener: ConnectIQ.IQSendMessageListener? = null) {
        val targets = connectedDevices()
        if (targets.isEmpty()) {
            Log.d(TAG, "No connected Garmin devices, skipping send")
            return
        }
        for (device in targets) {
            runCatching {
                connectIQ.sendMessage(device, app, message) { d, a, status ->
                    Log.d(TAG, "sendMessage to ${d.friendlyName} status=$status")
                    listener?.onMessageStatus(d, a, status)
                }
            }.onFailure {
                Log.w(TAG, "sendMessage to ${device.friendlyName} threw", it)
            }
        }
    }

    fun appReference(): IQApp = app

    fun connectIQ(): ConnectIQ = connectIQ

    private fun attachToKnownDevices() {
        val known = runCatching { connectIQ.knownDevices ?: emptyList() }
            .getOrElse {
                Log.w(TAG, "knownDevices threw", it)
                emptyList()
            }
        Log.i(
            TAG,
            "Known Garmin devices count=${known.size} values=${
                known.joinToString { "${it.friendlyName}:${it.deviceIdentifier}:${it.status}" }
            }"
        )
        for (device in known) {
            registerDevice(device)
        }
        refreshDeviceList()
    }

    private fun registerDevice(device: IQDevice) {
        if (!registeredDeviceEvents.contains(device.deviceIdentifier)) {
            runCatching {
                connectIQ.registerForDeviceEvents(device, deviceEventListener)
            }.onSuccess {
                registeredDeviceEvents.add(device.deviceIdentifier)
                Log.i(TAG, "registerForDeviceEvents OK for ${device.friendlyName} (${device.deviceIdentifier})")
            }.onFailure {
                Log.w(TAG, "registerForDeviceEvents failed for ${device.friendlyName}", it)
            }
        }

        if (!registeredAppEvents.contains(device.deviceIdentifier)) {
            runCatching {
                connectIQ.registerForAppEvents(device, app, appEventListener)
            }.onSuccess {
                registeredAppEvents.add(device.deviceIdentifier)
                Log.i(
                    TAG,
                    "registerForAppEvents OK for ${device.friendlyName} (${device.deviceIdentifier}) appId=${GarminConstants.APP_ID}"
                )
            }.onFailure {
                when (it) {
                    is InvalidStateException,
                    is ServiceUnavailableException ->
                        Log.w(TAG, "registerForAppEvents not ready for ${device.friendlyName}: ${it.message}")
                    else -> Log.w(TAG, "registerForAppEvents failed for ${device.friendlyName}", it)
                }
            }
        }
    }

    private fun refreshDeviceList() {
        val known = runCatching { connectIQ.knownDevices ?: emptyList() }.getOrElse { emptyList() }
        for (device in known) {
            registerDevice(device)
        }
        val infos = known.map { GarminDeviceInfo(it.deviceIdentifier, it.friendlyName, it.status) }
        val connectedCount = infos.count { it.status == IQDevice.IQDeviceStatus.CONNECTED }
        Log.d(TAG, "refreshDeviceList connected=$connectedCount known=${infos.size}")
        _connectionState.value = _connectionState.value.copy(
            knownDevices = infos,
            connectedDeviceCount = connectedCount
        )
    }

    private companion object {
        const val TAG = "GarminConnectivity"
    }
}
