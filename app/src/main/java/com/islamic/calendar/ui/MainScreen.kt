package com.islamic.calendar.ui

import android.Manifest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MoonCycleHeroSection(moon = state.moon)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MoonCycleStatsCard(moon = state.moon, zoneId = state.zoneId)
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (state.usedGeocoderTimezone) {
                    stringResource(R.string.location_using_timezone, state.zoneId.id)
                } else {
                    stringResource(R.string.location_system_timezone)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun moonNightSkyBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            Color(0xFF050810),
            Color(0xFF0F1B2E),
            Color(0xFF1A3552),
            Color(0xFF243D5C),
        ),
    )
}

@Composable
private fun MoonCycleHeroSection(moon: MoonPhaseInfo) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(moonNightSkyBrush()),
        )
        MoonStarfield(modifier = Modifier.matchParentSize())
        MoonOrbitRings(modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MoonPhaseHeroOrb(
                moon = moon,
                modifier = Modifier.size(232.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(moon.label.stringRes()),
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFFFF4DC),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 28.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.lunar_cycle_title),
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.2.sp),
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun MoonStarfield(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        repeat(56) { i ->
            val x = (abs(sin(i * 12.9898)) * w).toFloat()
            val y = (abs(cos(i * 9.542 + 2.17)) * h).toFloat()
            val r = 0.8f + (i % 4) * 0.55f
            val a = 0.12f + (i % 7) * 0.055f
            drawCircle(
                color = Color.White.copy(alpha = a.coerceIn(0.08f, 0.55f)),
                radius = r,
                center = Offset(x, y),
            )
        }
    }
}

@Composable
private fun MoonOrbitRings(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height / 2f + size.height * 0.02f)
        val stroke = Stroke(width = 1.dp.toPx())
        listOf(0.42f, 0.52f, 0.62f).forEachIndexed { idx, fr ->
            drawCircle(
                color = Color.White.copy(alpha = 0.06f + idx * 0.025f),
                radius = size.minDimension * fr,
                center = c,
                style = stroke,
            )
        }
    }
}

@Composable
private fun MoonPhaseHeroOrb(moon: MoonPhaseInfo, modifier: Modifier = Modifier) {
    val haloGold = Color(0xFFE8C547)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        haloGold.copy(alpha = 0.42f),
                        haloGold.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    center = c,
                    radius = maxR * 1.18f,
                ),
                radius = maxR * 1.12f,
                center = c,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = maxR * 0.94f,
                center = c,
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
        MoonPhaseVisual(
            moon = moon,
            modifier = Modifier.size(208.dp),
        )
    }
}

@Composable
private fun MoonCycleStatsCard(moon: MoonPhaseInfo, zoneId: ZoneId) {
    val progressPct = (moon.phase * 100.0).roundToInt().coerceIn(0, 100)
    val illumPct = (moon.illuminatedFraction * 100.0).roundToInt().coerceIn(0, 100)
    val scheme = MaterialTheme.colorScheme
    val progressBrush = Brush.horizontalGradient(
        colors = listOf(
            scheme.primary,
            scheme.tertiary,
            scheme.primary.copy(alpha = 0.85f),
        ),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.moon_at_a_glance),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                )
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (moon.waxing) {
                        scheme.primaryContainer.copy(alpha = 0.65f)
                    } else {
                        scheme.secondaryContainer.copy(alpha = 0.65f)
                    },
                ) {
                    Text(
                        text = stringResource(
                            if (moon.waxing) R.string.moon_trend_waxing else R.string.moon_trend_waning,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        lineHeight = 16.sp,
                        color = if (moon.waxing) scheme.onPrimaryContainer else scheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        maxLines = 2,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MoonMetricTile(
                    label = stringResource(R.string.moon_stat_label_age),
                    value = stringResource(R.string.moon_stat_age_value, moon.ageDays),
                    modifier = Modifier.weight(1f),
                )
                MoonMetricTile(
                    label = stringResource(R.string.moon_stat_label_lit),
                    value = stringResource(R.string.moon_stat_lit_value, illumPct),
                    modifier = Modifier.weight(1f),
                )
                MoonMetricTile(
                    label = stringResource(R.string.moon_stat_label_lunation),
                    value = stringResource(R.string.moon_stat_lunation_value, progressPct),
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.45f))

            Text(
                text = stringResource(R.string.moon_lunation_track),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(scheme.surfaceVariant.copy(alpha = 0.65f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(moon.phase.toFloat())
                        .align(Alignment.CenterStart)
                        .background(progressBrush),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.moon_new),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.moon_full),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.moon_new),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            Text(
                text = stringResource(R.string.moon_milestones_title),
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MoonMilestoneTile(
                    title = stringResource(R.string.moon_full),
                    subtitle = stringResource(R.string.moon_next_full, moon.daysUntilNextFullMoon),
                    modifier = Modifier.weight(1f),
                    containerColor = scheme.primaryContainer.copy(alpha = 0.35f),
                    contentColor = scheme.onPrimaryContainer,
                )
                MoonMilestoneTile(
                    title = stringResource(R.string.moon_new),
                    subtitle = stringResource(R.string.moon_next_new, moon.daysUntilNextNewMoon),
                    modifier = Modifier.weight(1f),
                    containerColor = scheme.secondaryContainer.copy(alpha = 0.35f),
                    contentColor = scheme.onSecondaryContainer,
                )
            }

            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.35f))

            Text(
                text = stringResource(R.string.moon_cycle_subtitle, zoneId.id),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.88f),
            )
        }
    }
}

@Composable
private fun MoonMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.widthIn(min = 0.dp),
        shape = RoundedCornerShape(18.dp),
        color = scheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.primary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MoonMilestoneTile(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor.copy(alpha = 0.85f),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
            )
        }
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
