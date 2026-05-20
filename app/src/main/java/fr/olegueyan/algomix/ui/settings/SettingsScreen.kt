package fr.olegueyan.algomix.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.domain.settings.AppAppearance
import fr.olegueyan.algomix.domain.settings.CubeTheme
import fr.olegueyan.algomix.ui.LocalAppContainer
import fr.olegueyan.algomix.ui.components.common.AlgomixCard
import fr.olegueyan.algomix.ui.components.common.AlgomixDangerButton
import fr.olegueyan.algomix.ui.components.common.AlgomixGhostButton
import fr.olegueyan.algomix.ui.components.common.AlgomixOutlinedButton
import fr.olegueyan.algomix.ui.components.common.AlgomixPrimaryButton
import fr.olegueyan.algomix.ui.components.common.AlgomixSegmented
import fr.olegueyan.algomix.ui.rememberSettingsViewModel
import fr.olegueyan.algomix.ui.theme.AlgomixPalette
import fr.olegueyan.algomix.ui.theme.AlgomixSpacing

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = rememberSettingsViewModel(container)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val cubeThemeLabels = mapOf(
        CubeTheme.FILLED to stringResource(R.string.settings_theme_filled),
        CubeTheme.STICKER_ON_BLACK to stringResource(R.string.settings_theme_sticker),
        CubeTheme.CARBON to stringResource(R.string.settings_theme_carbon),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(PaddingValues(horizontal = AlgomixSpacing.lg, vertical = AlgomixSpacing.md)),
        verticalArrangement = Arrangement.spacedBy(AlgomixSpacing.md),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            color = AlgomixPalette.BrandBlue,
            style = MaterialTheme.typography.headlineMedium,
        )

        state.feedbackMessage?.let { msg ->
            Text(
                text = msg,
                color = if (state.isError) AlgomixPalette.Danger else AlgomixPalette.TextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(AlgomixSpacing.md),
        ) {
            // Appearance card
            itemsIndexed(SETTINGS_SECTIONS) { index, _ ->
                when (index) {
                    0 -> AppearanceCard(state.preferences.appAppearance, viewModel::setAppAppearance)
                    1 -> CubeThemeCard(state.preferences.cubeTheme, cubeThemeLabels, viewModel::setCubeTheme)
                    2 -> PersistenceCard(
                        localCacheEnabled = state.preferences.localCubeCacheEnabled,
                        sessionPersistenceEnabled = state.preferences.sessionPersistenceEnabled,
                        onLocalCacheToggle = viewModel::setLocalCubeCacheEnabled,
                        onSessionToggle = viewModel::setSessionPersistenceEnabled,
                    )
                    3 -> CloudCard(
                        isAuthenticated = state.isAuthenticated,
                        email = state.cloudSession?.user?.email,
                        onSignIn = { /* TODO: open sign-in dialog */ },
                        onCreate = { /* TODO: open create-account dialog */ },
                        onSignOut = viewModel::signOut,
                        onRecover = viewModel::recoverCloud,
                        onPurge = viewModel::purgeCloud,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceCard(current: AppAppearance, onSelect: (AppAppearance) -> Unit) {
    val labels = mapOf(
        AppAppearance.LIGHT to stringResource(R.string.settings_light_mode),
        AppAppearance.DARK to stringResource(R.string.settings_dark_mode),
    )
    AlgomixCard(title = stringResource(R.string.settings_app_appearance)) {
        AlgomixSegmented(
            options = AppAppearance.entries,
            selected = current,
            onSelect = onSelect,
            label = { labels.getValue(it) },
        )
    }
}

@Composable
private fun CubeThemeCard(
    current: CubeTheme,
    labels: Map<CubeTheme, String>,
    onSelect: (CubeTheme) -> Unit,
) {
    AlgomixCard(title = stringResource(R.string.settings_cube_themes)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AlgomixSpacing.xs),
        ) {
            CubeTheme.entries.forEach { theme ->
                if (current == theme) {
                    AlgomixPrimaryButton(
                        text = labels.getValue(theme),
                        onClick = { onSelect(theme) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    AlgomixOutlinedButton(
                        text = labels.getValue(theme),
                        onClick = { onSelect(theme) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PersistenceCard(
    localCacheEnabled: Boolean,
    sessionPersistenceEnabled: Boolean,
    onLocalCacheToggle: (Boolean) -> Unit,
    onSessionToggle: (Boolean) -> Unit,
) {
    AlgomixCard(title = stringResource(R.string.settings_data_persistence)) {
        SwitchRow(
            label = stringResource(R.string.settings_local_cube_cache),
            info = stringResource(R.string.settings_local_cube_cache_info),
            checked = localCacheEnabled,
            onToggle = onLocalCacheToggle,
        )
        SwitchRow(
            label = stringResource(R.string.settings_session_persistence),
            info = stringResource(R.string.settings_session_persistence_info),
            checked = sessionPersistenceEnabled,
            onToggle = onSessionToggle,
        )
    }
}

@Composable
private fun SwitchRow(label: String, info: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = AlgomixSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = AlgomixPalette.TextPrimary, style = MaterialTheme.typography.bodyMedium)
            Text(text = info, color = AlgomixPalette.TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AlgomixPalette.SurfaceWhite,
                checkedTrackColor = AlgomixPalette.OrangeSoftBorder,
                checkedBorderColor = AlgomixPalette.OrangeSoftBorder,
            ),
        )
    }
}

@Composable
private fun CloudCard(
    isAuthenticated: Boolean,
    email: String?,
    onSignIn: () -> Unit,
    onCreate: () -> Unit,
    onSignOut: () -> Unit,
    onRecover: () -> Unit,
    onPurge: () -> Unit,
) {
    AlgomixCard(title = stringResource(R.string.settings_cloud_sync)) {
        Text(
            text = if (isAuthenticated && email != null) {
                stringResource(R.string.settings_cloud_connected_format, email)
            } else {
                stringResource(R.string.settings_cloud_disconnected)
            },
            color = AlgomixPalette.TextMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!isAuthenticated) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = AlgomixSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AlgomixSpacing.xs),
            ) {
                AlgomixPrimaryButton(
                    text = stringResource(R.string.settings_sign_in),
                    onClick = onSignIn,
                    modifier = Modifier.weight(1f)
                )
                AlgomixGhostButton(
                    text = stringResource(R.string.settings_create_account),
                    onClick = onCreate,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = AlgomixSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AlgomixSpacing.xs),
            ) {
                AlgomixGhostButton(
                    text = stringResource(R.string.settings_recover_cloud),
                    onClick = onRecover,
                    modifier = Modifier.weight(1f)
                )
                AlgomixDangerButton(
                    text = stringResource(R.string.settings_purge_cloud),
                    onClick = onPurge,
                    modifier = Modifier.weight(1f)
                )
            }
            AlgomixOutlinedButton(
                text = stringResource(R.string.settings_sign_out),
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().padding(top = AlgomixSpacing.xs),
            )
        }
    }
}

private val SETTINGS_SECTIONS = listOf("appearance", "cube_theme", "persistence", "cloud")
