package com.royalenfield.vehicleadvertiser

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import java.util.Arrays
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var mBluetoothGattServer: BluetoothGattServer
    private val BATTERY_SERVICE_UUID = UUID
        .fromString("0000180F-0000-1000-8000-00805f9b34fb")

    private val BATTERY_LEVEL_UUID = UUID
        .fromString("00002A19-0000-1000-8000-00805f9b34fb")
/////////////////////////////////////

    private val SPEED_SERVICE_UUID = UUID
        .fromString("9eeac52e-47cc-4bf7-b25c-0a1fa92a3695")

    private val SPEED_LEVEL_UUID = UUID
        .fromString("7afab1f8-24cc-4142-a63a-d235ab49c0a1")

///////////////////////////////////////
    private val CHARACTERISTIC_USER_DESCRIPTION_UUID = UUID
        .fromString("00002901-0000-1000-8000-00805f9b34fb")
    private val CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID
        .fromString("00002902-0000-1000-8000-00805f9b34fb")


    private val SPEED_CHARACTERISTIC_USER_DESCRIPTION_UUID = UUID
    .fromString("1239fcd9-7560-4b1d-b8ac-a75f614dfb75")

private val SPEED_CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID
    .fromString("4730dc40-5c9f-43dc-af07-31c1fcffea5f")
    /////////////////////
    private val INITIAL_BATTERY_LEVEL = 50
    private val BATTERY_LEVEL_MAX = 100
    private val BATTERY_LEVEL_DESCRIPTION = "The current charge level of a " +
            "battery. 100% represents fully charged while 0% represents fully discharged."


    ////////////////
private val INITIAL_SPEED_LEVEL = 50
    private val SPEED_LEVEL_MAX = 100
    private val SPEED_LEVEL_DESCRIPTION = "The current speed of the vehicle."+
            "speed"

//////////private val ALLOWED_DEVICE_ADDRESS = "AA:BB:CC:DD:EE:FF"

    ///////////////////////
///private val ALLOWED_DEVICE_ADDRESS = "AA:BB:CC:DD:EE:FF"

    private var mBatteryService: BluetoothGattService? = null
    private var mSpeedService: BluetoothGattService? = null
    private var mBatteryLevelCharacteristic: BluetoothGattCharacteristic? = null
    private var mSpeedLevelCharacteristic: BluetoothGattCharacteristic? = null
    private var mBluetoothGattService: BluetoothGattService? = null
    private var mAdvData: AdvertiseData? = null
    private var mAdvScanResponse: AdvertiseData? = null
    private var batteryServiceData : AdvertiseData? = null
    private var mAdvScanResponseBattery : AdvertiseData? = null
    private var  speedServiceData : AdvertiseData? = null

    private var mAdvScanResponseSpeed : AdvertiseData? = null
    private var mAdvSettings: AdvertiseSettings? = null
    private lateinit var mAdvertiser: BluetoothLeAdvertiser
    private lateinit var mGattServer: BluetoothGattServer
    private var mBluetoothManager: BluetoothManager? = null
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private val mBluetoothDevices: HashSet<BluetoothDevice>? = null

    private lateinit var mAdvStatus: TextView
    private var mConnectionStatus: TextView? = null
    private lateinit var mStartAdvertise: Button

    private val mAdvCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e("BLE_DEVICE_STATUS", "Not broadcasting: $errorCode")
            val statusText: Int
            when (errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED -> {
                    statusText = R.string.status_advertising
                    Log.w("BLE_DEVICE_STATUS", "App was already advertising")
                }

                ADVERTISE_FAILED_DATA_TOO_LARGE -> statusText = R.string.status_advDataTooLarge
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> statusText = R.string.status_advFeatureUnsupported

                ADVERTISE_FAILED_INTERNAL_ERROR -> statusText = R.string.status_advInternalError
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> statusText = R.string.status_advTooManyAdvertisers

                else -> {
                    statusText = R.string.status_notAdvertising
                    Log.wtf("BLE_DEVICE_STATUS", "Unhandled error: $errorCode")
                }
            }
            mAdvStatus!!.setText(statusText)
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.v("BLE_DEVICE_STATUS", "Broadcasting")
            Log.d("BLE_DEVICE_STATUS", "Advertisement started successfully")
            mAdvStatus!!.setText(R.string.status_advertising)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)


        mAdvStatus = findViewById<TextView>(R.id.tv_info)
        mStartAdvertise = findViewById<Button>(R.id.bt_start)
        mConnectionStatus = findViewById<TextView>(R.id.tv_status)



        mBluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = mBluetoothManager!!.getAdapter()

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
        {
            return
        }
        mBluetoothAdapter.setName("RoyalEnfieldEV");

        mBatteryService = BluetoothGattService(
            BATTERY_SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY)


        mBatteryLevelCharacteristic = BluetoothGattCharacteristic(
            BATTERY_LEVEL_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ)
        Log.d("CharacteristicHandle", "Battery Level Characteristic handle: ${mBatteryLevelCharacteristic!!.instanceId}")

        mBatteryLevelCharacteristic!!.addDescriptor(
            getClientCharacteristicConfigurationDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION_UUID)
        )

        mBatteryLevelCharacteristic!!.addDescriptor(
            getCharacteristicUserDescriptionDescriptor(BATTERY_LEVEL_DESCRIPTION, CHARACTERISTIC_USER_DESCRIPTION_UUID)
        )


        mBatteryLevelCharacteristic!!.setValue(
            85,
            BluetoothGattCharacteristic.FORMAT_UINT8,  /* offset */0)
        mBatteryService!!.addCharacteristic(mBatteryLevelCharacteristic)

        mSpeedService = BluetoothGattService(
            SPEED_SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        mSpeedLevelCharacteristic = BluetoothGattCharacteristic(
    SPEED_LEVEL_UUID,
    BluetoothGattCharacteristic.PROPERTY_READ or
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
    BluetoothGattCharacteristic.PERMISSION_READ
)
        Log.d("CharacteristicHandle", "Speed Level Characteristic handle: ${mSpeedLevelCharacteristic!!.instanceId}")


        mSpeedLevelCharacteristic!!.addDescriptor(
            getClientCharacteristicConfigurationDescriptor(SPEED_CLIENT_CHARACTERISTIC_CONFIGURATION_UUID)
        )

        mSpeedLevelCharacteristic!!.addDescriptor(
            getCharacteristicUserDescriptionDescriptor(SPEED_LEVEL_DESCRIPTION, SPEED_CHARACTERISTIC_USER_DESCRIPTION_UUID)
        )
        mSpeedLevelCharacteristic!!.setValue(
            79,
            BluetoothGattCharacteristic.FORMAT_UINT8,
            0  // Offset
        )
       // mSpeedLevelCharacteristic!!.setValue(79, BluetoothGattCharacteristic.FORMAT_UINT8, 0)

        mSpeedService!!.addCharacteristic(mSpeedLevelCharacteristic)

        mStartAdvertise.setOnClickListener {

            // Advertise Battery Service
            mGattServer = mBluetoothManager!!.openGattServer(this, mGattServerCallback)
            mGattServer.addService(mBatteryService)

            mAdvSettings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build()

            batteryServiceData = AdvertiseData.Builder()
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(ParcelUuid(BATTERY_SERVICE_UUID))
                .build()

           mAdvScanResponseBattery = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                // Add any additional data specific to the battery service
                .build()

            mAdvertiser = mBluetoothAdapter.bluetoothLeAdvertiser
            mAdvertiser.startAdvertising(mAdvSettings, batteryServiceData, mAdvScanResponseBattery, mAdvCallback)
            //mAdvertiser.startAdvertising(mAdvSettings, mAdvData, null, mAdvCallback)

            mGattServer.addService(mSpeedService)

            mAdvSettings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build()

             speedServiceData = AdvertiseData.Builder()
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(ParcelUuid(SPEED_SERVICE_UUID))
                .build()
            mAdvScanResponseSpeed = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                // Add any additional data specific to the speed service
                .build()
            mAdvertiser = mBluetoothAdapter.bluetoothLeAdvertiser
            mAdvertiser.startAdvertising(mAdvSettings, speedServiceData, mAdvScanResponseSpeed, mAdvCallback)
            //mAdvertiser.startAdvertising(mAdvSettings, mAdvData, null, mAdvCallback)


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                return@setOnClickListener


            }else{


                if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                    Log.d("BLE_ADVERTISING", "Multiple advertisement is supported on this device")
                    mAdvertiser = mBluetoothAdapter.bluetoothLeAdvertiser
                    mAdvertiser.startAdvertising(
                        mAdvSettings,
                        batteryServiceData,
                        mAdvScanResponseBattery,
                        mAdvCallback
                    )
                    mAdvertiser.startAdvertising(
                        mAdvSettings,
                        speedServiceData,
                        mAdvScanResponseSpeed,
                        mAdvCallback
                    )
                    mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser()
                } else {
                    Log.d("BLE_ADVERTISING", "Multiple advertisement is not supported on this device")
                    mAdvStatus.setText(R.string.status_noLeAdv)
                    Toast.makeText(this, "Multiple advertisement is not supported on this device", Toast.LENGTH_SHORT).show()
                }
            }

        }

ensureBleFeaturesAvailable()

    }
    /*
private val mGattServerCallback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {
    override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
        super.onConnectionStateChange(device, status, newState)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                if (device.address == ALLOWED_DEVICE_ADDRESS) {
                    mBluetoothDevices?.add(device)
                    updateConnectedDevicesStatus()
                    Log.v("BLE_DEVICE_STATUS", "Connected to device: " + device.address)
                } else {
                    // Not the desired device, disconnect
                    Log.e("BLE_DEVICE_STATUS", "Unauthorized device connected: " + device.address)
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                         return
                    }
                    mBluetoothGattServer?.cancelConnection(device)
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Unauthorized device connected", Toast.LENGTH_LONG).show()
                    }
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                mBluetoothDevices?.remove(device)
                updateConnectedDevicesStatus()
                Log.v("BLE_DEVICE_STATUS", "Disconnected from device: " + device.address)
            }
        } else {
            mBluetoothDevices?.remove(device)
            updateConnectedDevicesStatus()
            val errorMessage = getString(R.string.status_errorWhenConnecting) + ": " + status
            runOnUiThread {
                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
            }
            Log.e("BLE_DEVICE_STATUS", "Error when connecting: $status")
        }
    }
    */

    private val mGattServerCallback: BluetoothGattServerCallback =
        object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int
            ) {
                super.onConnectionStateChange(device, status, newState)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        mBluetoothDevices?.add(device)
                        updateConnectedDevicesStatus()
                        Log.v(
                            "BLE_DEVICE_STATUS",
                            "Connected to device: " + device.address
                        )
                    } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                        mBluetoothDevices?.remove(device)
                        updateConnectedDevicesStatus()
                        Log.v(
                            "BLE_DEVICE_STATUS",
                            "Disconnected from device"
                        )
                    }
                } else {
                    mBluetoothDevices?.remove(device)
                    updateConnectedDevicesStatus()
                     val errorMessage =
                        getString(R.string.status_errorWhenConnecting) + ": " + status
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                    Log.e(
                        "BLE_DEVICE_STATUS",
                        "Error when connecting: $status"
                    )
                }
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
                Log.d("BLE_DEVICE_STATUS", "Device tried to read characteristic: ${characteristic.uuid}")
                Log.d("BLE_DEVICE_STATUS", "Value: ${Arrays.toString(characteristic.value)}")

                val value: ByteArray
                when (characteristic.uuid) {
                    BATTERY_LEVEL_UUID -> {
                        // Respond with the value of the battery level characteristic
                        val batteryLevel = mBatteryLevelCharacteristic?.value
                        mGattServer.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            offset,
                            batteryLevel
                        )
                    }
                    SPEED_LEVEL_UUID -> {
                        // Respond with the value of the speed level characteristic
                        val speedLevel = mSpeedLevelCharacteristic?.value
                        mGattServer.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            offset,
                            speedLevel
                        )
                    }
                    else -> {
                        // Handle other characteristic read requests
                        mGattServer.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            offset,
                            null
                        )
                    }
                }
                if (offset != 0) {
                    // Offset is not supported, send an error response
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    mGattServer!!.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_INVALID_OFFSET,
                        offset,  /* value (optional) */
                        null
                    )
                    return
                }

                // Send the response with the characteristic value
                mGattServer!!.sendResponse(
                    device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, null
                )
            }

            override fun onNotificationSent(device: BluetoothDevice, status: Int) {
                super.onNotificationSent(device, status)
                Log.v(
                    "BLE_DEVICE_STATUS",
                    "Notification sent. Status: $status"
                )
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                super.onCharacteristicWriteRequest(
                    device, requestId, characteristic, preparedWrite,
                    responseNeeded, offset, value
                )
                Log.v(
                    "BLE_DEVICE_STATUS",
                    "Characteristic Write request: " + Arrays.toString(value)
                )
                val status: Int =
                    writeCharacteristic(characteristic, offset, value)
                if (responseNeeded) {
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        return
                    }
                    mGattServer!!.sendResponse(
                        device, requestId, status,  /* No need to respond with an offset */
                        0,  /* No need to respond with a value */
                        null
                    )
                }
            }

          override fun onDescriptorReadRequest(
                device: BluetoothDevice, requestId: Int,
                offset: Int, descriptor: BluetoothGattDescriptor
            ) {
                super.onDescriptorReadRequest(device, requestId, offset, descriptor)
                Log.d(
                    "BLE_DEVICE_STATUS",
                    "Device tried to read descriptor: " + descriptor.uuid
                )
                Log.d(
                    "BLE_DEVICE_STATUS",
                    "Value: " + Arrays.toString(descriptor.value)
                )

              val value: ByteArray
              if (descriptor.uuid == BATTERY_LEVEL_UUID) {
                  // Check if the requested characteristic is the speed level characteristic
                  value = byteArrayOf(mBatteryLevelCharacteristic!!.value[0]) // Get the value of the speed level characteristic
              } else if (descriptor.uuid == SPEED_LEVEL_UUID) {
                  // Check if the requested characteristic is the battery level characteristic
                  value = byteArrayOf(mSpeedLevelCharacteristic!!.value[0]) // Get the value of the battery level characteristic
              } else {
                  // Handle other characteristic read requests
                  return
              }

                if (offset != 0) {
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        return
                    }
                    mGattServer!!.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_INVALID_OFFSET,
                        offset,  /* value (optional) */
                        null
                    )
                    return
                }
                mGattServer!!.sendResponse(
                    device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                    descriptor.value
                )
            }
////////////////////
override fun onDescriptorWriteRequest(
    device: BluetoothDevice,
    requestId: Int,
    descriptor: BluetoothGattDescriptor,
    preparedWrite: Boolean,
    responseNeeded: Boolean,
    offset: Int,
    value: ByteArray
) {
    super.onDescriptorWriteRequest(
        device, requestId, descriptor, preparedWrite, responseNeeded,
        offset, value
    )
    Log.v(
        "BLE_DEVICE_STATUS",
        "Descriptor Write Request " + descriptor.uuid + " " + Arrays.toString(value)
    )
    var status = BluetoothGatt.GATT_SUCCESS
    when (descriptor.uuid) {
        CLIENT_CHARACTERISTIC_CONFIGURATION_UUID -> {
            val characteristic = descriptor.characteristic
            val supportsNotifications = characteristic.properties and
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
            val supportsIndications = characteristic.properties and
                    BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
            if (!(supportsNotifications || supportsIndications)) {
                status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
            } else if (value.size != 2) {
                status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH
            } else if (Arrays.equals(
                    value,
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                )
            ) {
                status = BluetoothGatt.GATT_SUCCESS
                notificationsDisabled(characteristic)
                descriptor.value = value
            } else if (supportsNotifications &&
                Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            ) {
                status = BluetoothGatt.GATT_SUCCESS
                notificationsEnabled(
                    characteristic,
                    false /* indicate */
                )
                descriptor.value = value
            } else if (supportsIndications &&
                Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
            ) {
                status = BluetoothGatt.GATT_SUCCESS
                notificationsEnabled(
                    characteristic,
                    true /* indicate */
                )
                descriptor.value = value
            } else {
                status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
            }
        }
        SPEED_CHARACTERISTIC_USER_DESCRIPTION_UUID -> {
            // Handle write request for the Speed Service characteristic user description descriptor
            val characteristic = descriptor.characteristic
            val supportsNotifications = characteristic.properties and
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
            val supportsIndications = characteristic.properties and
                    BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
            if (!(supportsNotifications || supportsIndications)) {
                status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
            } else if (value.size != 2) {
                status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH
            } else if (Arrays.equals(
                    value,
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                )
            ) {
                status = BluetoothGatt.GATT_SUCCESS
                notificationsDisabled(characteristic)
                descriptor.value = value
            } else if (supportsNotifications &&
                Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            ) {
                status = BluetoothGatt.GATT_SUCCESS
                notificationsEnabled(
                    characteristic,
                    false /* indicate */
                )
                descriptor.value = value
            } else if (supportsIndications &&
                Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
            ) {
                status = BluetoothGatt.GATT_SUCCESS
                notificationsEnabled(
                    characteristic,
                    true /* indicate */
                )
                descriptor.value = value
            } else {
                status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
            }
        }
        else -> {
            status = BluetoothGatt.GATT_SUCCESS
            descriptor.value = value
        }
    }

    if (responseNeeded) {
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return
        }
        mGattServer!!.sendResponse(
            device, requestId, status,  /* No need to respond with offset */
            0,  /* No need to respond with a value */
            null
        )
    }
}
///////////////////
        }

    private fun updateConnectedDevicesStatus() {
       if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }else{
           val message =  (getString(R.string.status_devicesConnected) + " "
                   + mBluetoothManager!!.getConnectedDevices(BluetoothGattServer.GATT).size)
           runOnUiThread { mConnectionStatus!!.text = message }
       }

    }

fun getClientCharacteristicConfigurationDescriptor(uuid: UUID): BluetoothGattDescriptor {
    val descriptor = BluetoothGattDescriptor(
        uuid,
        BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
    )
    descriptor.value = byteArrayOf(0x01, 0x00)
    //descriptor.value = byteArrayOf(0, 0)
    return descriptor
}

    fun getCharacteristicUserDescriptionDescriptor(defaultValue: String, uuid: UUID): BluetoothGattDescriptor {
        val descriptor = BluetoothGattDescriptor(
            uuid,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        try {
            descriptor.value = defaultValue.toByteArray(Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e("DescriptorError", "Failed to set descriptor value: ${e.message}")
        }
        return descriptor
    }

    ///////////
    fun getClientCharacteristicConfigurationDescriptor(): BluetoothGattDescriptor? {
        val descriptor = BluetoothGattDescriptor(
          CLIENT_CHARACTERISTIC_CONFIGURATION_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        descriptor.value = byteArrayOf(0, 0)
        return descriptor
    }

    fun getCharacteristicUserDescriptionDescriptor(defaultValue: String): BluetoothGattDescriptor? {
        val descriptor = BluetoothGattDescriptor(
            CHARACTERISTIC_USER_DESCRIPTION_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        try {
            descriptor.value = defaultValue.toByteArray(charset("UTF-8"))
        } finally {
            return descriptor
        }
    }

    fun notificationsEnabled(characteristic: BluetoothGattCharacteristic?, indicate: Boolean) {
        throw UnsupportedOperationException("Method notificationsEnabled not overridden")
    }
    fun notificationsDisabled(characteristic: BluetoothGattCharacteristic?) {
        throw java.lang.UnsupportedOperationException("Method notificationsDisabled not overridden")
    }
    fun writeCharacteristic(
        characteristic: BluetoothGattCharacteristic?,
        offset: Int,
        value: ByteArray?
    ): Int {
        throw java.lang.UnsupportedOperationException("Method writeCharacteristic not overridden")
    }


    private fun ensureBleFeaturesAvailable() {

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetoothNotSupported, Toast.LENGTH_LONG).show()
            Log.e(
               "BLE_DEVICE_STATUS",
                "Bluetooth not supported"
            )
            finish()
        } else if (!mBluetoothAdapter.isEnabled) {
            // Make sure bluetooth is enabled.
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                return
            }
            startActivityForResult(
                enableBtIntent,
                1
            )
        }
    }
}