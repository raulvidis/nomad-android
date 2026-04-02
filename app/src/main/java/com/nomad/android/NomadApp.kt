package com.nomad.android

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NomadApp : Application()

@Composable
fun NomadApp() {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Text(
            text = "NOMAD",
            modifier = Modifier.padding(innerPadding)
        )
    }
}
