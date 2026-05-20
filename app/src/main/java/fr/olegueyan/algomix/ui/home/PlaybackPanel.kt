package fr.olegueyan.algomix.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.domain.cube.PlaybackState
import fr.olegueyan.algomix.ui.components.common.AlgomixGhostButton
import fr.olegueyan.algomix.ui.components.common.AlgomixOutlinedButton
import fr.olegueyan.algomix.ui.components.common.AlgomixPrimaryButton
import fr.olegueyan.algomix.ui.components.common.AlgomixProgressBar
import fr.olegueyan.algomix.ui.theme.AlgomixPalette
import fr.olegueyan.algomix.ui.theme.AlgomixSpacing

@Composable
fun PlaybackPanel(
    playbackState: PlaybackState,
    autoPlayEnabled: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit,
    onToggleAuto: () -> Unit,
    onToggleLoop: () -> Unit,
    onCycleSpeed: () -> Unit,
    onLoadAlgorithm: () -> Unit,
    onLoadScramble: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = playbackState.sequence.moves.size
    val progress = if (total == 0) 0f else playbackState.currentIndex.toFloat() / total
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AlgomixPalette.SurfaceWhite, RoundedCornerShape(12.dp))
            .border(1.dp, AlgomixPalette.Divider, RoundedCornerShape(12.dp))
            .padding(AlgomixSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AlgomixSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AlgomixSpacing.sm),
        ) {
            AlgomixGhostButton(
                text = stringResource(R.string.home_load_algorithm),
                onClick = onLoadAlgorithm,
                modifier = Modifier.weight(1f),
            )
            AlgomixGhostButton(
                text = stringResource(R.string.home_load_scramble),
                onClick = onLoadScramble,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = stringResource(R.string.home_progress_format, playbackState.currentIndex, total),
            color = AlgomixPalette.TextMuted,
            style = MaterialTheme.typography.labelMedium,
        )
        AlgomixProgressBar(progress = progress)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AlgomixSpacing.xs),
        ) {
            AlgomixOutlinedButton(
                text = stringResource(R.string.home_previous),
                onClick = onPrev,
                modifier = Modifier.weight(1f)
            )
            AlgomixOutlinedButton(
                text = stringResource(R.string.home_next),
                onClick = onNext,
                modifier = Modifier.weight(1f)
            )
            AlgomixOutlinedButton(
                text = stringResource(R.string.home_speed_format, playbackState.speedMultiplier.toString()),
                onClick = onCycleSpeed,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AlgomixSpacing.xs),
        ) {
            val autoLabel = if (autoPlayEnabled) {
                stringResource(R.string.home_toggle_on_format, stringResource(R.string.home_auto))
            } else {
                stringResource(R.string.home_toggle_off_format, stringResource(R.string.home_auto))
            }
            val loopLabel = if (playbackState.loop) {
                stringResource(R.string.home_toggle_on_format, stringResource(R.string.home_loop))
            } else {
                stringResource(R.string.home_toggle_off_format, stringResource(R.string.home_loop))
            }
            AlgomixOutlinedButton(text = autoLabel, onClick = onToggleAuto, modifier = Modifier.weight(1f))
            AlgomixOutlinedButton(text = loopLabel, onClick = onToggleLoop, modifier = Modifier.weight(1f))
            AlgomixPrimaryButton(
                text = stringResource(R.string.home_reset),
                onClick = onReset,
                modifier = Modifier.weight(1f)
            )
        }
        SequenceText(playbackState = playbackState)
    }
}

@Composable
private fun SequenceText(playbackState: PlaybackState) {
    val annotated: AnnotatedString = if (playbackState.sequence.moves.isEmpty()) {
        AnnotatedString(stringResource(R.string.home_empty_sequence))
    } else {
        buildAnnotatedString {
            playbackState.sequence.moves.forEachIndexed { index, move ->
                val style = when {
                    index < playbackState.currentIndex -> SpanStyle(color = AlgomixPalette.TextMuted)
                    index == playbackState.currentIndex -> SpanStyle(
                        color = AlgomixPalette.OrangeOnSoft,
                        background = AlgomixPalette.OrangeSoftBg,
                    )
                    else -> SpanStyle(color = AlgomixPalette.TextPrimary)
                }
                withStyle(style) {
                    append(move.normalizedNotation)
                }
                append(' ')
            }
        }
    }
    Text(
        text = annotated,
        modifier = Modifier
            .fillMaxWidth()
            .background(AlgomixPalette.SurfaceSoftBlue, RoundedCornerShape(8.dp))
            .padding(AlgomixSpacing.sm),
        style = MaterialTheme.typography.bodyMedium,
    )
}
