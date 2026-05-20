package fr.olegueyan.algomix.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.ui.components.common.AlgomixDangerButton
import fr.olegueyan.algomix.ui.components.common.AlgomixOutlinedButton
import fr.olegueyan.algomix.ui.components.common.AlgomixPrimaryButton
import fr.olegueyan.algomix.ui.theme.AlgomixSpacing

@Composable
fun EditActionsBar(
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSuppress: () -> Unit,
    onDeleteAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AlgomixSpacing.xs),
    ) {
        AlgomixPrimaryButton(
            text = stringResource(R.string.home_save),
            onClick = onSave,
            modifier = Modifier.weight(1f)
        )
        AlgomixOutlinedButton(
            text = stringResource(R.string.home_undo),
            onClick = onUndo,
            modifier = Modifier.weight(1f)
        )
        AlgomixOutlinedButton(
            text = stringResource(R.string.home_redo),
            onClick = onRedo,
            modifier = Modifier.weight(1f)
        )
        AlgomixOutlinedButton(
            text = stringResource(R.string.home_suppress),
            onClick = onSuppress,
            modifier = Modifier.weight(1f)
        )
        AlgomixDangerButton(
            text = stringResource(R.string.home_delete_all),
            onClick = onDeleteAll,
            modifier = Modifier.weight(1f)
        )
    }
}
