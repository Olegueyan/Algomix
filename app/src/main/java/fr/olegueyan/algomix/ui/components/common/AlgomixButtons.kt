package fr.olegueyan.algomix.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import fr.olegueyan.algomix.ui.theme.AlgomixPalette
import fr.olegueyan.algomix.ui.theme.AlgomixSpacing

@Composable
fun AlgomixPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 36.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AlgomixPalette.OrangeSoftBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AlgomixPalette.OrangeSoftBg,
            contentColor = AlgomixPalette.OrangeOnSoft,
            disabledContainerColor = AlgomixPalette.DisabledBg,
            disabledContentColor = AlgomixPalette.DisabledText,
        ),
        contentPadding = PaddingValues(horizontal = AlgomixSpacing.lg, vertical = AlgomixSpacing.sm),
    ) {
        ButtonContent(text, leadingIcon)
    }
}

@Composable
fun AlgomixGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 36.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AlgomixPalette.BlueGhostBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AlgomixPalette.BlueGhostBg,
            contentColor = AlgomixPalette.BlueGhostText,
            disabledContainerColor = AlgomixPalette.DisabledBg,
            disabledContentColor = AlgomixPalette.DisabledText,
        ),
        contentPadding = PaddingValues(horizontal = AlgomixSpacing.lg, vertical = AlgomixSpacing.sm),
    ) {
        ButtonContent(text, leadingIcon)
    }
}

@Composable
fun AlgomixOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 36.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AlgomixPalette.Divider),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AlgomixPalette.SurfaceWhite,
            contentColor = AlgomixPalette.TextPrimary,
            disabledContainerColor = AlgomixPalette.DisabledBg,
            disabledContentColor = AlgomixPalette.DisabledText,
        ),
        contentPadding = PaddingValues(horizontal = AlgomixSpacing.lg, vertical = AlgomixSpacing.sm),
    ) {
        ButtonContent(text, leadingIcon)
    }
}

@Composable
fun AlgomixDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 36.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AlgomixPalette.DangerSoftBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AlgomixPalette.DangerSoftBg,
            contentColor = AlgomixPalette.DangerOnSoft,
            disabledContainerColor = AlgomixPalette.DisabledBg,
            disabledContentColor = AlgomixPalette.DisabledText,
        ),
        contentPadding = PaddingValues(horizontal = AlgomixSpacing.lg, vertical = AlgomixSpacing.sm),
    ) {
        ButtonContent(text, null)
    }
}

@Composable
fun AlgomixIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AlgomixPalette.TextPrimary,
    container: Color = AlgomixPalette.SurfaceWhite,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(34.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = container,
            contentColor = tint,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = tint,
        )
    }
}

@Composable
private fun ButtonContent(text: String, leadingIcon: ImageVector?) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(AlgomixSpacing.xs))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
