package fr.olegueyan.algomix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.ui.state.MainRoute
import fr.olegueyan.algomix.ui.theme.AlgomixPalette

@Composable
fun AlgomixBottomBar(
    selected: MainRoute,
    onSelect: (MainRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(AlgomixPalette.SurfaceWhite)
                .border(1.dp, AlgomixPalette.Divider),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomItem(
                label = stringResource(R.string.nav_home),
                icon = Icons.Outlined.ViewInAr,
                selected = selected == MainRoute.HOME,
                onClick = { onSelect(MainRoute.HOME) },
            )
            BottomItem(
                label = stringResource(R.string.nav_library),
                icon = Icons.AutoMirrored.Outlined.LibraryBooks,
                selected = selected == MainRoute.LIBRARY,
                onClick = { onSelect(MainRoute.LIBRARY) },
            )
            BottomItem(
                label = stringResource(R.string.nav_timer),
                icon = Icons.Outlined.Timer,
                selected = selected == MainRoute.TIMER,
                onClick = { onSelect(MainRoute.TIMER) },
            )
            BottomItem(
                label = stringResource(R.string.nav_settings),
                icon = Icons.Outlined.Settings,
                selected = selected == MainRoute.SETTINGS,
                onClick = { onSelect(MainRoute.SETTINGS) },
            )
        }
    }
}

@Composable
private fun BottomItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tint: Color = if (selected) AlgomixPalette.OrangePrimary else AlgomixPalette.TextMuted
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) AlgomixPalette.OrangeSoftBg else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = tint, style = MaterialTheme.typography.labelSmall)
    }
}
