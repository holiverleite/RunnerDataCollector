package com.holiverleite.runnerdatacollector

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import jxl.Workbook
import jxl.WorkbookSettings
import jxl.write.Label
import jxl.write.WritableSheet
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity(), SensorEventListener {

    private var commonReference: DatabaseReference? = null
    private var deviceReference: DatabaseReference? = null
    private var masterUploading: DatabaseReference? = null
    private var device1Uploading: DatabaseReference? = null

    private var currentState: String = "0"

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var mGyroscope: Sensor? = null

    private val fileReference = FirebaseStorage.getInstance().reference.child("Runner.xls")

    private var typeDevice: Device? = null

    enum class Device {
        DEVICE1,
        MASTER
    }

    data class GiroscopeData(
        val timestamp: String,
        val gx: String,
        val gy: String,
        val gz: String
    )

    data class AccelerometerData(
        val timestamp: String,
        val ax: String,
        val ay: String,
        val az: String
    )

    var giroscopeDataCollected: MutableList<GiroscopeData> = mutableListOf()
    var accelerometerDataCollected: MutableList<AccelerometerData> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.setAllSensors()

        val database = FirebaseDatabase.getInstance()
        this.commonReference = database.getReference("command")
        this.masterUploading = database.getReference("masterUploading")
        this.device1Uploading = database.getReference("device1Uploading")

        this.device_master.setOnClickListener {
            typeDevice = Device.MASTER
            this.deviceReference = database.getReference("deviceMaster")

            this.disableButtons(this.device_master)
            this.showSensorParameters()
            this.device_master.setBackgroundColor(Color.GREEN)
            this.start_button.isEnabled = true
            this.start_button.isVisible = true
            blockingView.text = "Esperando DEVICE1"
        }

        this.generateXLSFile.setOnClickListener {
            progress?.visibility = View.VISIBLE
            this.masterUploading?.setValue("1")
            this.device1Uploading?.setValue("0")
            updateXLS()

        }

        this.start_button.setOnClickListener {
            if (this.currentState == "0") {
                this.currentState = "1"
                this.commonReference?.setValue("1")
                start_button.text = "STOP"
                this.generateXLSFile.isVisible = false
            } else {
                this.currentState = "0"
                this.commonReference?.setValue("0")
                start_button.text = "RESTART"
                this.generateXLSFile.isVisible = true
            }
        }

        this.device1.setOnClickListener {
            typeDevice = Device.DEVICE1
            this.deviceReference = database.getReference("device1")

            this.disableButtons(this.device1)
            this.showSensorParameters()
            this.device1.setBackgroundColor(Color.GREEN)
            blockingView.text = "Esperando MASTER"
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

                    if(typeDevice == Device.DEVICE1) {
                        blockingView.visibility = View.VISIBLE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Toast.makeText(
                    applicationContext,
                    error.toException().toString(),
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        this.masterUploading?.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Toast.makeText(
                    applicationContext,
                    error.toException().toString(),
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val upload = dataSnapshot.getValue(String::class.java)
                if (upload == "1" && typeDevice == Device.DEVICE1) {
                    blockingView.visibility = View.VISIBLE
                }
            }

        })

        this.device1Uploading?.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Toast.makeText(
                    applicationContext,
                    error.toException().toString(),
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val upload = dataSnapshot.getValue(String::class.java)
                if (upload == "1" && typeDevice == Device.MASTER) {
                    blockingView.visibility = View.VISIBLE
                }

                if(upload == "1" && typeDevice == Device.DEVICE1) {
                    blockingView.visibility = View.GONE
                    updateXLS()
                }

                if(upload == "0") {
                    blockingView.visibility = View.GONE
                }
            }

        })
    }

    private fun updateXLS() {
        writeInXLS {
            if(typeDevice == Device.MASTER) {
                this.masterUploading?.setValue("0")
                this.device1Uploading?.setValue("1")
            } else {
                this.device1Uploading?.setValue("0")
            }
        }
    }

    private fun writeInXLS(finishXLS: () -> Unit) {
        val localFile = File.createTempFile("Runner", "xls")

        fileReference.getFile(localFile).addOnSuccessListener {
            val workbook = Workbook.getWorkbook(localFile)
            val copyWorkbook = Workbook.createWorkbook(localFile, workbook)
            val beforeSheet = workbook.getSheet(workbook.numberOfSheets - 1)

            val copySheet = when {
                typeDevice == Device.DEVICE1 -> {
                    val numberSheet = workbook.numberOfSheets - 1
                    copyWorkbook.getSheet(numberSheet)
                }
                beforeSheet.name.contains("Coleta") -> {
                    val numberSheet = beforeSheet.name.replace("Coleta ", "").toInt()
                    copyWorkbook.createSheet("Coleta ${numberSheet + 1}", numberSheet)
                }
                else -> {
                    val numberSheet = 0
                    copyWorkbook.createSheet("Coleta ${numberSheet + 1}", numberSheet)
                }
            }

            writeCellsInSheet(copySheet)

            copyWorkbook.write()
            copyWorkbook.close()
            workbook.close()

            saveFileInFirebase(fileReference, localFile, finishXLS)

        }.addOnFailureListener {
            if (it is StorageException) {
                val workbookSettings = WorkbookSettings()
                val workbook = Workbook.createWorkbook(localFile, workbookSettings)
                val sheet = workbook.createSheet("Coleta 1", 0)

                writeCellsInSheet(sheet)

                workbook.write()
                workbook.close()

                saveFileInFirebase(fileReference, localFile, finishXLS)

            } else {
                progress?.visibility = View.GONE
                Toast.makeText(this, "Não foi possível conexão com Firebase", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun saveFileInFirebase(
        reference: StorageReference,
        localFile: File,
        finishXLS: () -> Unit
    ) {
        reference.putFile(Uri.fromFile(localFile)).addOnSuccessListener {
            progress?.visibility = View.GONE
            Toast.makeText(this, "A escrita terminou", Toast.LENGTH_LONG).show()
            finishXLS.invoke()
        }
    }

    private fun writeCellsInSheet(sheet: WritableSheet) {
        when (typeDevice) {

            Device.MASTER -> {
                sheet.addCell(Label(0, 0, "MASTER"))

                sheet.addCell(Label(0, 1, "Acelerometro"))
                sheet.addCell(Label(0, 2, "Timestamp"))
                sheet.addCell(Label(1, 2, "X"))
                sheet.addCell(Label(2, 2, "Y"))
                sheet.addCell(Label(3, 2, "Z"))

                sheet.addCell(Label(5, 1, "Giroscopio"))
                sheet.addCell(Label(5, 2, "Timestamp"))
                sheet.addCell(Label(6, 2, "X"))
                sheet.addCell(Label(7, 2, "Y"))
                sheet.addCell(Label(8, 2, "Z"))

                accelerometerDataCollected.forEachIndexed { index, data ->
                    // 3 pq é a ultima linha dos headers
                    sheet.addCell(Label(0, index + 3, data.timestamp))
                    sheet.addCell(Label(1, index + 3, data.ax))
                    sheet.addCell(Label(2, index + 3, data.ay))
                    sheet.addCell(Label(3, index + 3, data.az))
                }

                giroscopeDataCollected.forEachIndexed { index, data ->
                    // 3 pq é a ultima linha dos headers
                    sheet.addCell(Label(5, index + 3, data.timestamp))
                    sheet.addCell(Label(6, index + 3, data.gx))
                    sheet.addCell(Label(7, index + 3, data.gy))
                    sheet.addCell(Label(8, index + 3, data.gz))
                }
            }

            Device.DEVICE1 -> {

                sheet.addCell(Label(10, 0, "DEVICE1"))

                sheet.addCell(Label(10, 1, "Acelerometro"))
                sheet.addCell(Label(10, 2, "Timestamp"))
                sheet.addCell(Label(11, 2, "X"))
                sheet.addCell(Label(12, 2, "Y"))
                sheet.addCell(Label(13, 2, "Z"))

                sheet.addCell(Label(15, 1, "Giroscopio"))
                sheet.addCell(Label(15, 2, "Timestamp"))
                sheet.addCell(Label(16, 2, "X"))
                sheet.addCell(Label(17, 2, "Y"))
                sheet.addCell(Label(18, 2, "Z"))

                accelerometerDataCollected.forEachIndexed { index, data ->
                    // 3 pq é a ultima linha dos headers
                    sheet.addCell(Label(10, index + 3, data.timestamp))
                    sheet.addCell(Label(11, index + 3, data.ax))
                    sheet.addCell(Label(12, index + 3, data.ay))
                    sheet.addCell(Label(13, index + 3, data.az))
                }

                giroscopeDataCollected.forEachIndexed { index, data ->
                    // 3 pq é a ultima linha dos headers
                    sheet.addCell(Label(15, index + 3, data.timestamp))
                    sheet.addCell(Label(16, index + 3, data.gx))
                    sheet.addCell(Label(17, index + 3, data.gy))
                    sheet.addCell(Label(18, index + 3, data.gz))
                }
            }
        }
    }

    fun enableSensorListeners() {
        this.mSensorManager?.registerListener(
            this,
            mAccelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        this.mSensorManager?.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun disableSensorListeners() {
        this.mSensorManager?.unregisterListener(this, this.mGyroscope)
        this.mSensorManager?.unregisterListener(this, this.mAccelerometer)
    }

    fun disableButtons(button: Button) {
        this.device1.isGone = true
        this.device1.isEnabled = false
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

        this.currentTimestamp.text = "Timestamp= ${event?.timestamp.toString()}"

        when {
            event?.sensor?.type == Sensor.TYPE_ACCELEROMETER -> {
                val acX = String.format("%.3f", event.values[0])
                val acY = String.format("%.3f", event.values[1])
                val acZ = String.format("%.3f", event.values[2])
                this.aceleXLabel.text = "X = $acX"
                this.aceleYLabel.text = "Y = $acY"
                this.aceleZLabel.text = "Z = $acZ"

                accelerometerDataCollected.add(
                    AccelerometerData(
                        timestamp = event.timestamp.toString(),
                        ax = acX,
                        ay = acY,
                        az = acZ
                    )
                )
            }
            event?.sensor?.type == Sensor.TYPE_GYROSCOPE -> {
                val eiX = String.format("%.3f", event.values[0])
                val eiY = String.format("%.3f", event.values[1])
                val eiZ = String.format("%.3f", event.values[2])
                this.eixoXLabel.text = "X = $eiX"
                this.eixoYLabel.text = "Y = $eiY"
                this.eixoZLabel.text = "Z = $eiZ"

                giroscopeDataCollected.add(
                    GiroscopeData(
                        timestamp = event.timestamp.toString(),
                        gx = eiX,
                        gy = eiY,
                        gz = eiZ
                    )
                )
            }
        }
    }
}
