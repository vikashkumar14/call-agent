package com.codeninjavik.myra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.codeninjavik.myra.ui.forwarding.ForwardingScreen
import com.codeninjavik.myra.ui.theme.MyraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyraTheme {
                ForwardingScreen()
            }
        }
    }
}
