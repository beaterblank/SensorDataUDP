package com.example.sensordataudp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.text.isDigitsOnly

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var gyroscopeSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var ambientTemperatureSensor: Sensor? = null
    private var statusChecked:Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SensorManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // Get the gyroscope sensor
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        ambientTemperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        // Set up the content using Jetpack Compose
        setContent {
            SensorApp()
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
        ambientTemperatureSensor?.let{
            sensorManager.registerListener(this,it,SensorManager.SENSOR_DELAY_NORMAL)
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
            if (!statusChecked) SensorApp.updateGyroscopeSupport(true)
            SensorApp.updateGyroscopeData(mapOf( "x" to angularSpeedX, "y" to angularSpeedY,"z" to angularSpeedZ))
        }
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER){
            val accelX = event.values[0]
            val accelY = event.values[1]
            val accelZ = event.values[2]
            if(!statusChecked) SensorApp.updateAccelerometerSupport(true)
            SensorApp.updateAccelerometerData(mapOf( "x" to accelX, "y" to accelY,"z" to accelZ))
        }
        if (event?.sensor?.type == Sensor.TYPE_AMBIENT_TEMPERATURE){
            val temp = event.values[0]
            SensorApp.updateAmbientTemperatureSupport(true)
            SensorApp.updateAmbientTemperatureData(mapOf("temperature" to temp))
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
fun SensorCard(title:String,about:String,supported:Boolean,sensorData:Map<String,Float>){
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
            if(!supported) Text(
                text = "sensor is not supported on your device",
                color = Color(200,0,0)
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
@Preview(showBackground=true)
fun SensorApp() {
    var gyroscopeData by remember { mutableStateOf(mapOf("x" to 0.0f,"y" to 0.0f,"z" to 0.0f) )}
    var supportGyroscope by remember { mutableStateOf(false) }
    var accelerometerData by remember { mutableStateOf(mapOf("x" to 0.0f,"y" to 0.0f,"z" to 0.0f))}
    var supportAccelerometer by remember { mutableStateOf(false) }
    var temperatureData by remember { mutableStateOf(mapOf("temperature" to 0.0f)) }
    var supportAmbientTemperature by remember { mutableStateOf(false) }
    val aboutGyroscope = """A gyroscope is a device used for measuring or maintaining orientation and angular velocity. It is a spinning wheel or disc in which the axis of rotation is free to assume any orientation by itself"""
    val aboutAccelerometer = """An accelerometer is a tool that measures proper acceleration. Proper acceleration is the acceleration of a body in its own instantaneous rest frame; this is different from coordinate acceleration, which is acceleration in a fixed coordinate system."""
    val aboutTemp = "Shows Ambient room temperature"
    val interfaces = NetworkInterface.getNetworkInterfaces()
    val addresses = mutableListOf("/0.0.0.0")
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
    var selectedAddress by remember { mutableStateOf("") }
    val onItemClick = { address: String -> selectedAddress = address}
    var serverStatus by remember { mutableStateOf(false) }
    var portNumber by remember { mutableStateOf("26076") }
    val scrollState = rememberScrollState()



    Column (modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        , horizontalAlignment=Alignment.CenterHorizontally
    ) {
        LazyColumn(modifier = Modifier.padding(vertical = 16.dp)) {
            stickyHeader{
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = portNumber,
                            placeholder = { Text("26076")},
                            onValueChange = { if (it.isDigitsOnly()) portNumber = it },
                            label = { Text("Port") },
                            modifier = Modifier
                                .padding( 16.dp),
                                //.width(128.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Column {
                            Text(text = "Server")
                            Switch(checked = serverStatus, onCheckedChange = {serverStatus = it})
                        }
                    }
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        //horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        items(addresses) { address ->
                            Card (
                                modifier = Modifier.padding(horizontal = 8.dp),
                                colors = CardDefaults.cardColors(
                                containerColor = if (address == selectedAddress) Color(252,163,17) else Color(229,229,229),

                            )){
                                Text(text = address, modifier = Modifier
                                    .clickable { onItemClick.invoke(address) }
                                    .fillMaxWidth()
                                    .padding(12.dp)
                                )
                            }

                        }
                    }
                }
            }
        }
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            SensorCard("Gyroscope", about = aboutGyroscope,supported=supportGyroscope ,sensorData = gyroscopeData)
            SensorCard("Accelerometer", about = aboutAccelerometer,supported=supportAccelerometer, sensorData = accelerometerData)
            SensorCard("Ambient Temperature",about = aboutTemp,supported=supportAmbientTemperature, sensorData = temperatureData)
        }

    }
    LaunchedEffect(Unit) {
        SensorApp.updateGyroscopeData = {
                data:Map<String, Float> -> run{
                    gyroscopeData = data
                }
        }
        SensorApp.updateGyroscopeSupport = {
            flag:Boolean -> supportGyroscope = flag
        }
        SensorApp.updateAccelerometerData = {
                data:Map<String, Float> -> accelerometerData = data
        }
        SensorApp.updateAccelerometerSupport = {
                flag:Boolean -> supportAccelerometer = flag
        }
        SensorApp.updateAmbientTemperatureData = {
                data:Map<String, Float> -> temperatureData = data
        }
        SensorApp.updateAmbientTemperatureSupport = {
                flag:Boolean -> supportAmbientTemperature = flag
        }
    }
}

object SensorApp {
    var updateGyroscopeData: (Map<String, Float>) -> Unit = {}
    fun updateGyroscopeData(data: Map<String, Float>) {
        updateGyroscopeData.invoke(data)
    }
    var updateGyroscopeSupport: (Boolean)->Unit = {}
    fun updateGyroscopeSupport(flag:Boolean){
        updateGyroscopeSupport.invoke(flag)
    }
    var updateAccelerometerData: (Map<String, Float>) -> Unit = {}
    fun updateAccelerometerData(data: Map<String, Float>) {
        updateAccelerometerData.invoke(data)
    }
    var updateAccelerometerSupport: (Boolean)->Unit = {}
    fun updateAccelerometerSupport(flag:Boolean){
        updateAccelerometerSupport.invoke(flag)
    }
    var updateAmbientTemperatureData: (Map<String, Float>) -> Unit = {}
    fun updateAmbientTemperatureData(data:Map<String, Float>) {
        updateAmbientTemperatureData.invoke(data)
    }
    var updateAmbientTemperatureSupport: (Boolean)->Unit = {}
    fun updateAmbientTemperatureSupport(flag:Boolean){
        updateAmbientTemperatureSupport.invoke(flag)
    }
}
