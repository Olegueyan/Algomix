package fr.olegueyan.algomix.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.ui.LocalAppContainer
import fr.olegueyan.algomix.ui.components.common.AlgomixCard
import fr.olegueyan.algomix.ui.components.common.AlgomixOutlinedButton
import fr.olegueyan.algomix.ui.components.common.AlgomixPrimaryButton
import fr.olegueyan.algomix.ui.rememberTimerViewModel
import fr.olegueyan.algomix.ui.state.TimerRunState
import fr.olegueyan.algomix.ui.theme.AlgomixPalette
import fr.olegueyan.algomix.ui.theme.AlgomixSpacing
import kotlinx.coroutines.delay

private const val REFRESH_PERIOD_MS = 50L

@Composable
fun TimerScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = rememberTimerViewModel(container)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.runState) {
        if (state.runState == TimerRunState.RUNNING) {
            while (true) {
                delay(REFRESH_PERIOD_MS)
                viewModel.refreshElapsed()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(PaddingValues(horizontal = AlgomixSpacing.lg, vertical = AlgomixSpacing.md)),
        verticalArrangement = Arrangement.spacedBy(AlgomixSpacing.md),
    ) {
        Text(
            text = stringResource(R.string.timer_title),
            color = AlgomixPalette.BrandBlue,
            style = MaterialTheme.typography.headlineMedium,
        )

        state.feedbackMessage?.let { feedback ->
            Text(
                text = feedback,
                color = if (state.isError) AlgomixPalette.Danger else AlgomixPalette.TextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        AlgomixCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            title = stringResource(R.string.timer_history_title),
        ) {
            if (state.history.isEmpty()) {
                Text(
                    text = stringResource(R.string.timer_history_empty),
                    color = AlgomixPalette.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AlgomixSpacing.xs),
                ) {
                    items(state.history, key = { it.id.value }) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AlgomixPalette.SurfaceSoftBlue, RoundedCornerShape(10.dp))
                                .padding(AlgomixSpacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = entry.durationLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = AlgomixPalette.BrandBlue,
                            )
                            Text(
                                text = entry.solvedAtLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = AlgomixPalette.TextMuted,
                            )
                        }
                    }
                }
            }
        }

        AlgomixCard(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(AlgomixSpacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.durationLabel,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AlgomixPalette.BrandBlue,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = AlgomixSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AlgomixSpacing.sm),
            ) {
                val startLabel = when (state.runState) {
                    TimerRunState.IDLE -> stringResource(R.string.timer_start)
                    TimerRunState.RUNNING -> stringResource(R.string.timer_pause)
                    TimerRunState.PAUSED -> stringResource(R.string.timer_resume)
                }
                AlgomixPrimaryButton(
                    text = startLabel,
                    onClick = viewModel::startOrPause,
                    modifier = Modifier.weight(1f),
                )
                AlgomixOutlinedButton(
                    text = stringResource(R.string.timer_reset),
                    onClick = viewModel::reset,
                    modifier = Modifier.weight(1f),
                )
                AlgomixOutlinedButton(
                    text = stringResource(R.string.timer_save_time),
                    onClick = viewModel::saveTime,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
