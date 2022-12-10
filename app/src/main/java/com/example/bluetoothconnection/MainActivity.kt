package com.example.bluetoothconnection

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.view.SupportActionModeWrapper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.bluetoothconnection.databinding.ActivityMainBinding
import com.google.gson.Gson
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class MainActivity : Activity() {
    var h: Handler? = null
    val RECEIVE_MESSAGE = 1
    private var btAdapter: BluetoothAdapter? = null
    private var btSocket: BluetoothSocket? = null
    private val sb = StringBuilder()
    private var mConnectedThread: ConnectedThread? = null
    private lateinit var binding: ActivityMainBinding
    var settings = Settings(0, 0, false, 0, 0, arrayOf(0, 0, 0))
    var data = Data(arrayOf(0, 0), arrayOf(0, 0), arrayOf(0, 0, 0))

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bottomNavigationView.setOnItemSelectedListener{ item ->
            when(item.itemId){
                R.id.home -> {

                    true
                }
                R.id.weather ->{

                    true
                }
                R.id.charts ->{

                    true
                }
                R.id.profile ->{

                    true
                }
                else -> false
            }
        }
//        h = object : Handler() {
//            override fun handleMessage(msg: Message) {
//                when (msg.what) {
//                    RECEIVE_MESSAGE -> {
//                        val readBuf = msg.obj as ByteArray
//                        val strIncom = String(readBuf, 0, msg.arg1)
//                        sb.append(strIncom) // формируем строку
//                        val endOfLineIndex = sb.indexOf("}") // определяем символы конца строки
//                        if (endOfLineIndex > 0) {                                            // если встречаем конец строки,
//                            var sbprint = sb.substring(0, endOfLineIndex) // то извлекаем строку
//                            sb.delete(0, sb.length) // и очищаем sb
//                            sbprint += "}"
//                            Log.d(TAG, sbprint)
//                            var gson =Gson()
//                            if(REQUEST_ID == 0) {
//                                settings = gson.fromJson(sbprint, Settings::class.java)
//                            }
//                            else{
//                                if(REQUEST_ID == 1){
//                                    data = gson.fromJson(sbprint, Data::class.java)
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
        //hello world
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        checkBTState()

    }

    public override fun onResume() {
        super.onResume()
        Log.d(TAG, "...onResume - попытка соединения...")
        val device = btAdapter!!.getRemoteDevice(address)
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
        } catch (e: IOException) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.message + ".")
        }
        btAdapter!!.cancelDiscovery()
        Log.d(TAG, "...Соединяемся...")
        try {
            btSocket!!.connect()
            Log.d(TAG, "...Соединение установлено и готово к передачи данных...")
        } catch (e: IOException) {
            try {
                btSocket!!.close()
                Log.d(TAG, "Соединение не установлено")
           } catch (e2: IOException) {
                errorExit(
                    "Fatal Error",
                    "In onResume() and unable to close socket during connection failure" + e2.message + "."
                )
            }
        }
        Log.d(TAG, "...Создание Socket...")
        mConnectedThread = ConnectedThread(btSocket!!)
        mConnectedThread!!.start()
    }
    public override fun onPause() {
        super.onPause()
        Log.d(TAG, "...In onPause()...")
        try {
            btSocket!!.close()
        } catch (e2: IOException) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.message + ".")
        }
    }
    private fun checkBTState() {
        if (btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth не поддерживается")
        } else {
            if (btAdapter!!.isEnabled) {
                Log.d(TAG, "...Bluetooth включен...")
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }
    }
    private fun errorExit(title: String, message: String) {
        Toast.makeText(baseContext, "$title - $message", Toast.LENGTH_LONG).show()
        finish()
    }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        override fun run() {
            val buffer = ByteArray(256)
            var bytes: Int
            while (true) {
                try {
                    bytes =
                        mmInStream!!.read(buffer)
                    h?.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer)
                        ?.sendToTarget()
                } catch (e: IOException) {
                    break
                }
            }
        }
        fun write(message: String) {
            try {
                mmOutStream!!.write(message.toByteArray())
                Log.d(TAG, "Данные отправлены")
            } catch (e: IOException) {
                Log.d(TAG, "...Ошибка отправки данных: " + e.message + "...")
            }
        }
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
            }
        }
        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }
    }
    companion object {
        private const val TAG = "bluetooth"
        private const val REQUEST_ENABLE_BT = 1
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val address = "20:16:01:19:68:59"
    }
}
