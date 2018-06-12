/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dreamwalker.knu2018.bluno

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

import java.io.UnsupportedEncodingException


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
class BluetoothLeService : Service() {

    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    internal var mBluetoothGatt: BluetoothGatt? = null
    var mBluetoothDeviceAddress: String
    var mConnectionState = STATE_DISCONNECTED
    //Show that Characteristic is writing or not.
    private var mIsWritingCharacteristic = false
    //ring buffer
    private val mCharacteristicRingBuffer = RingBuffer<BluetoothGattCharacteristicHelper>(8)
    //    public final static UUID UUID_HEART_RATE_MEASUREMENT =
    //            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private val mGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            println("BluetoothGattCallback----onConnectionStateChange$newState")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED
                mConnectionState = STATE_CONNECTED
                broadcastUpdate(intentAction)
                Log.i(TAG, "Connected to GATT server.")
                // Attempts to discover services after successful connection.
                if (mBluetoothGatt!!.discoverServices()) {
                    Log.i(TAG, "Attempting to start service discovery:")

                } else {
                    Log.i(TAG, "Attempting to start service discovery:not success")

                }


            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED
                mConnectionState = STATE_DISCONNECTED
                Log.i(TAG, "Disconnected from GATT server.")
                broadcastUpdate(intentAction)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            println("onServicesDiscovered $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            //this block should be synchronized to prevent the function overloading
            synchronized(this) {
                //CharacteristicWrite success
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    println("onCharacteristicWrite success:" + String(characteristic.value))
                    if (mCharacteristicRingBuffer.isEmpty()) {
                        mIsWritingCharacteristic = false
                    } else {
                        val bluetoothGattCharacteristicHelper = mCharacteristicRingBuffer.next()
                        if (bluetoothGattCharacteristicHelper.mCharacteristicValue.length > MAX_CHARACTERISTIC_LENGTH) {
                            try {
                                bluetoothGattCharacteristicHelper.mCharacteristic.setValue(bluetoothGattCharacteristicHelper.mCharacteristicValue.substring(0, MAX_CHARACTERISTIC_LENGTH).toByteArray(charset("ISO-8859-1")))

                            } catch (e: UnsupportedEncodingException) {
                                // this should never happen because "US-ASCII" is hard-coded.
                                throw IllegalStateException(e)
                            }


                            if (mBluetoothGatt!!.writeCharacteristic(bluetoothGattCharacteristicHelper.mCharacteristic)) {
                                println("writeCharacteristic init " + String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":success")
                            } else {
                                println("writeCharacteristic init " + String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":failure")
                            }
                            bluetoothGattCharacteristicHelper.mCharacteristicValue = bluetoothGattCharacteristicHelper.mCharacteristicValue.substring(MAX_CHARACTERISTIC_LENGTH)
                        } else {
                            try {
                                bluetoothGattCharacteristicHelper.mCharacteristic.setValue(bluetoothGattCharacteristicHelper.mCharacteristicValue.toByteArray(charset("ISO-8859-1")))
                            } catch (e: UnsupportedEncodingException) {
                                // this should never happen because "US-ASCII" is hard-coded.
                                throw IllegalStateException(e)
                            }

                            if (mBluetoothGatt!!.writeCharacteristic(bluetoothGattCharacteristicHelper.mCharacteristic)) {
                                println("writeCharacteristic init " + String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":success")
                            } else {
                                println("writeCharacteristic init " + String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":failure")
                            }
                            bluetoothGattCharacteristicHelper.mCharacteristicValue = ""

                            //	            			System.out.print("before pop:");
                            //	            			System.out.println(mCharacteristicRingBuffer.size());
                            mCharacteristicRingBuffer.pop()
                            //	            			System.out.print("after pop:");
                            //	            			System.out.println(mCharacteristicRingBuffer.size());
                        }
                    }
                } else if (status == WRITE_NEW_CHARACTERISTIC) {
                    if (!mCharacteristicRingBuffer.isEmpty() && mIsWritingCharacteristic == false) {
                        val bluetoothGattCharacteristicHelper = mCharacteristicRingBuffer.next()
                        if (bluetoothGattCharacteristicHelper.mCharacteristicValue.length > MAX_CHARACTERISTIC_LENGTH) {

                            try {
                                bluetoothGattCharacteristicHelper.mCharacteristic.setValue(bluetoothGattCharacteristicHelper.mCharacteristicValue.substring(0, MAX_CHARACTERISTIC_LENGTH).toByteArray(charset("ISO-8859-1")))
                            } catch (e: UnsupportedEncodingException) {
                                // this should never happen because "US-ASCII" is hard-coded.
                                throw IllegalStateException(e)
                            }

                            if (mBluetoothGatt!!.writeCharacteristic(bluetoothGattCharacteristicHelper.mCharacteristic)) {
                                println("writeCharacteristic init " + String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":success")
                            } else {
                                println("writeCharacteristic init " + String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":failure")
                            }
                            bluetoothGattCharacteristicHelper.mCharacteristicValue = bluetoothGattCharacteristicHelper.mCharacteristicValue.substring(MAX_CHARACTERISTIC_LENGTH)
                        } else {
                            try {
                                bluetoothGattCharacteristicHelper.mCharacteristic.setValue(bluetoothGattCharacteristicHelper.mCharacteristicValue.toByteArray(charset("ISO-8859-1")))
                            } catch (e: UnsupportedEncodingException) {
                                // this should never happen because "US-ASCII" is hard-coded.
                                throw IllegalStateException(e)
                            }


                            if (mBluetoothGatt!!.writeCharacteristic(bluetoothGattCharacteristicHelper.mCharacteristic)) {
                                println("writeCharacteristic init " + String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":success")
                                //	            	        	System.out.println((byte)bluetoothGattCharacteristicHelper.mCharacteristic.getValue()[0]);
                                //	            	        	System.out.println((byte)bluetoothGattCharacteristicHelper.mCharacteristic.getValue()[1]);
                                //	            	        	System.out.println((byte)bluetoothGattCharacteristicHelper.mCharacteristic.getValue()[2]);
                                //	            	        	System.out.println((byte)bluetoothGattCharacteristicHelper.mCharacteristic.getValue()[3]);
                                //	            	        	System.out.println((byte)bluetoothGattCharacteristicHelper.mCharacteristic.getValue()[4]);
                                //	            	        	System.out.println((byte)bluetoothGattCharacteristicHelper.mCharacteristic.getValue()[5]);

                            } else {
                                println("writeCharacteristic init " + String(bluetoothGattCharacteristicHelper.mCharacteristic.getValue()) + ":failure")
                            }
                            bluetoothGattCharacteristicHelper.mCharacteristicValue = ""

                            //		            			System.out.print("before pop:");
                            //		            			System.out.println(mCharacteristicRingBuffer.size());
                            mCharacteristicRingBuffer.pop()
                            //		            			System.out.print("after pop:");
                            //		            			System.out.println(mCharacteristicRingBuffer.size());
                        }
                    }

                    mIsWritingCharacteristic = true

                    //clear the buffer to prevent the lock of the mIsWritingCharacteristic
                    if (mCharacteristicRingBuffer.isFull()) {
                        mCharacteristicRingBuffer.clear()
                        mIsWritingCharacteristic = false
                    }
                } else
                //CharacteristicWrite fail
                {
                    mCharacteristicRingBuffer.clear()
                    println("onCharacteristicWrite fail:" + String(characteristic.value))
                    println(status)
                }//WRITE a NEW CHARACTERISTIC
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt,
                                          characteristic: BluetoothGattCharacteristic,
                                          status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                println("onCharacteristicRead  " + characteristic.uuid.toString())
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt,
                                       characteristic: BluetoothGattDescriptor,
                                       status: Int) {
            println("onDescriptorWrite  " + characteristic.uuid.toString() + " " + status)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt,
                                             characteristic: BluetoothGattCharacteristic) {
            println("onCharacteristicChanged  " + String(characteristic.value))
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    private val mBinder = LocalBinder()

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after `BluetoothGatt#discoverServices()` completes successfully.
     *
     * @return A `List` of supported services.
     */
    val supportedGattServices: List<BluetoothGattService>?
        get() = if (mBluetoothGatt == null) null else mBluetoothGatt!!.services

    //class to store the Characteristic and content string push into the ring buffer.
    private inner class BluetoothGattCharacteristicHelper internal constructor(internal var mCharacteristic: BluetoothGattCharacteristic, internal var mCharacteristicValue: String)

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String,
                                characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)
        println("BluetoothLeService broadcastUpdate")
        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        //        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
        //            int flag = characteristic.getProperties();
        //            int format = -1;
        //            if ((flag & 0x01) != 0) {
        //                format = BluetoothGattCharacteristic.FORMAT_UINT16;
        //                Log.d(TAG, "Heart rate format UINT16.");
        //            } else {
        //                format = BluetoothGattCharacteristic.FORMAT_UINT8;
        //                Log.d(TAG, "Heart rate format UINT8.");
        //            }
        //            final int heartRate = characteristic.getIntValue(format, 1);
        //            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
        //            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        //        } else {
        // For all other profiles, writes the data formatted in HEX.
        val data = characteristic.value
        if (data != null && data.size > 0) {
            intent.putExtra(EXTRA_DATA, String(data))
            sendBroadcast(intent)
        }
        //        }
    }

    inner class LocalBinder : Binder() {
        internal val service: BluetoothLeService
            get() = this@BluetoothLeService
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close()
        return super.onUnbind(intent)
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    fun initialize(): Boolean {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        println("BluetoothLeService initialize" + mBluetoothManager!!)
        if (mBluetoothManager == null) {
            mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.")
                return false
            }
        }

        mBluetoothAdapter = mBluetoothManager!!.adapter
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }

        return true
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun connect(address: String?): Boolean {
        println("BluetoothLeService connect$address$mBluetoothGatt")
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.")
            return false
        }

        // Previously connected device.  Try to reconnect.
        //        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
        //                && mBluetoothGatt != null) {
        //            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
        //            if (mBluetoothGatt.connect()) {
        //            	System.out.println("mBluetoothGatt connect");
        //                mConnectionState = STATE_CONNECTING;
        //                return true;
        //            } else {
        //            	System.out.println("mBluetoothGatt else connect");
        //                return false;
        //            }
        //        }

        val device = mBluetoothAdapter!!.getRemoteDevice(address)
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.")
            return false
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        println("device.connectGatt connect")
        synchronized(this) {
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
        }
        Log.d(TAG, "Trying to create a new connection.")
        mBluetoothDeviceAddress = address
        mConnectionState = STATE_CONNECTING
        return true
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun disconnect() {
        println("BluetoothLeService disconnect")
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.disconnect()
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    fun close() {
        println("BluetoothLeService close")
        if (mBluetoothGatt == null) {
            return
        }
        mBluetoothGatt!!.close()
        mBluetoothGatt = null
    }

    /**
     * Request a read on a given `BluetoothGattCharacteristic`. The read result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.readCharacteristic(characteristic)
    }


    /**
     * Write information to the device on a given `BluetoothGattCharacteristic`. The content string and characteristic is
     * only pushed into a ring buffer. All the transmission is based on the `onCharacteristicWrite` call back function,
     * which is called directly in this function
     *
     * @param characteristic The characteristic to write to.
     */
    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }

        //The character size of TI CC2540 is limited to 17 bytes, otherwise characteristic can not be sent properly,
        //so String should be cut to comply this restriction. And something should be done here:
        val writeCharacteristicString: String
        try {
            writeCharacteristicString = String(characteristic.value, "ISO-8859-1")
        } catch (e: UnsupportedEncodingException) {
            // this should never happen because "US-ASCII" is hard-coded.
            throw IllegalStateException(e)
        }

        println("allwriteCharacteristicString:$writeCharacteristicString")

        //As the communication is asynchronous content string and characteristic should be pushed into an ring buffer for further transmission
        mCharacteristicRingBuffer.push(BluetoothGattCharacteristicHelper(characteristic, writeCharacteristicString))
        System.out.println("mCharacteristicRingBufferlength:" + mCharacteristicRingBuffer.size())


        //The progress of onCharacteristicWrite and writeCharacteristic is almost the same. So callback function is called directly here
        //for details see the onCharacteristicWrite function
        mGattCallback.onCharacteristicWrite(mBluetoothGatt, characteristic, WRITE_NEW_CHARACTERISTIC)

    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic,
                                      enabled: Boolean) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)

        //BluetoothGattDescriptor descriptor = characteristic.getDescriptor(characteristic.getUuid());
        //descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        //mBluetoothGatt.writeDescriptor(descriptor);

        // This is specific to Heart Rate Measurement.
        //        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
        //            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
        //                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
        //            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        //            mBluetoothGatt.writeDescriptor(descriptor);
        //        }
    }

    companion object {
        private val TAG = BluetoothLeService::class.java!!.getSimpleName()

        private val STATE_DISCONNECTED = 0
        private val STATE_CONNECTING = 1
        private val STATE_CONNECTED = 2


        //To tell the onCharacteristicWrite call back function that this is a new characteristic,
        //not the Write Characteristic to the device successfully.
        private val WRITE_NEW_CHARACTERISTIC = -1
        //define the limited length of the characteristic.
        private val MAX_CHARACTERISTIC_LENGTH = 17

        val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        val ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
    }


}
