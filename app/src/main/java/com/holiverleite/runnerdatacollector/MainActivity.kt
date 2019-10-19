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
 /*

 COMO TESTAR

 RODE EM UM EMULADOR  OU DEVICE REAL E SELECIONE MASTER

 RODE EM UM EMULADOR OU DEVICE REAL E SELECIONE DEVICE1

 COMECE A CAPTURA NO MASTER E PAUSE QUANDO QUISER

 CLIQUE EM GERAR XLS E VEJA O PROCESSO ACONTECER

  */

class MainActivity : AppCompatActivity(), SensorEventListener {

    //REFERENCIA QUE CONTROLA O INICIO OU TERMINO DE UMA CAPTURA
    private var commonReference: DatabaseReference? = null

    // REFERENCIA QUE INDICA Q O MASTER ESTA FAZENDO A ESCRITA E UPLOAD DO XLS
    private var masterUploading: DatabaseReference? = null

    // REFERENCIA QUE INDICA Q O DEVICE1 ESTA FAZENDO A ESCRITA E UPLOAD DO XLS
    private var device1Uploading: DatabaseReference? = null

    // CONTROLA ESTADO DO BOTAO
    private var currentState: String = "0"

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var mGyroscope: Sensor? = null


    //REFERENCIA DO ARQUIVO XLS NO FIREBASESTORAGE
    private val fileReference = FirebaseStorage.getInstance().reference.child("Runner.xls")

    // DEFINE O TIPO DO DEVICE, OU MASTER OU DEVICE1 - PARA CONTROLAR COMPORTAMENTO DE TELAS
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

    //DADOS COLETADOS DO GIRO - LOCAL
    var giroscopeDataCollected: MutableList<GiroscopeData> = mutableListOf()
    //DADOS COLETADOS DO ACCELEROMETER - LOCAL
    var accelerometerDataCollected: MutableList<AccelerometerData> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.setAllSensors()

        val database = FirebaseDatabase.getInstance()
        //PEGA AS REFERENCIAS NO DATABASE
        this.commonReference = database.getReference("command")
        this.masterUploading = database.getReference("masterUploading")
        this.device1Uploading = database.getReference("device1Uploading")

        //CONFIGURA CASO USUARIO ESCOLHAR O DEVICE ATUAL COMO MASTER
        this.device_master.setOnClickListener {
            typeDevice = Device.MASTER

            this.disableButtons(this.device_master)
            this.showSensorParameters()
            this.device_master.setBackgroundColor(Color.GREEN)
            this.start_button.isEnabled = true
            this.start_button.isVisible = true
            //DEFINE O TEXTO DA BLOCKINGVIEW QUANDO O DEVICE1 ESTIVER FAZENDO UPLOAD
            blockingView.text = "Esperando DEVICE1"
        }

        this.generateXLSFile.setOnClickListener {
            progress?.visibility = View.VISIBLE
            //AVISA QUE O MASTER TA COMECANDO O UPLOAD E O DEVICE1 NAO
            this.masterUploading?.setValue("1")
            this.device1Uploading?.setValue("0")
            updateXLS()

        }

        this.start_button.setOnClickListener {
            //USUARIO CLICOU PARA COMECAR E SETA COMMON REFERENCE PRA 1, DISPARANDO A CAPTURA NO DEVICE1 TBEM
            if (this.currentState == "0") {
                this.currentState = "1"
                this.commonReference?.setValue("1")
                start_button.text = "STOP"
                this.generateXLSFile.isVisible = false
            } else {
                //PAUSA CAPTURA NO DEVICE 1 E MASTER
                this.currentState = "0"
                this.commonReference?.setValue("0")
                start_button.text = "RESTART"
                this.generateXLSFile.isVisible = true
            }
        }

        this.device1.setOnClickListener {
            typeDevice = Device.DEVICE1

            this.disableButtons(this.device1)
            this.showSensorParameters()
            this.device1.setBackgroundColor(Color.GREEN)

            //DEFINE TEXTO DO BLOCKINGVIEW PARA DEVICE1 QUANDO O MASTER TA FAZENDO UPLOAD
            blockingView.text = "Esperando MASTER"
        }

        // Common reference - All devices listen to this database reference
        commonReference?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val value = dataSnapshot.getValue(String::class.java)

                if (value == "1") {
                    //TODOS COMECAM A CAPTURAR
                    chronometer.base = SystemClock.elapsedRealtime()

                    chronometer.start()

                    enableSensorListeners()
                } else {
                    //TODOS PARAM
                    chronometer.stop()

                    disableSensorListeners()

                    if(typeDevice == Device.DEVICE1) {
                        //DEVICE1 MOSTRA A BLOCKINGVIEW ESPERANDO MASTER
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
                // CASO MASTER ESTEJA FAZENDO UPLOAD (1) E O DEVICE ATUAL FOR O 1, MOSTRA A BLOCKINGVIEW
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
                // SE UPLOAD DO DEVICE1 TA ACONTECENDO E O MASTER É O DEVICE ATUAL, MOSTRE A BLOCKINGVIEW ESPERANDO PELO DEVICE1
                if (upload == "1" && typeDevice == Device.MASTER) {
                    blockingView.visibility = View.VISIBLE
                }

                //SE O UPLOADING DO DEVICE1 ESTIVER ACONTECENDO E O DEVICE ATUAL É O DEVICE1, ESCONDE A BLOCKINGVIEW DELE E COMECA O UPDATE DO XLS
                if(upload == "1" && typeDevice == Device.DEVICE1) {
                    blockingView.visibility = View.GONE
                    updateXLS()
                }

                // SE O UPLOADING DO DEVICE1 NAO ESTIVER ACONTECENDO, ESCONDA A BLOCKINGVIEW INDEPENDENTE DO DEVICE
                if(upload == "0") {
                    blockingView.visibility = View.GONE
                }
            }

        })
    }

    private fun updateXLS() {
        writeInXLS {
            //BLOCO CHAMADO DEPOIS QUE UM UPLOAD DE XLS ACONTECE

            //CASO O DEVICE ATUAL SEJA O MASTER, SINALIZA QUE O MASTERUPLOADING ACABOU E SETA O DEVICE1UPLOADING COMO INICIANDO
            //SENAO O UPLOADING DO DEVICE1 ACABOU TBEM
            if(typeDevice == Device.MASTER) {
                this.masterUploading?.setValue("0")
                this.device1Uploading?.setValue("1")
            } else {
                this.device1Uploading?.setValue("0")
            }
        }
    }

    private fun writeInXLS(finishXLS: () -> Unit) {
        //CRIAR FILE TEMPORARIO
        val localFile = File.createTempFile("Runner", "xls")

        //TENTA PEGAR O FILE REFERENCE (FILE NO FIREBASE) E ATRIBUI AO LOCAL FILE. NO SUCESSO  EXECUTA O BLOCO,
        // SENAO ELE VAI CRIAR UM ARQUIVO E FAZER A PRIMEIRA CAPTURA
        fileReference.getFile(localFile).addOnSuccessListener {
            val workbook = Workbook.getWorkbook(localFile)
            val copyWorkbook = Workbook.createWorkbook(localFile, workbook)
            val beforeSheet = workbook.getSheet(workbook.numberOfSheets - 1)

            val copySheet = when {
                //SE O ARQUIVO JA EXISTE E O DEVICE É O 1, ELE PRECISA PEGAR O ULTIMO SHEET PARA ESCREVER COM O MASTER
                typeDevice == Device.DEVICE1 -> {
                    val numberSheet = workbook.numberOfSheets - 1
                    copyWorkbook.getSheet(numberSheet)
                }
                // SENAO ELE PARSEA O NOME DA ULTIMA SHEET E AUMENTA O INDICE PARA A NOVA SHEET
                beforeSheet.name.contains("Coleta") -> {
                    val numberSheet = beforeSheet.name.replace("Coleta ", "").toInt()
                    copyWorkbook.createSheet("Coleta ${numberSheet + 1}", numberSheet)
                }
                else -> {
                    // AQUI É QUANDO É A PRIMEIRA SHEET, ARQUIVO NOVO
                    val numberSheet = 0
                    copyWorkbook.createSheet("Coleta ${numberSheet + 1}", numberSheet)
                }
            }

            //ESCREVE TUDO NO OBJETO WORKBOOK
            writeCellsInSheet(copySheet)


            //ESCREVE O XLS
            copyWorkbook.write()
            copyWorkbook.close()
            workbook.close()

            //SALVA NO FIREBASE
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
            //CHAMA AQUELE BLOCO QUE INDICA QUE ACABOU O UPLOAD
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
