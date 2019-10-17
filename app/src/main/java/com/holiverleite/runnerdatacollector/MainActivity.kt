package com.holiverleite.runnerdatacollector

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), SensorEventListener {

    private var commonReference: DatabaseReference? = null
    private var deviceReference: DatabaseReference? = null

    private var currentState: String = "0"

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var mGyroscope: Sensor? = null

    data class SensorData(val timestamp: String,
                          val gx: String,
                          val gy: String,
                          val gz: String,
                          val ax: String,
                          val ay: String,
                          val az: String
    )

    var dataCollected: MutableList<SensorData> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.setAllSensors()

        val database = FirebaseDatabase.getInstance()
        this.commonReference = database.getReference("command")

        this.device_master.setOnClickListener {
            this.deviceReference = database.getReference("deviceMaster")

            this.disableButtons(this.device_master)
            this.showSensorParameters()
            this.device_master.setBackgroundColor(Color.GREEN)
            this.start_button.isEnabled = true
            this.start_button.isVisible = true
        }

        this.generateXLSFile.setOnClickListener {
            // Gerar XLS com os dados do Firebase
        }

        this.start_button.setOnClickListener {
            if (this.currentState == "0") {
                this.currentState = "1"
                this.commonReference?.setValue("1")
                start_button.text = "STOP"
                this.generateXLSFile.isVisible = false
            }else {
                this.currentState = "0"
                this.commonReference?.setValue("0")
                start_button.text = "RESTART"
                this.generateXLSFile.isVisible = true
            }
        }

        this.device1.setOnClickListener {
            this.deviceReference = database.getReference("device1")

            this.disableButtons(this.device1)
            this.showSensorParameters()
            this.device1.setBackgroundColor(Color.GREEN)
        }

        this.device2.setOnClickListener {
            this.deviceReference = database.getReference("device2")

            this.disableButtons(this.device2)
            this.showSensorParameters()
            this.device2.setBackgroundColor(Color.GREEN)
        }

        this.device3.setOnClickListener {
            this.deviceReference = database.getReference("device3")

            this.disableButtons(this.device3)
            this.showSensorParameters()
            this.device3.setBackgroundColor(Color.GREEN)
        }

        this.device4.setOnClickListener {
            this.deviceReference = database.getReference("device4")

            this.disableButtons(this.device4)
            this.showSensorParameters()
            this.device4.setBackgroundColor(Color.GREEN)
        }

        // Common reference - All devices listen to this database reference
        commonReference?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val value = dataSnapshot.getValue(String::class.java)

                if (value == "1") {
                    // Deleta dados do device do Firebase
                    deviceReference?.removeValue()

                    chronometer.base = SystemClock.elapsedRealtime()

                    chronometer.start()

                    enableSensorListeners()
                } else {
                    chronometer.stop()

                    disableSensorListeners()

                    saveDataInDataBase()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Toast.makeText(applicationContext, error.toException().toString(), Toast.LENGTH_LONG).show()
            }
        })
    }

    fun enableSensorListeners() {
        this.mSensorManager?.registerListener(this,mAccelerometer,SensorManager.SENSOR_DELAY_NORMAL)
        this.mSensorManager?.registerListener(this,mGyroscope,SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun disableSensorListeners() {
        this.mSensorManager?.unregisterListener(this,this.mGyroscope)
        this.mSensorManager?.unregisterListener(this,this.mAccelerometer)
    }

    fun saveDataInDataBase() {
        var index = 0
        this.dataCollected.forEach {
            this.deviceReference?.child(index.toString())?.child("timestamp")?.setValue(it.timestamp)
            this.deviceReference?.child(index.toString())?.child("ax")?.setValue(it.ax)
            this.deviceReference?.child(index.toString())?.child("ay")?.setValue(it.ay)
            this.deviceReference?.child(index.toString())?.child("az")?.setValue(it.az)
            this.deviceReference?.child(index.toString())?.child("gx")?.setValue(it.gx)
            this.deviceReference?.child(index.toString())?.child("gy")?.setValue(it.gy)
            this.deviceReference?.child(index.toString())?.child("gz")?.setValue(it.gz)
            index++
        }
    }

    fun disableButtons(button: Button) {
        this.device1.isGone = true
        this.device1.isEnabled = false
        this.device2.isGone = true
        this.device2.isEnabled = false
        this.device3.isGone = true
        this.device3.isEnabled = false
        this.device4.isGone = true
        this.device4.isEnabled = false
        this.device_master.isGone = true
        this.device_master.isEnabled = false

        button.isGone = false
        this.chronometer.isVisible = true
    }

    fun showSensorParameters() {
        this.eixoXLabel.isVisible = true
        this.eixoYLabel.isVisible = true
        this.eixoZLabel.isVisible = true
        this.aceleXLabel.isVisible = true
        this.aceleYLabel.isVisible = true
        this.aceleZLabel.isVisible = true
        this.currentTimestamp.isVisible = true
    }

    fun setAllSensors() {
        this.mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        this.mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        this.mGyroscope = mSensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {

        var currentTimestamp = System.currentTimeMillis()/1000
        this.currentTimestamp.text = "Timestamp: " + currentTimestamp.toString()

        val sensorData = SensorData(currentTimestamp.toString(),
            this.eixoXLabel.text.toString(),
            this.eixoYLabel.text.toString(),
            this.eixoZLabel.text.toString(),
            this.aceleXLabel.text.toString(),
            this.aceleYLabel.text.toString(),
            this.aceleZLabel.text.toString())

        this.dataCollected.add(sensorData)

        if (event?.sensor!!.type == Sensor.TYPE_ACCELEROMETER) {
            this.aceleXLabel.text = event.values[0].toString()
            this.aceleYLabel.text = event.values[1].toString()
            this.aceleZLabel.text = event.values[2].toString()
        }

        if (event?.sensor!!.type == Sensor.TYPE_GYROSCOPE) {
            this.eixoXLabel.text = event.values[0].toString()
            this.eixoYLabel.text = event.values[1].toString()
            this.eixoZLabel.text = event.values[2].toString()
        }
    }
}
