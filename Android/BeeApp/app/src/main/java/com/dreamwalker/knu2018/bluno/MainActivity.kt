package com.dreamwalker.knu2018.bluno

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatDelegate
import android.support.v7.view.ActionMode
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : BlunoLibrary() {
    private var buttonScan: Button? = null
    private var buttonSerialSend: Button? = null
    private var serialSendText: EditText? = null
    private var serialReceivedText: TextView? = null

    private var delegate: AppCompatDelegate? = null

    protected fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //let's create the delegate, passing the activity at both arguments (Activity, AppCompatCallback)
        delegate = AppCompatDelegate.create(this, this)
        //we need to call the onCreate() of the AppCompatDelegate
        delegate!!.onCreate(savedInstanceState)
        //we use the delegate to inflate the layout
        delegate!!.setContentView(R.layout.activity_main)
        //Finally, let's add the Toolbar
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        delegate!!.setSupportActionBar(toolbar)

        onCreateProcess()                                                        //onCreate Process by BlunoLibrary

        serialBegin(115200)                                                    //set the Uart Baudrate on BLE chip to 115200

        serialReceivedText = findViewById(R.id.serialReveicedText) as TextView    //initial the EditText of the received data
        serialSendText = findViewById(R.id.serialSendText) as EditText            //initial the EditText of the sending data

        buttonSerialSend = findViewById(R.id.buttonSerialSend) as Button        //initial the button for sending the data
        buttonSerialSend!!.setOnClickListener {
            // TODO Auto-generated method stub

            serialSend(serialSendText!!.text.toString())                //send the data to the BLUNO
        }

        buttonScan = findViewById(R.id.buttonScan) as Button                    //initial the button for scanning the BLE device
        buttonScan!!.setOnClickListener {
            // TODO Auto-generated method stub

            buttonScanOnClickProcess()                                        //Alert Dialog for selecting the BLE device
        }
    }

    protected fun onResume() {
        super.onResume()
        println("BlUNOActivity onResume")
        onResumeProcess()                                                        //onResume Process by BlunoLibrary
    }


    protected fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        onActivityResultProcess(requestCode, resultCode, data)                    //onActivityResult Process by BlunoLibrary
        super.onActivityResult(requestCode, resultCode, data)
    }

    protected fun onPause() {
        super.onPause()
        onPauseProcess()                                                        //onPause Process by BlunoLibrary
    }

    protected fun onStop() {
        super.onStop()
        onStopProcess()                                                        //onStop Process by BlunoLibrary
    }

    protected fun onDestroy() {
        super.onDestroy()
        onDestroyProcess()                                                        //onDestroy Process by BlunoLibrary
    }

    fun onConectionStateChange(theConnectionState: connectionStateEnum) {//Once connection state changes, this function will be called
        when (theConnectionState) {
        //Four connection state
            isConnected -> buttonScan!!.text = "Connected"
            isConnecting -> buttonScan!!.text = "Connecting"
            isToScan -> buttonScan!!.text = "Scan"
            isScanning -> buttonScan!!.text = "Scanning"
            isDisconnecting -> buttonScan!!.text = "isDisconnecting"
            else -> {
            }
        }
    }

    fun onSerialReceived(theString: String) {                            //Once connection data received, this function will be called
        // TODO Auto-generated method stub
        serialReceivedText!!.append(theString)                            //append the text into the EditText
        //The Serial data from the BLUNO may be sub-packaged, so using a buffer to hold the String is a good choice.
        (serialReceivedText!!.parent as ScrollView).fullScroll(View.FOCUS_DOWN)
    }

    fun onWindowStartingSupportActionMode(callback: ActionMode.Callback): ActionMode? {
        return null
    }
}