package com.engboost.encryptedca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.engboost.encryptedca.navigation.AppNavHost
import com.engboost.encryptedca.ui.theme.EncryptedCaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val repository = (application as EncryptedCaApplication).certificateProfiles
        setContent {
            EncryptedCaTheme {
                AppNavHost(repository)
            }
        }
    }
}
