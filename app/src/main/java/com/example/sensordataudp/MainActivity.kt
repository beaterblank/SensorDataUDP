package com.example.sensordataudp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.net.NetworkInterface

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var gyroscopeSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SensorManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Get the gyroscope sensor
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        // Set up the content using Jetpack Compose
        setContent {
            SensorApp()
        }

        if (gyroscopeSensor == null) {
            // Gyroscope sensor is not supported, show a message
            showToast()
        }
        if (accelerometerSensor == null){
            // Accelerometer sensor is not supported, show a message
            showToast()
        }
    }

    override fun onResume() {
        super.onResume()
        // Register the sensor listener if it's not null
        gyroscopeSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the gyroscope sensor listener to save battery
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing for now
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
            val angularSpeedX = event.values[0]
            val angularSpeedY = event.values[1]
            val angularSpeedZ = event.values[2]

            SensorApp.updateGyroscopeData(mapOf( "x" to angularSpeedX, "y" to angularSpeedY,"z" to angularSpeedZ))
        }
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER){
            val accelX = event.values[0]
            val accelY = event.values[1]
            val accelZ = event.values[2]

            SensorApp.updateAccelerometerData(mapOf( "x" to accelX, "y" to accelY,"z" to accelZ))
        }
    }

    private fun showToast() {
        val message = "Gyroscope sensor is not supported on this device."
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun SensorValueCard(title: String, value: String) {
    Card(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        modifier = Modifier
            .padding(16.dp),
        colors = CardDefaults.cardColors(
                containerColor = Color(252,163,17),
        )

    ) {
        Row(
            modifier = Modifier
                .padding(8.dp),
        ) {
            Text(
                text = "$title : ",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight(900),
                color = Color(20,33,61)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SensorCard(title:String,about:String,sensorData:Map<String,Float>){
    Card(modifier = Modifier.padding(8.dp)){
        Column(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight(900),
                color = Color(20,33,61)
            )
            Text(
                text = about,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(20,33,61)
            )
            FlowRow {
                sensorData.forEach { (key, value) ->
                    SensorValueCard(title = key, value = "%+.3f".format(value))
                }
            }
        }
    }
}

@Composable
@Preview(showBackground=true)
fun SensorApp() {
    var gyroscopeData by remember { mutableStateOf(mapOf("x" to 0.0f,"y" to 0.0f,"z" to 0.0f) )}
    var accelerometerData by remember { mutableStateOf(mapOf("x" to 0.0f,"y" to 0.0f,"z" to 0.0f))}
    val aboutGyroscope = """A gyroscope is a device used for measuring or maintaining orientation and angular velocity. It is a spinning wheel or disc in which the axis of rotation is free to assume any orientation by itself"""
    val aboutAccelerometer = """An accelerometer is a tool that measures proper acceleration. Proper acceleration is the acceleration of a body in its own instantaneous rest frame; this is different from coordinate acceleration, which is acceleration in a fixed coordinate system."""
    val interfaces = NetworkInterface.getNetworkInterfaces()
    val addresses = mutableListOf<String>()
    while (interfaces.hasMoreElements()) {
        val netInterface = interfaces.nextElement()
        val inetAddresses = netInterface.inetAddresses
        while (inetAddresses.hasMoreElements()) {
            val ip = inetAddresses.nextElement().toString()
            if (ip.contains(".")){
                addresses.add(ip)
            }
        }
    }

    var expanded by remember { mutableStateOf(false) }
    var selectedAddress by remember { mutableStateOf(addresses.first()) }

    Column (modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp), horizontalAlignment=Alignment.CenterHorizontally){
        Column{
            addresses.forEach { address ->
                DropdownMenuItem(
                    text = { Text(address) },
                    onClick = {
                        selectedAddress = address
                        expanded = false
                    }
                )
            }
        }
        SensorCard("Gyroscope",about=aboutGyroscope,sensorData = gyroscopeData)
        SensorCard("Accelerometer",about=aboutAccelerometer,sensorData = accelerometerData)
    }

    LaunchedEffect(Unit) {
        SensorApp.updateGyroscopeData = {
                data:Map<String, Float> -> gyroscopeData = data
        }
        SensorApp.updateAccelerometerData = {
                data:Map<String, Float> -> accelerometerData = data

        }
    }
}

object SensorApp {
    var updateGyroscopeData: (Map<String, Float>) -> Unit = {}
    fun updateGyroscopeData(data: Map<String, Float>) {
        updateGyroscopeData.invoke(data)
    }
    var updateAccelerometerData: (Map<String, Float>) -> Unit = {}
    fun updateAccelerometerData(data: Map<String, Float>) {
        updateAccelerometerData.invoke(data)
    }
}
