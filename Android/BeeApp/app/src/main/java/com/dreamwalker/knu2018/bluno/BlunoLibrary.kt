package com.dreamwalker.knu2018.bluno

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.support.v7.app.AppCompatCallback
import android.support.v7.view.ActionMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast

import java.util.ArrayList

abstract class BlunoLibrary : Activity(), AppCompatCallback {
    internal var mBluetoothLeService: BluetoothLeService? = null
    private var mGattCharacteristics = ArrayList<ArrayList<BluetoothGattCharacteristic>>()
    private var mLeDeviceListAdapter: LeDeviceListAdapter? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mScanning = false
    var mConnectionState = connectionStateEnum.isNull

    var mConnected = false


    internal var mScanDeviceDialog: AlertDialog
    private val mainContext = this
    private val mHandler = Handler()

    private var mBaudrate = 115200    //set the default baud rate to 115200
    private val mPassword = "AT+PASSWOR=DFRobot\r\n"
    private var mBaudrateBuffer = "AT+CURRUART=$mBaudrate\r\n"
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null


    private val mConnectingOverTimeRunnable = Runnable {
        if (mConnectionState == connectionStateEnum.isConnecting)
            mConnectionState = connectionStateEnum.isToScan
        onConectionStateChange(mConnectionState)
        mBluetoothLeService!!.close()
    }

    private val mDisonnectingOverTimeRunnable = Runnable {
        if (mConnectionState == connectionStateEnum.isDisconnecting)
            mConnectionState = connectionStateEnum.isToScan
        onConectionStateChange(mConnectionState)
        mBluetoothLeService!!.close()
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private val mGattUpdateReceiver = object : BroadcastReceiver() {
        @SuppressLint("DefaultLocale")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            println("mGattUpdateReceiver->onReceive->action=" + action!!)
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true
                mHandler.removeCallbacks(mConnectingOverTimeRunnable)

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false
                mConnectionState = connectionStateEnum.isToScan
                onConectionStateChange(mConnectionState)
                mHandler.removeCallbacks(mDisonnectingOverTimeRunnable)
                mBluetoothLeService!!.close()
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                for (gattService in mBluetoothLeService!!.getSupportedGattServices()) {
                    println("ACTION_GATT_SERVICES_DISCOVERED  " + gattService.getUuid().toString())
                }
                getGattServices(mBluetoothLeService!!.getSupportedGattServices())
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (mSCharacteristic === mModelNumberCharacteristic) {
                    if (intent.getStringExtra(BluetoothLeService.EXTRA_DATA).toUpperCase().startsWith("DF BLUNO")) {
                        mBluetoothLeService!!.setCharacteristicNotification(mSCharacteristic, false)
                        mSCharacteristic = mCommandCharacteristic
                        mSCharacteristic!!.setValue(mPassword)
                        mBluetoothLeService!!.writeCharacteristic(mSCharacteristic)
                        mSCharacteristic!!.setValue(mBaudrateBuffer)
                        mBluetoothLeService!!.writeCharacteristic(mSCharacteristic)
                        mSCharacteristic = mSerialPortCharacteristic
                        mBluetoothLeService!!.setCharacteristicNotification(mSCharacteristic, true)
                        mConnectionState = connectionStateEnum.isConnected
                        onConectionStateChange(mConnectionState)

                    } else {
                        Toast.makeText(mainContext, "Please select DFRobot devices", Toast.LENGTH_SHORT).show()
                        mConnectionState = connectionStateEnum.isToScan
                        onConectionStateChange(mConnectionState)
                    }
                } else if (mSCharacteristic === mSerialPortCharacteristic) {
                    onSerialReceived(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
                }


                println("displayData " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA))

                //            	mPlainProtocol.mReceivedframe.append(intent.getStringExtra(BluetoothLeService.EXTRA_DATA)) ;
                //            	System.out.print("mPlainProtocol.mReceivedframe:");
                //            	System.out.println(mPlainProtocol.mReceivedframe.toString());


            }
        }
    }

    // Code to manage Service lifecycle.
    internal var mServiceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            println("mServiceConnection onServiceConnected")
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).getService()
            if (!mBluetoothLeService!!.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
                (mainContext as Activity).finish()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            println("mServiceConnection onServiceDisconnected")
            mBluetoothLeService = null
        }
    }

    // Device scan callback.
    private val mLeScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        (mainContext as Activity).runOnUiThread {
            println("mLeScanCallback onLeScan run ")
            mLeDeviceListAdapter!!.addDevice(device)
            mLeDeviceListAdapter!!.notifyDataSetChanged()
        }
    }

    enum class connectionStateEnum {
        isNull, isScanning, isToScan, isConnecting, isConnected, isDisconnecting
    }


    //	public BlunoLibrary(Context theContext) {
    //
    //		mainContext=theContext;
    //	}

    abstract fun onConectionStateChange(theconnectionStateEnum: connectionStateEnum)

    abstract fun onSerialReceived(theString: String)

    fun serialSend(theString: String) {
        if (mConnectionState == connectionStateEnum.isConnected) {
            mSCharacteristic!!.setValue(theString)
            mBluetoothLeService!!.writeCharacteristic(mSCharacteristic)
        }
    }


    //	byte[] mBaudrateBuffer={0x32,0x00,(byte) (mBaudrate & 0xFF),(byte) ((mBaudrate>>8) & 0xFF),(byte) ((mBaudrate>>16) & 0xFF),0x00};;


    fun serialBegin(baud: Int) {
        mBaudrate = baud
        mBaudrateBuffer = "AT+CURRUART=$mBaudrate\r\n"
    }


    internal class ViewHolder {
        var deviceName: TextView? = null
        var deviceAddress: TextView? = null
    }

    override fun onSupportActionModeStarted(mode: ActionMode) {
        //let's leave this empty, for now
    }

    override fun onSupportActionModeFinished(mode: ActionMode) {
        // let's leave this empty, for now
    }


    fun onCreateProcess() {
        if (!initiate()) {
            Toast.makeText(mainContext, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show()
            (mainContext as Activity).finish()
        }


        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)

        // Initializes list view adapter.
        mLeDeviceListAdapter = LeDeviceListAdapter()
        // Initializes and show the scan Device Dialog
        mScanDeviceDialog = AlertDialog.Builder(mainContext)
                .setTitle("BLE Device Scan...").setAdapter(mLeDeviceListAdapter, DialogInterface.OnClickListener { dialog, which ->
                    val device = mLeDeviceListAdapter!!.getDevice(which) ?: return@OnClickListener
                    scanLeDevice(false)

                    if (device.name == null || device.address == null) {
                        mConnectionState = connectionStateEnum.isToScan
                        onConectionStateChange(mConnectionState)
                    } else {

                        println("onListItemClick " + device.name.toString())

                        println("Device Name:" + device.name + "   " + "Device Name:" + device.address)

                        mDeviceName = device.name.toString()
                        mDeviceAddress = device.address.toString()

                        if (mBluetoothLeService!!.connect(mDeviceAddress)) {
                            Log.d(TAG, "Connect request success")
                            mConnectionState = connectionStateEnum.isConnecting
                            onConectionStateChange(mConnectionState)
                            mHandler.postDelayed(mConnectingOverTimeRunnable, 10000)
                        } else {
                            Log.d(TAG, "Connect request fail")
                            mConnectionState = connectionStateEnum.isToScan
                            onConectionStateChange(mConnectionState)
                        }
                    }
                })
                .setOnCancelListener {
                    println("mBluetoothAdapter.stopLeScan")

                    mConnectionState = connectionStateEnum.isToScan
                    onConectionStateChange(mConnectionState)
                    mScanDeviceDialog.dismiss()

                    scanLeDevice(false)
                }.create()

    }


    fun onResumeProcess() {
        println("BlUNOActivity onResume")
        // Ensures Bluetooth is enabled on the device. If Bluetooth is not
        // currently enabled,
        // fire an intent to display a dialog asking the user to grant
        // permission to enable it.
        if (!mBluetoothAdapter!!.isEnabled) {
            if (!mBluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(
                        BluetoothAdapter.ACTION_REQUEST_ENABLE)
                (mainContext as Activity).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }


        mainContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())

    }


    fun onPauseProcess() {
        println("BLUNOActivity onPause")
        scanLeDevice(false)
        mainContext.unregisterReceiver(mGattUpdateReceiver)
        mLeDeviceListAdapter!!.clear()
        mConnectionState = connectionStateEnum.isToScan
        onConectionStateChange(mConnectionState)
        mScanDeviceDialog.dismiss()
        if (mBluetoothLeService != null) {
            mBluetoothLeService!!.disconnect()
            mHandler.postDelayed(mDisonnectingOverTimeRunnable, 10000)

            //			mBluetoothLeService.close();
        }
        mSCharacteristic = null

    }


    fun onStopProcess() {
        println("MiUnoActivity onStop")
        if (mBluetoothLeService != null) {
            //			mBluetoothLeService.disconnect();
            //            mHandler.postDelayed(mDisonnectingOverTimeRunnable, 10000);
            mHandler.removeCallbacks(mDisonnectingOverTimeRunnable)
            mBluetoothLeService!!.close()
        }
        mSCharacteristic = null
    }

    fun onDestroyProcess() {
        mainContext.unbindService(mServiceConnection)
        mBluetoothLeService = null
    }

    fun onActivityResultProcess(requestCode: Int, resultCode: Int, data: Intent) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            (mainContext as Activity).finish()
            return
        }
    }

    internal fun initiate(): Boolean {
        // Use this check to determine whether BLE is supported on the device.
        // Then you can
        // selectively disable BLE-related features.
        if (!mainContext.packageManager.hasSystemFeature(
                        PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false
        }

        // Initializes a Bluetooth adapter. For API level 18 and above, get a
        // reference to
        // BluetoothAdapter through BluetoothManager.
        val bluetoothManager = mainContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        // Checks if Bluetooth is supported on the device.
        return if (mBluetoothAdapter == null) {
            false
        } else true
    }

    internal fun buttonScanOnClickProcess() {
        when (mConnectionState) {
            BlunoLibrary.connectionStateEnum.isNull -> {
                mConnectionState = connectionStateEnum.isScanning
                onConectionStateChange(mConnectionState)
                scanLeDevice(true)
                mScanDeviceDialog.show()
            }
            BlunoLibrary.connectionStateEnum.isToScan -> {
                mConnectionState = connectionStateEnum.isScanning
                onConectionStateChange(mConnectionState)
                scanLeDevice(true)
                mScanDeviceDialog.show()
            }
            BlunoLibrary.connectionStateEnum.isScanning -> {
            }

            BlunoLibrary.connectionStateEnum.isConnecting -> {
            }
            BlunoLibrary.connectionStateEnum.isConnected -> {
                mBluetoothLeService!!.disconnect()
                mHandler.postDelayed(mDisonnectingOverTimeRunnable, 10000)

                //			mBluetoothLeService.close();
                mConnectionState = connectionStateEnum.isDisconnecting
                onConectionStateChange(mConnectionState)
            }
            BlunoLibrary.connectionStateEnum.isDisconnecting -> {
            }

            else -> {
            }
        }


    }

    internal fun scanLeDevice(enable: Boolean) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.

            println("mBluetoothAdapter.startLeScan")

            if (mLeDeviceListAdapter != null) {
                mLeDeviceListAdapter!!.clear()
                mLeDeviceListAdapter!!.notifyDataSetChanged()
            }

            if (!mScanning) {
                mScanning = true
                mBluetoothAdapter!!.startLeScan(mLeScanCallback)
            }
        } else {
            if (mScanning) {
                mScanning = false
                mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
            }
        }
    }

    private fun getGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String? = null
        mModelNumberCharacteristic = null
        mSerialPortCharacteristic = null
        mCommandCharacteristic = null
        mGattCharacteristics = ArrayList()

        // Loops through available GATT Services.
        for (gattService in gattServices) {
            uuid = gattService.uuid.toString()
            println("displayGattServices + uuid=$uuid")

            val gattCharacteristics = gattService.characteristics
            val charas = ArrayList<BluetoothGattCharacteristic>()

            // Loops through available Characteristics.
            for (gattCharacteristic in gattCharacteristics) {
                charas.add(gattCharacteristic)
                uuid = gattCharacteristic.uuid.toString()
                if (uuid == ModelNumberStringUUID) {
                    mModelNumberCharacteristic = gattCharacteristic
                    println("mModelNumberCharacteristic  " + mModelNumberCharacteristic!!.uuid.toString())
                } else if (uuid == SerialPortUUID) {
                    mSerialPortCharacteristic = gattCharacteristic
                    println("mSerialPortCharacteristic  " + mSerialPortCharacteristic!!.uuid.toString())
                    //                    updateConnectionState(R.string.comm_establish);
                } else if (uuid == CommandUUID) {
                    mCommandCharacteristic = gattCharacteristic
                    println("mSerialPortCharacteristic  " + mSerialPortCharacteristic!!.uuid.toString())
                    //                    updateConnectionState(R.string.comm_establish);
                }
            }
            mGattCharacteristics.add(charas)
        }

        if (mModelNumberCharacteristic == null || mSerialPortCharacteristic == null || mCommandCharacteristic == null) {
            Toast.makeText(mainContext, "Please select DFRobot devices", Toast.LENGTH_SHORT).show()
            mConnectionState = connectionStateEnum.isToScan
            onConectionStateChange(mConnectionState)
        } else {
            mSCharacteristic = mModelNumberCharacteristic
            mBluetoothLeService!!.setCharacteristicNotification(mSCharacteristic, true)
            mBluetoothLeService!!.readCharacteristic(mSCharacteristic)
        }

    }

    private inner class LeDeviceListAdapter : BaseAdapter() {
        private val mLeDevices: ArrayList<BluetoothDevice>
        private val mInflator: LayoutInflater

        init {
            mLeDevices = ArrayList()
            mInflator = (mainContext as Activity).layoutInflater
        }

        fun addDevice(device: BluetoothDevice) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device)
            }
        }

        fun getDevice(position: Int): BluetoothDevice? {
            return mLeDevices[position]
        }

        fun clear() {
            mLeDevices.clear()
        }

        override fun getCount(): Int {
            return mLeDevices.size
        }

        override fun getItem(i: Int): Any {
            return mLeDevices[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
            var view = view
            val viewHolder: ViewHolder
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null)
                viewHolder = ViewHolder()
                viewHolder.deviceAddress = view!!.findViewById(R.id.device_address) as TextView
                viewHolder.deviceName = view.findViewById(R.id.device_name) as TextView
                println("mInflator.inflate  getView")
                view.tag = viewHolder
            } else {
                viewHolder = view.tag as ViewHolder
            }

            val device = mLeDevices[i]
            val deviceName = device.name
            if (deviceName != null && deviceName.length > 0)
                viewHolder.deviceName!!.text = deviceName
            else
                viewHolder.deviceName!!.setText(R.string.unknown_device)
            viewHolder.deviceAddress!!.text = device.address

            return view
        }
    }

    companion object {

        private val TAG = BlunoLibrary::class.java!!.getSimpleName()

        val SerialPortUUID = "0000dfb1-0000-1000-8000-00805f9b34fb"
        val CommandUUID = "0000dfb2-0000-1000-8000-00805f9b34fb"
        val ModelNumberStringUUID = "00002a24-0000-1000-8000-00805f9b34fb"

        private var mSCharacteristic: BluetoothGattCharacteristic? = null
        private var mModelNumberCharacteristic: BluetoothGattCharacteristic? = null
        private var mSerialPortCharacteristic: BluetoothGattCharacteristic? = null
        private var mCommandCharacteristic: BluetoothGattCharacteristic? = null
        private val REQUEST_ENABLE_BT = 1

        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
            return intentFilter
        }
    }
}
