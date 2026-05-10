package com.islamic.calendar.ui

import android.Manifest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.islamic.calendar.HijriUiState
import com.islamic.calendar.HijriViewModel
import com.islamic.calendar.R
import com.islamic.calendar.domain.MoonPhaseInfo
import com.islamic.calendar.domain.hijriMonthStringRes
import com.islamic.calendar.domain.stringRes
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private object NavDestinations {
    const val HOME = "home"
    const val SETTINGS = "settings"
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun IslamicCalendarRoute(viewModel: HijriViewModel) {
    val coarseLocation = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(coarseLocation.status.isGranted) {
        if (coarseLocation.status.isGranted) {
            viewModel.refreshZoneFromLocation()
        } else {
            viewModel.clearLocationZone()
        }
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavDestinations.HOME,
    ) {
        composable(NavDestinations.HOME) {
            MainScreen(
                state = state,
                locationGranted = coarseLocation.status.isGranted,
                shouldShowRationale = coarseLocation.status.shouldShowRationale,
                onRequestLocation = { coarseLocation.launchPermissionRequest() },
                onOpenSettings = { navController.navigate(NavDestinations.SETTINGS) },
            )
        }
        composable(NavDestinations.SETTINGS) {
            SettingsRoute(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun MainScreen(
    state: HijriUiState,
    locationGranted: Boolean,
    shouldShowRationale: Boolean,
    onRequestLocation: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val gregorianFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(2f),
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.open_settings),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 28.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MoonPhaseVisual(
                        moon = state.moon,
                        modifier = Modifier.size(220.dp),
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(state.moon.label.stringRes()),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(
                            R.string.hero_gregorian,
                            state.hijri.gregorian.format(gregorianFormatter),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))

                    AnimatedContent(
                        targetState = Triple(state.hijri.dayOfMonth, state.hijri.monthValue, state.hijri.year),
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "hijriDate",
                    ) { (day, month, year) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(hijriMonthStringRes(month)),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = year.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = if (state.usedGeocoderTimezone) {
                    stringResource(R.string.location_using_timezone, state.zoneId.id)
                } else {
                    stringResource(R.string.location_system_timezone)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
            )

            if (!locationGranted) {
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = onRequestLocation,
                    modifier = Modifier.fillMaxWidth(0.85f),
                ) {
                    Text(stringResource(R.string.grant_location))
                }
                if (shouldShowRationale) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.permission_rationale),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (state.isRefreshingLocation) {
                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                )
            }
        }
    }
}

@Composable
private fun MoonPhaseVisual(moon: MoonPhaseInfo, modifier: Modifier = Modifier) {
    val disk = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
    val shadowTint = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    val glow = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = minOf(cx, cy) * 0.88f
                val illum = moon.illuminatedFraction.toFloat()
                val dx = 2f * r * (illum - 0.5f) * if (moon.waxing) -1f else 1f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glow, Color.Transparent),
                        center = Offset(cx, cy),
                        radius = r * 1.35f,
                    ),
                    radius = r * 1.2f,
                    center = Offset(cx, cy),
                )

                val moonDisk = Path().apply {
                    addOval(Rect(cx - r, cy - r, cx + r, cy + r))
                }
                val shadowDisk = Path().apply {
                    addOval(Rect(cx + dx - r, cy - r, cx + dx + r, cy + r))
                }
                val lit = Path()
                lit.op(moonDisk, shadowDisk, PathOperation.Difference)

                drawPath(path = lit, color = disk)

                drawCircle(
                    color = shadowTint.copy(alpha = 0.08f),
                    radius = r,
                    center = Offset(cx, cy),
                )
            }
        }
    }
}
