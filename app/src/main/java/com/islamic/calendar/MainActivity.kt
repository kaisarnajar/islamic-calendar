package com.islamic.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.islamic.calendar.ui.IslamicCalendarRoute
import com.islamic.calendar.ui.theme.IslamicCalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IslamicCalendarTheme {
                val viewModel: HijriViewModel = viewModel(
                    factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application),
                )
                IslamicCalendarRoute(viewModel)
            }
        }
    }
}
