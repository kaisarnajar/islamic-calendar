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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Brightness2
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

private object NavDestinations {
    const val MAIN_TABS = "main_tabs"
    const val SETTINGS = "settings"
}

private object TabRoutes {
    const val DATE = "tab_date"
    const val MOON = "tab_moon"
    const val MONTHS = "tab_months"
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

    val outerNavController = rememberNavController()

    NavHost(
        navController = outerNavController,
        startDestination = NavDestinations.MAIN_TABS,
    ) {
        composable(NavDestinations.MAIN_TABS) {
            MainTabsScreen(
                state = state,
                locationGranted = coarseLocation.status.isGranted,
                shouldShowRationale = coarseLocation.status.shouldShowRationale,
                onRequestLocation = { coarseLocation.launchPermissionRequest() },
                onOpenSettings = { outerNavController.navigate(NavDestinations.SETTINGS) },
            )
        }
        composable(NavDestinations.SETTINGS) {
            SettingsRoute(
                viewModel = viewModel,
                onNavigateBack = { outerNavController.popBackStack() },
            )
        }
    }
}

private fun navigateTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun screenGradientBrush(): Brush {
    return Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTabsScreen(
    state: HijriUiState,
    locationGranted: Boolean,
    shouldShowRationale: Boolean,
    onRequestLocation: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: TabRoutes.DATE
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (currentRoute) {
                            TabRoutes.MOON -> stringResource(R.string.nav_moon_cycle)
                            TabRoutes.MONTHS -> stringResource(R.string.nav_islamic_months)
                            else -> stringResource(R.string.nav_islamic_date)
                        },
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.open_settings),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = stringResource(R.string.cd_nav_tab_date),
                        )
                    },
                    label = { Text(stringResource(R.string.nav_islamic_date)) },
                    selected = currentRoute == TabRoutes.DATE,
                    onClick = { navigateTab(tabNavController, TabRoutes.DATE) },
                    colors = NavigationBarItemDefaults.colors(),
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Outlined.Brightness2,
                            contentDescription = stringResource(R.string.cd_nav_tab_moon),
                        )
                    },
                    label = { Text(stringResource(R.string.nav_moon_cycle)) },
                    selected = currentRoute == TabRoutes.MOON,
                    onClick = { navigateTab(tabNavController, TabRoutes.MOON) },
                    colors = NavigationBarItemDefaults.colors(),
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = stringResource(R.string.cd_nav_tab_months),
                        )
                    },
                    label = { Text(stringResource(R.string.nav_islamic_months)) },
                    selected = currentRoute == TabRoutes.MONTHS,
                    onClick = { navigateTab(tabNavController, TabRoutes.MONTHS) },
                    colors = NavigationBarItemDefaults.colors(),
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = TabRoutes.DATE,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TabRoutes.DATE) {
                IslamicDateTab(
                    state = state,
                    locationGranted = locationGranted,
                    shouldShowRationale = shouldShowRationale,
                    onRequestLocation = onRequestLocation,
                )
            }
            composable(TabRoutes.MOON) {
                MoonCycleTab(state = state)
            }
            composable(TabRoutes.MONTHS) {
                IslamicMonthsTab()
            }
        }
    }
}

@Composable
private fun IslamicDateTab(
    state: HijriUiState,
    locationGranted: Boolean,
    shouldShowRationale: Boolean,
    onRequestLocation: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val gregorianFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenGradientBrush())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column(
                modifier = Modifier.padding(vertical = 32.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.nav_islamic_date),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(16.dp))
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
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(
                        R.string.hero_gregorian,
                        state.hijri.gregorian.format(gregorianFormatter),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
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

@Composable
private fun MoonCycleTab(state: HijriUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenGradientBrush())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
                Spacer(Modifier.height(16.dp))
                LunarCycleSection(moon = state.moon, zoneId = state.zoneId)
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
    }
}

@Composable
private fun IslamicMonthsTab() {
    val months = (1..12).toList()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenGradientBrush()),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text(
                    text = stringResource(R.string.tab_months_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
            items(months, key = { it }) { monthIndex ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(hijriMonthStringRes(monthIndex)),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.month_order_format, monthIndex),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun LunarCycleSection(moon: MoonPhaseInfo, zoneId: ZoneId) {
    val progressPct = (moon.phase * 100.0).roundToInt().coerceIn(0, 100)
    val illumPct = (moon.illuminatedFraction * 100.0).roundToInt().coerceIn(0, 100)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.lunar_cycle_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.moon_cycle_subtitle, zoneId.id),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { moon.phase.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.moon_age_days, moon.ageDays),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.moon_lunation_progress, progressPct),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.moon_illumination_percent, illumPct),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(
                if (moon.waxing) R.string.moon_trend_waxing else R.string.moon_trend_waning,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.moon_next_full, moon.daysUntilNextFullMoon),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.moon_next_new, moon.daysUntilNextNewMoon),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
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
