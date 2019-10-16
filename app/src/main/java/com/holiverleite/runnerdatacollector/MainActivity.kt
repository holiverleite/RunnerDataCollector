package com.holiverleite.runnerdatacollector

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.widget.Chronometer
import android.widget.Toast
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
            this.disableAllButtons()
            this.device_master.setBackgroundColor(Color.GREEN)
            this.start_button.isEnabled = true
            this.start_button.isVisible = true

//            Toast.makeText(applicationContext, timer.toString(), Toast.LENGTH_LONG).show()

            // Call to set as a MASTER
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
            this.disableAllButtons()
            this.device1.setBackgroundColor(Color.GREEN)
            this.chronometer.isVisible = true

            // Call to set as a MASTER
        }

        this.device2.setOnClickListener {
            this.disableAllButtons()
            this.device2.setBackgroundColor(Color.GREEN)
            this.chronometer.isVisible = true

            // Call to set as a MASTER
        }

        this.device3.setOnClickListener {
            this.disableAllButtons()
            this.device3.setBackgroundColor(Color.GREEN)
            this.chronometer.isVisible = true

            // Call to set as a MASTER
        }

        this.device4.setOnClickListener {
            this.disableAllButtons()
            this.device4.setBackgroundColor(Color.GREEN)
            this.chronometer.isVisible = true

            // Call to set as a MASTER
        }

        this.device5.setOnClickListener {
            this.disableAllButtons()
            this.device5.setBackgroundColor(Color.GREEN)
            this.chronometer.isVisible = true

            // Call to set as a MASTER
        }

        this.device6.setOnClickListener {
            this.disableAllButtons()
            this.device6.setBackgroundColor(Color.GREEN)
            this.chronometer.isVisible = true

            // Call to set as a MASTER
        }

        this.device7.setOnClickListener {
            this.disableAllButtons()
            this.device7.setBackgroundColor(Color.GREEN)
            this.chronometer.isVisible = true

            // Call to set as a MASTER
        }

        // Read from the database
        myRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val value = dataSnapshot.getValue(String::class.java)

                if (value == "1") {
                    chronometer.base = SystemClock.elapsedRealtime() + timeWhenStopped
                    chronometer.start()
                } else {
                    timeWhenStopped = chronometer.base - SystemClock.elapsedRealtime()
                    chronometer.stop()
                }
//                Toast.makeText(applicationContext, value, Toast.LENGTH_LONG).show()
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Toast.makeText(applicationContext, error.toException().toString(), Toast.LENGTH_LONG).show()
            }
        })
    }

    fun disableAllButtons() {
        this.device1.isEnabled = false
        this.device2.isEnabled = false
        this.device3.isEnabled = false
        this.device4.isEnabled = false
        this.device5.isEnabled = false
        this.device6.isEnabled = false
        this.device7.isEnabled = false
        this.device_master.isEnabled = false
    }
}
