package fr.olegueyan.algomix.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class ScanRouteState {
    private var visible by mutableStateOf(false)

    val isVisible: Boolean
        get() = visible

    fun show() {
        visible = true
    }

    fun hide() {
        visible = false
    }
}

object ScanRoute {
    @Composable
    fun rememberState(): ScanRouteState = remember { ScanRouteState() }
}
