package com.holiverleite.runnerdatacollector

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.widget.Button
import android.widget.Chronometer
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
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity() {

    private var myRef: DatabaseReference? = null
    private var timeWhenStopped: Long = 0
    private var currentState: String = "0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            this.disableButtons(this.device1)
            this.device1.setBackgroundColor(Color.GREEN)
            this.chronometer.isVisible = true
        }

        this.device2.setOnClickListener {
            this.disableButtons(this.device2)
            this.device2.setBackgroundColor(Color.GREEN)
            this.chronometer.isVisible = true
        }

        this.device3.setOnClickListener {
            this.disableButtons(this.device3)
            this.device3.setBackgroundColor(Color.GREEN)
            this.chronometer.isVisible = true
        }

        this.device4.setOnClickListener {
            this.disableButtons(this.device4)
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
                } else {
                    timeWhenStopped = chronometer.base - SystemClock.elapsedRealtime()
                    chronometer.stop()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Toast.makeText(applicationContext, error.toException().toString(), Toast.LENGTH_LONG).show()
            }
        })
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
}
