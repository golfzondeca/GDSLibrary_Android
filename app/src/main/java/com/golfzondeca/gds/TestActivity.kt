package com.golfzondeca.gds

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
@ExperimentalComposeUiApi
class TestActivity: ComponentActivity() {
    private val gdsRepository by lazy {
        GDSRepository(this, "", "")
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

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(Environment.isExternalStorageManager()) {

            } else {
                try {
                    startActivity(
                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                            addCategory("android.intent.category.DEFAULT")
                            data = Uri.parse(String.format("package:%s", application.packageName))
                            startActivity(intent)
                        }
                    )
                } catch (e: Exception) {
                    startActivity(
                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                            addCategory("android.intent.category.DEFAULT")
                            startActivity(intent)
                        }
                    )
                }
            }
        } else {
            TODO("VERSION.SDK_INT < R")
        }
    }
}

@Composable
@ExperimentalComposeUiApi
fun TestScreen() {
    val viewModel: TestViewModel = viewModel()

    val keyboardController = LocalSoftwareKeyboardController.current

    val altitudeData: Int? by viewModel.altitudeData.observeAsState(null)
    val holeMapData: Bitmap? by viewModel.holeMapData.observeAsState(null)
    val undulationMapData: ArrayList<Bitmap>? by viewModel.undulationMapData.observeAsState(null)

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

        Row {
            TextButton(
                modifier = Modifier.padding(1.dp, 0.dp),
                onClick = {
                    keyboardController?.hide()
                    viewModel.loadRemoteData()
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Blue,
                    contentColor = Color.White
                ),
            ) {
                Text(text = "Load Remote")
            }

            Spacer(modifier = Modifier.width(12.dp))
            TextButton(
                modifier = Modifier.padding(1.dp, 0.dp),
                onClick = {
                    keyboardController?.hide()
                    viewModel.loadAssetData()
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Blue,
                    contentColor = Color.White
                ),
            ) {
                Text(text = "Load Asset")
            }

            Spacer(modifier = Modifier.width(12.dp))
            TextButton(
                modifier = Modifier.padding(1.dp, 0.dp),
                onClick = {
                    keyboardController?.hide()
                    viewModel.loadFileData()
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Blue,
                    contentColor = Color.White
                ),
            ) {
                Text(text = "Load File")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if(altitudeData != null)
            Text ("Altitude: $altitudeData")
        else
            Text ("Altitude: none")

        Spacer(modifier = Modifier.height(12.dp))

        Row {
            holeMapData?.let {
                Image(bitmap = it.asImageBitmap(), contentDescription = "")
            }

            undulationMapData?.let { list ->
                list.forEach {
                    Spacer(modifier = Modifier.width(12.dp))
                    Image(bitmap = it.asImageBitmap(), contentDescription = "")
                }
            }
        }
    }
}