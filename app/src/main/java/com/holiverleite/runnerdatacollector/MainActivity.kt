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
import java.sql.Timestamp


class MainActivity : AppCompatActivity(), SensorEventListener {
    private var myRef: DatabaseReference? = null
    private var deviceReference: DatabaseReference? = null
    private var timeWhenStopped: Long = 0
    private var currentState: String = "0"

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var mGyroscope: Sensor? = null

    data class SensorData(val timestamp: String, val gx: String, val gy: String, val gz: String, val ax: String, val ay: String, val az: String)
    var dataCollected: List<SensorData> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.setAllSensors()

        val database = FirebaseDatabase.getInstance()
        this.myRef = database.getReference("command")

        this.device_master.setOnClickListener {
            this.disableButtons(this.device_master)
            this.device_master.setBackgroundColor(Color.GREEN)
            this.start_button.isEnabled = true
            this.start_button.isVisible = true
        }

        this.start_button.setOnClickListener {
            if (this.currentState == "0") {
                this.currentState = "1"
                this.myRef?.setValue("1")
                start_button.text = "STOP"
            }else {
                this.currentState = "0"
                this.myRef?.setValue("0")
                start_button.text = "START"
            }
        }

        this.device1.setOnClickListener {
            this.deviceReference = database.getReference("device1")

            this.deviceReference?.child("device1")?.setValue("aaaa")
            this.disableButtons(this.device1)
            this.showSensorParameters()
            this.device1.setBackgroundColor(Color.GREEN)
            this.chronometer.isVisible = true
        }

        this.device2.setOnClickListener {
            this.deviceReference = database.getReference("device2")
            this.deviceReference?.setValue("device2")
            this.disableButtons(this.device2)
            this.showSensorParameters()
            this.device2.setBackgroundColor(Color.GREEN)
            this.chronometer.isVisible = true
        }

        this.device3.setOnClickListener {
            this.disableButtons(this.device3)
            this.showSensorParameters()
            this.device3.setBackgroundColor(Color.GREEN)
            this.chronometer.isVisible = true
        }

        this.device4.setOnClickListener {
            this.disableButtons(this.device4)
            this.showSensorParameters()
            this.device4.setBackgroundColor(Color.GREEN)
            this.chronometer.isVisible = true
        }

        // Read from the database
        myRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val value = dataSnapshot.getValue(String::class.java)

                if (value == "1") {
                    chronometer.base = SystemClock.elapsedRealtime() + timeWhenStopped
                    chronometer.start()

                    enableSensorListeners()
                } else {
                    timeWhenStopped = chronometer.base - SystemClock.elapsedRealtime()
                    chronometer.stop()

                    disableSensorListeners()
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
    }

    fun showSensorParameters() {
        this.eixoXLabel.isVisible = true
        this.eixoYLabel.isVisible = true
        this.eixoZLabel.isVisible = true
        this.aceleXLabel.isVisible = true
        this.aceleYLabel.isVisible = true
        this.aceleZLabel.isVisible = true
    }

    fun setAllSensors() {
        this.mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        this.mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        this.mGyroscope = mSensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }


    // SENSOR IMPLEMENTATIOS
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {

        if (event?.sensor!!.type == Sensor.TYPE_ACCELEROMETER) {
            this.aceleXLabel.text = "AcelerometroX: " + event.values[0].toString()
            this.aceleYLabel.text = "AcelerometroY: " + event.values[1].toString()
            this.aceleZLabel.text = "AcelerometroZ: " + event.values[2].toString()
        }

        if (event?.sensor!!.type == Sensor.TYPE_GYROSCOPE) {
            this.eixoXLabel.text = "GyroscopeX: " + event.values[0].toString()
            this.eixoYLabel.text = "GyroscopeY: " + event.values[1].toString()
            this.eixoZLabel.text = "GyroscopeZ: " + event.values[2].toString()
        }
    }
}
