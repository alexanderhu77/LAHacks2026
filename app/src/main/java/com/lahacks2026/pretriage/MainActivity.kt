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
import com.lahacks2026.pretriage.ui.PreTriageNavGraph
import com.lahacks2026.pretriage.ui.theme.AppTheme
import com.lahacks2026.pretriage.ui.theme.LocalAppPalette
import com.lahacks2026.pretriage.ui.theme.paletteFor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppViewModel = viewModel()
            val state by vm.state.collectAsState()
            val palette = paletteFor(state.theme, state.largeType)

            AppTheme(palette = palette) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalAppPalette.current.bg)
                ) {
                    PreTriageNavGraph(vm = vm)
                }
            }
        }
    }
}
