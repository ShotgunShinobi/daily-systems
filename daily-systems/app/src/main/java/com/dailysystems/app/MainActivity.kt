package com.dailysystems.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dailysystems.app.ui.DailySystemsRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as DailySystemsApp).container
        setContent { DailySystemsRoot(container) }
    }

    /** Coming back after midnight: Today moves to the new date straight away. */
    override fun onResume() {
        super.onResume()
        val app = application as DailySystemsApp
        app.container.refreshToday()
        app.dayRollover.schedule()
    }
}
