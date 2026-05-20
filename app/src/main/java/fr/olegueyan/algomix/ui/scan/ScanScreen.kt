package fr.olegueyan.algomix.ui.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.ui.components.common.AlgomixOutlinedButton
import fr.olegueyan.algomix.ui.theme.AlgomixPalette
import fr.olegueyan.algomix.ui.theme.AlgomixSpacing
import fr.olegueyan.algomix.ui.viewmodel.SharedCubeViewModel

@Composable
fun ScanScreen(
    sharedCubeViewModel: SharedCubeViewModel,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AlgomixPalette.BackgroundLight)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(AlgomixSpacing.lg),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AlgomixSpacing.lg, alignment = Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.scan_title),
                    color = AlgomixPalette.BrandBlue,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.scan_preview_unavailable),
                    color = AlgomixPalette.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                AlgomixOutlinedButton(
                    text = androidx.compose.ui.res.stringResource(R.string.scan_cancel),
                    onClick = onDismiss,
                )
            }
        }
    }
}
