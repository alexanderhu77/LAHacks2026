package com.lahacks2026.pretriage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lahacks2026.pretriage.ui.AppViewModel
import com.lahacks2026.pretriage.ui.PreTriageNavGraph
import com.lahacks2026.pretriage.ui.theme.NoraAppTheme
import com.lahacks2026.pretriage.ui.theme.NoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppViewModel = viewModel()
            val state by vm.state.collectAsState()
            NoraAppTheme(themeKey = state.theme, largeType = state.largeType) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NoraTheme.colors.bg)
                ) {
                    PreTriageNavGraph()
                }
            }
        }
    }
}
