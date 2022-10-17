package com.golfzondeca.gds

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class TestActivity: ComponentActivity() {
    private val gdsRepository by lazy {
        GDSRepository(this, "")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")

        setContent {
            Surface(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)) {
                TestScreen()
            }
        }

    }
}

@Composable
fun TestScreen() {
    val viewModel: TestViewModel = viewModel()

    val altitudeData: Int? by viewModel.altitudeData.observeAsState(null)
    val holeMapData: Bitmap? by viewModel.holeMapData.observeAsState(null)

    var ccId by remember { mutableStateOf(TextFieldValue(viewModel.ccID.value)) }
    var countryCode by remember { mutableStateOf(TextFieldValue(viewModel.countryCode.value)) }
    var stateCode by remember { mutableStateOf(TextFieldValue(viewModel.stateCode.value)) }
    var courseCount by remember { mutableStateOf(TextFieldValue(viewModel.courseCount.value)) }
    var courseNum by remember { mutableStateOf(TextFieldValue(viewModel.courseNum.value)) }
    var latitude by remember { mutableStateOf(TextFieldValue(viewModel.latitude.value)) }
    var longitude by remember { mutableStateOf(TextFieldValue(viewModel.longitude.value)) }
    var holeNum by remember { mutableStateOf(TextFieldValue(viewModel.holeNum.value)) }

    Column {
        OutlinedTextField(
            modifier = Modifier.width(200.dp),
            value = ccId,
            onValueChange = {
                ccId = it
                viewModel.onCCIDChanged(it.text)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = {
                Text("CC ID")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row {
            OutlinedTextField(
                modifier = Modifier.weight(0.4f),
                value = countryCode,
                onValueChange = {
                    countryCode = it
                    viewModel.onCountryCodeChanged(it.text)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = {
                    Text("Country Code")
                }
            )

            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                modifier = Modifier.weight(0.4f),
                value = stateCode,
                onValueChange = {
                    stateCode = it
                    viewModel.onStateCodeChanged(it.text)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = {
                    Text("State Code")
                }
            )
        }


        Spacer(modifier = Modifier.height(12.dp))

        Row {
            OutlinedTextField(
                modifier = Modifier.weight(0.4f),
                value = courseCount,
                onValueChange = {
                    courseCount = it
                    viewModel.onCourseCountChanged(it.text)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = {
                    Text("Course Count")
                }
            )

            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                modifier = Modifier.weight(0.4f),
                value = courseNum,
                onValueChange = {
                    courseNum = it
                    viewModel.onCourseNumChanged(it.text)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = {
                    Text("Course Number")
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row {
            OutlinedTextField(
                modifier = Modifier.weight(0.4f),
                value = latitude,
                onValueChange = {
                    latitude = it
                    viewModel.onLatitudeChanged(it.text)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = {
                    Text("Latitude")
                }
            )

            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                modifier = Modifier.weight(0.4f),
                value = longitude,
                onValueChange = {
                    longitude = it
                    viewModel.onLongitudeChanged(it.text)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = {
                    Text("Longitude")
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))


        OutlinedTextField(
            modifier = Modifier.width(200.dp),
            value = holeNum,
            onValueChange = {
                holeNum = it
                viewModel.onHoleNumChanged(it.text)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = {
                Text("Hole Number")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = { viewModel.onRequestData() }) {
            Text(text = "Request Data")
        }        

        Spacer(modifier = Modifier.height(24.dp))

        if(altitudeData != null)
            Text ("Altitude: $altitudeData")
        else
            Text ("Altitude: none")

        Spacer(modifier = Modifier.height(12.dp))

        holeMapData?.let {
            Image(bitmap = it.asImageBitmap(), contentDescription = "")
        }
    }
}