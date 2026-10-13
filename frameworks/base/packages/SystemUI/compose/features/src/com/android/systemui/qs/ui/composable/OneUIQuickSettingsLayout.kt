/*
 * Copyright (C) 2026 Shinkai Project / Wowo OS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalFoundationApi::class)

package com.android.systemui.qs.ui.composable

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile.STATE_ACTIVE
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.State
import com.android.systemui.qs.footer.ui.viewmodel.FooterActionsViewModel
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import com.android.systemui.brightness.domain.model.GammaBrightness
import com.android.systemui.brightness.ui.viewmodel.BrightnessSliderViewModel
import com.android.systemui.brightness.ui.viewmodel.Drag
import com.android.systemui.qs.panels.ui.compose.TileListener
import com.android.systemui.qs.panels.ui.viewmodel.EditModeViewModel
import com.android.systemui.qs.panels.ui.viewmodel.EditTileViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.PlatformSliderDefaults
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.content.state.TransitionState
import com.android.systemui.animation.Expandable
import com.android.systemui.brightness.ui.compose.BrightnessSliderContainer
import com.android.systemui.brightness.ui.compose.ContainerColors
import com.android.systemui.common.shared.model.Icon as ModelIcon
import com.android.systemui.development.ui.viewmodel.BuildNumberViewModel
import com.android.systemui.lifecycle.rememberViewModel
import com.android.systemui.media.remedia.ui.compose.Media
import com.android.systemui.media.remedia.ui.compose.MediaPresentationStyle
import com.android.systemui.plugins.qs.QSTile
import com.android.systemui.qs.composefragment.ui.GridAnchor
import com.android.systemui.qs.panels.ui.compose.infinitegrid.SmallTileContent
import com.android.systemui.qs.panels.ui.viewmodel.IconProvider
import com.android.systemui.qs.panels.ui.viewmodel.TileViewModel
import com.android.systemui.qs.panels.ui.viewmodel.toIconProvider
import com.android.systemui.qs.panels.ui.viewmodel.toolbar.ToolbarViewModel
import com.android.systemui.qs.shared.ui.QuickSettings.Elements.toElementKey
import com.android.systemui.qs.tileimpl.QSTileImpl
import com.android.systemui.qs.tiles.dialog.AudioDetailsViewModel
import com.android.systemui.qs.ui.composable.QuickSettingsShade.systemGestureExclusionInShade
import com.android.systemui.qs.ui.viewmodel.QuickSettingsContainerViewModel
import com.android.systemui.res.R
import com.android.systemui.shade.ui.composable.OverlayShade
import com.android.systemui.volume.panel.component.volume.slider.ui.viewmodel.AudioStreamSliderViewModel
import com.android.systemui.volume.panel.component.volume.ui.composable.VolumeSlider
import android.content.ComponentName
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

object OneUIElements {
    val HeaderClock = ElementKey("OneUIHeaderClock")
    val BrightnessSlider = ElementKey("OneUIBrightnessSlider")
    val MediaPlayer = ElementKey("OneUIMediaPlayer")
}

@Composable
fun rememberIsOneUiStyle(): State<Boolean> {
    val context = LocalContext.current
    return produceState(
        initialValue = try {
            Settings.System.getInt(context.contentResolver, "qs_oneui_style", 0) == 1
        } catch (_: Throwable) {
            false
        },
        context
    ) {
        try {
            val resolver = context.contentResolver
            val uri = Settings.System.getUriFor("qs_oneui_style")
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    value = try {
                        Settings.System.getInt(resolver, "qs_oneui_style", 0) == 1
                    } catch (_: Throwable) {
                        false
                    }
                }
            }
            resolver.registerContentObserver(uri, false, observer)
            awaitDispose {
                try {
                    resolver.unregisterContentObserver(observer)
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
    }
}

@Composable
fun rememberPillTileSpecs(): State<Set<String>> {
    val context = LocalContext.current
    return produceState(
        initialValue = getPillTileSpecs(context),
        context
    ) {
        try {
            val resolver = context.contentResolver
            val uri = Settings.System.getUriFor("qs_oneui_pill_tiles")
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    value = getPillTileSpecs(context)
                }
            }
            resolver.registerContentObserver(uri, false, observer)
            awaitDispose {
                try {
                    resolver.unregisterContentObserver(observer)
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
    }
}

fun getPillTileSpecs(context: Context): Set<String> {
    val raw = try {
        Settings.System.getString(context.contentResolver, "qs_oneui_pill_tiles")
    } catch (_: Throwable) {
        null
    }
    return if (raw != null) {
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    } else {
        // Default pills: internet/wifi and bluetooth
        setOf("internet", "wifi", "bt", "bluetooth")
    }
}

fun setTilePillMode(context: Context, spec: String, isPill: Boolean) {
    val current = getPillTileSpecs(context).toMutableSet()
    if (isPill) {
        current.add(spec)
    } else {
        current.remove(spec)
        if (spec == "internet") current.remove("wifi")
        if (spec == "wifi") current.remove("internet")
        if (spec == "bt") current.remove("bluetooth")
        if (spec == "bluetooth") current.remove("bt")
    }
    Settings.System.putString(context.contentResolver, "qs_oneui_pill_tiles", current.joinToString(","))
}

@Composable
fun rememberCarrierName(): String {
    val context = LocalContext.current
    return produceState(initialValue = "", context) {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val sm = context.getSystemService(SubscriptionManager::class.java)

        fun queryCarrier(): String {
            try {
                val subList = sm?.activeSubscriptionInfoList
                if (!subList.isNullOrEmpty()) {
                    val names = subList.mapNotNull { it.carrierName?.toString() }
                        .filter { it.isNotBlank() }
                    if (names.isNotEmpty()) {
                        return names.joinToString(" • ")
                    }
                }
                val opName = tm?.networkOperatorName
                if (!opName.isNullOrBlank()) {
                    return opName
                }
            } catch (_: Exception) {}
            return ""
        }

        value = queryCarrier()

        val filter = IntentFilter().apply {
            addAction("android.intent.action.NETWORK_OPERATOR_CHANGED")
            addAction("android.telephony.action.SERVICE_PROVIDERS_UPDATED")
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                value = queryCarrier()
            }
        }
        try {
            context.registerReceiver(receiver, filter)
        } catch (_: Exception) {}

        awaitDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }.value
}

@Composable
fun ContentScope.OneUIQuickSettingsLayout(
    qsContainerViewModel: QuickSettingsContainerViewModel,
    modifier: Modifier = Modifier,
    toolbarViewModelFactory: ToolbarViewModel.Factory? = null,
    buildNumberViewModelFactory: BuildNumberViewModel.Factory? = null,
    isTransparencyEnabled: Boolean = false,
    volumeSliderViewModel: AudioStreamSliderViewModel? = null,
    audioDetailsViewModelFactory: AudioDetailsViewModel.Factory? = null,
    footerActionsViewModel: FooterActionsViewModel? = null,
    media: (@Composable () -> Unit)? = null,
    isScrollable: Boolean = false,
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    // One UI Theme Colors - Refined Frosted Glass Palette
    val cardBg = if (isDark) Color(0x3514171E) else Color(0x66FFFFFF)
    val cardBorder = if (isDark) Color(0x24FFFFFF) else Color(0x24000000)
    val pillInactiveBg = if (isDark) Color(0x26FFFFFF) else Color(0x14000000)
    val pillActiveBg = if (isDark) Color(0xFFFFFFFF) else Color(0xFF202124)
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1E2024)
    val textSecondary = if (isDark) Color(0xB3FFFFFF) else Color(0xFF6B7280)
    val activeContentColor = if (isDark) Color(0xFF1E2024) else Color(0xFFFFFFFF)
    val inactiveContentColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF202124)

    val toolbarViewModel =
        toolbarViewModelFactory?.let { factory ->
            rememberViewModel("OneUIQuickSettingsLayout") { factory.create() }
        }
    val editModeButtonViewModel =
        toolbarViewModel?.let { tvm ->
            rememberViewModel("OneUIEditMode") {
                tvm.editModeButtonViewModelFactory.create()
            }
        }

    val pillSpecs by rememberPillTileSpecs()

    val allTiles = qsContainerViewModel.tileGridViewModel.tileViewModels
    TileListener(tiles = allTiles) { true }

    // Partition tiles into Pills (outside the box) vs Grid (inside the box)
    val pillTiles = allTiles.filter { pillSpecs.contains(it.spec.spec) }
    val gridTiles = allTiles.filter { !pillSpecs.contains(it.spec.spec) }

    // Top pills: first 2 pill tiles (default: Wi-Fi & Bluetooth, or whatever user placed first)
    val topPill1 = pillTiles.getOrNull(0) ?: allTiles.firstOrNull { it.spec.spec == "internet" || it.spec.spec == "wifi" }
    val topPill2 = pillTiles.getOrNull(1) ?: allTiles.firstOrNull { it.spec.spec == "bt" || it.spec.spec == "bluetooth" }

    // Bottom pills: any remaining pills beyond the first 2
    val bottomPillTiles = pillTiles.filter { it != topPill1 && it != topPill2 }

    val scrollModifier = if (isScrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier

    Column(
        modifier = modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .then(scrollModifier)
    ) {
        Spacer(Modifier.height(8.dp))

        val expandable = remember { Expandable() }

        // 1. Clock, Date, and Top Actions (Edit, Power, Settings)
        OneUIHeaderClockRow(
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            showEditButton = true,
            modifier = Modifier.element(OneUIElements.HeaderClock),
            onEditClick = {
                try {
                    if (editModeButtonViewModel != null) {
                        editModeButtonViewModel.onButtonClick()
                    } else {
                        qsContainerViewModel.editModeViewModel.startEditing()
                    }
                } catch (_: Exception) {}
            },
            onPowerClick = {
                try {
                    android.view.WindowManagerGlobal.getWindowManagerService()?.showGlobalActions()
                } catch (_: Exception) {
                    try {
                        if (toolbarViewModel != null) {
                            toolbarViewModel.powerButtonViewModel.onClick(expandable)
                        } else if (footerActionsViewModel?.power != null) {
                            footerActionsViewModel.power?.onClick?.invoke(expandable)
                        }
                    } catch (_: Exception) {}
                }
            },
            onSettingsClick = {
                try {
                    val statusBarManager =
                        context.getSystemService(Context.STATUS_BAR_SERVICE) as? android.app.StatusBarManager
                    statusBarManager?.collapsePanels()
                    if (toolbarViewModel?.settingsButtonViewModel != null) {
                        toolbarViewModel.settingsButtonViewModel?.onClick(expandable)
                    } else {
                        context.startActivity(
                            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                } catch (_: Exception) {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {}
                }
            },
        )

        Spacer(Modifier.height(14.dp))

        // 3. Card 1: Top Pills (Wi-Fi & Bluetooth or user's top pill tiles outside the box)
        if (topPill1 != null || topPill2 != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (topPill1 != null) {
                    OneUIBigPillTile(
                        title = topPill1.spec.spec.replaceFirstChar { it.uppercase() },
                        defaultIconRes = R.drawable.ic_statusbar_wifi,
                        tile = topPill1,
                        fallbackTile = allTiles.getOrNull(0),
                        modifier = Modifier.weight(1f),
                        isDark = isDark,
                        pillActiveBg = pillActiveBg,
                        pillInactiveBg = pillInactiveBg,
                        activeContentColor = activeContentColor,
                        inactiveContentColor = inactiveContentColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                    )
                }
                if (topPill2 != null) {
                    OneUIBigPillTile(
                        title = topPill2.spec.spec.replaceFirstChar { it.uppercase() },
                        defaultIconRes = R.drawable.vd_bluetooth,
                        tile = topPill2,
                        fallbackTile = allTiles.getOrNull(1),
                        modifier = Modifier.weight(1f),
                        isDark = isDark,
                        pillActiveBg = pillActiveBg,
                        pillInactiveBg = pillInactiveBg,
                        activeContentColor = activeContentColor,
                        inactiveContentColor = inactiveContentColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                    )
                } else if (topPill1 != null) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // 4. Card 2: Quick Settings Tile Box (4x2 Circular Buttons Grid in Glass Container)
        GridAnchor()
        OneUITileGridCard(
            tiles = gridTiles,
            cardBg = cardBg,
            cardBorder = cardBorder,
            isDark = isDark,
            activeBg = pillActiveBg,
            inactiveBg = pillInactiveBg,
            activeContentColor = activeContentColor,
            inactiveContentColor = inactiveContentColor,
        )

        Spacer(Modifier.height(12.dp))

        // 5. Card 3: Brightness Slider + Dark Mode Toggle
        if (qsContainerViewModel.isBrightnessSliderVisible) {
            Box(
                Modifier.systemGestureExclusionInShade(
                    enabled = {
                        runCatching {
                            layoutState.transitionState is TransitionState.Idle
                        }.getOrDefault(true)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OneUIBrightnessSlider(
                        viewModel = qsContainerViewModel.brightnessSliderViewModel,
                        isDark = isDark,
                        modifier = Modifier.weight(1f).element(OneUIElements.BrightnessSlider),
                    )

                    // Circular Dark Mode Toggle Button
                    OneUICircleActionButton(
                        iconRes = R.drawable.vd_theme,
                        contentDescription = "Dark mode toggle",
                        isDark = isDark,
                        pillBg = pillInactiveBg,
                        iconColor = inactiveContentColor,
                        onClick = {
                            try {
                                val uiModeManager =
                                    context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
                                uiModeManager?.setNightModeActivated(!isDark)
                            } catch (_: Exception) {}
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 6. Card 4: Volume Slider + Sound Mode Toggle (Dering, Getar, Senyap)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OneUIVolumeSlider(
                isDark = isDark,
                modifier = Modifier.weight(1f),
            )

            OneUISoundModeButton(
                isDark = isDark,
                pillBg = pillInactiveBg,
                iconColor = inactiveContentColor,
            )
        }

        Spacer(Modifier.height(12.dp))

        // 7. Card 5: Media Player Card Compact (One UI)
        OneUICompactMediaPlayer(
            modifier = Modifier.fillMaxWidth().element(OneUIElements.MediaPlayer)
        )
        Spacer(Modifier.height(12.dp))

        // 8. Card 6: Bottom Pills (Any pills placed outside the box below media)
        if (bottomPillTiles.isNotEmpty()) {
            val chunks = bottomPillTiles.chunked(2)
            for (chunk in chunks) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (pillTile in chunk) {
                        OneUIBigPillTile(
                            title = pillTile.spec.spec.replaceFirstChar { it.uppercase() },
                            defaultIconRes = R.drawable.ic_settings_24dp,
                            tile = pillTile,
                            fallbackTile = null,
                            modifier = Modifier.weight(1f),
                            isDark = isDark,
                            pillActiveBg = pillActiveBg,
                            pillInactiveBg = pillInactiveBg,
                            activeContentColor = activeContentColor,
                            inactiveContentColor = inactiveContentColor,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                        )
                    }
                    if (chunk.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}


/** Clock and date row on the left, action buttons (edit, power, settings) on the right */
@Composable
private fun OneUIHeaderClockRow(
    textPrimary: Color,
    textSecondary: Color,
    showEditButton: Boolean = true,
    onEditClick: () -> Unit = {},
    onPowerClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentTimeMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(1000)
            value = System.currentTimeMillis()
        }
    }

    val timeFormatted = remember(currentTimeMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(currentTimeMillis))
    }
    val dateFormatted = remember(currentTimeMillis) {
        SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(currentTimeMillis))
    }

    val carrierText = rememberCarrierName()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            if (carrierText.isNotBlank()) {
                Text(
                    text = carrierText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = timeFormatted,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = dateFormatted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = textSecondary,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (showEditButton) {
                // Edit Button (pencil)
                OneUIHeaderActionButton(
                    iconRes = R.drawable.ic_edit,
                    contentDescription = "Edit Quick Settings",
                    tint = textPrimary,
                    onClick = onEditClick,
                )
            }

            // Power Button
            OneUIHeaderActionButton(
                iconRes = R.drawable.ic_settings_power,
                contentDescription = "Power Menu",
                tint = textPrimary,
                onClick = onPowerClick,
            )

            // Settings Button (gear)
            OneUIHeaderActionButton(
                iconRes = R.drawable.ic_settings_24dp,
                contentDescription = "Settings",
                tint = textPrimary,
                onClick = onSettingsClick,
            )
        }
    }
}

@Composable
private fun OneUIHeaderActionButton(
    iconRes: Int,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = tint,
        )
    }
}

/** Big Connectivity Pill Tile (Wi-Fi or Bluetooth) */
@Composable
private fun ContentScope.OneUIBigPillTile(
    title: String,
    defaultIconRes: Int,
    tile: TileViewModel?,
    fallbackTile: TileViewModel?,
    modifier: Modifier = Modifier,
    isDark: Boolean,
    pillActiveBg: Color,
    pillInactiveBg: Color,
    activeContentColor: Color,
    inactiveContentColor: Color,
    textPrimary: Color,
    textSecondary: Color,
) {
    val activeTile = tile ?: fallbackTile

    val tileState by produceState(
        initialValue = activeTile?.currentState,
        activeTile
    ) {
        activeTile?.state?.collect { value = it }
    }

    val isActive = tileState?.state == STATE_ACTIVE
    val label = tileState?.label?.toString() ?: title
    val subLabel = tileState?.secondaryLabel?.toString() ?: if (isActive) "On" else "Off"

    val currentBg by animateColorAsState(
        if (isActive) (if (isDark) Color(0x55FFFFFF) else Color(0x30000000)) else pillInactiveBg,
        label = "bigPillBg"
    )

    val content = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(currentBg)
                .combinedClickable(
                    onClick = {
                        try {
                            activeTile?.mainClick(null)
                        } catch (_: Exception) {}
                    },
                    onLongClick = {
                        try {
                            activeTile?.settingsClick(null)
                        } catch (_: Exception) {}
                    }
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isActive) pillActiveBg else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                val iconProvider = tileState?.toIconProvider()
                if (iconProvider != null) {
                    SmallTileContent(
                        iconProvider = { getTileIcon(iconProvider) },
                        color = if (isActive) activeContentColor else textPrimary,
                        size = { 20.dp },
                    )
                } else {
                    Icon(
                        painter = painterResource(defaultIconRes),
                        contentDescription = label,
                        modifier = Modifier.size(20.dp),
                        tint = if (isActive) activeContentColor else textPrimary,
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subLabel,
                    fontSize = 12.sp,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    if (activeTile != null) {
        Element(activeTile.spec.toElementKey(), modifier = modifier) {
            content()
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}

/** 4x2 Circular Tile Grid Card with smooth frosted glass background */
@Composable
private fun ContentScope.OneUITileGridCard(
    tiles: List<TileViewModel>,
    cardBg: Color,
    cardBorder: Color,
    isDark: Boolean,
    activeBg: Color,
    inactiveBg: Color,
    activeContentColor: Color,
    inactiveContentColor: Color,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val hasMoreTiles = tiles.size > 8

    val columns = 4
    val visibleTiles = if (isExpanded || !hasMoreTiles) tiles else tiles.take(8)
    val totalRows = (visibleTiles.size + columns - 1) / columns

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(26.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (rowIndex in 0 until totalRows) {
                if (rowIndex > 0) {
                    Spacer(Modifier.height(12.dp))
                }
                val rowTiles = visibleTiles.drop(rowIndex * columns).take(columns)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    for (i in 0 until columns) {
                        val tile = rowTiles.getOrNull(i)
                        OneUICircularTileItem(
                            tile = tile,
                            isDark = isDark,
                            activeBg = activeBg,
                            inactiveBg = inactiveBg,
                            activeContentColor = activeContentColor,
                            inactiveContentColor = inactiveContentColor,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // One UI Signature Page / Expand Indicator Bar Strip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = hasMoreTiles) { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(if (isExpanded) 42.dp else 32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isDark) {
                                if (hasMoreTiles) Color(0x80FFFFFF) else Color(0x40FFFFFF)
                            } else {
                                if (hasMoreTiles) Color(0x60000000) else Color(0x33000000)
                            }
                        )
                )
            }
        }
    }
}

/** Single circular tile in the 4x2 grid */
@Composable
private fun ContentScope.OneUICircularTileItem(
    tile: TileViewModel?,
    isDark: Boolean,
    activeBg: Color,
    inactiveBg: Color,
    activeContentColor: Color,
    inactiveContentColor: Color,
) {
    if (tile == null) {
        // Placeholder empty slot to preserve grid alignment
        Spacer(Modifier.size(52.dp))
        return
    }

    val tileState by produceState(initialValue = tile.currentState, tile) {
        tile.state.collect { value = it }
    }

    val state = tileState
    val isActive = state?.state == STATE_ACTIVE
    val currentBg by animateColorAsState(
        if (isActive) activeBg else inactiveBg,
        label = "circleTileBg"
    )

    Element(tile.spec.toElementKey(), modifier = Modifier) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(currentBg)
                .combinedClickable(
                    onClick = {
                        try {
                            tile.mainClick(null)
                        } catch (_: Exception) {}
                    },
                    onLongClick = {
                        try {
                            tile.settingsClick(null)
                        } catch (_: Exception) {}
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            val iconProvider = state?.toIconProvider()
            if (iconProvider != null) {
                SmallTileContent(
                    iconProvider = { getTileIcon(iconProvider) },
                    color = if (isActive) activeContentColor else inactiveContentColor,
                    size = { 22.dp },
                )
            }
        }
    }
}

/** Action circular button (e.g. Dark Mode toggle, Volume ringer toggle) */
@Composable
private fun OneUICircleActionButton(
    iconRes: Int,
    contentDescription: String,
    isDark: Boolean,
    pillBg: Color,
    iconColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(pillBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = iconColor,
        )
    }
}

/** Custom One UI Pill Brightness Slider */
@Composable
fun OneUIBrightnessSlider(
    viewModel: BrightnessSliderViewModel,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val minVal = viewModel.minBrightness.value
    val maxVal = viewModel.maxBrightness.value
    val currentGamma = viewModel.currentBrightness.value

    var isDragging by remember { mutableStateOf(false) }
    val systemProgress = remember(currentGamma, minVal, maxVal) {
        if (maxVal > minVal) {
            ((currentGamma - minVal).toFloat() / (maxVal - minVal)).coerceIn(0f, 1f)
        } else {
            0.5f
        }
    }
    var sliderProgress by remember { mutableFloatStateOf(systemProgress) }

    LaunchedEffect(systemProgress) {
        if (!isDragging) {
            sliderProgress = systemProgress
        }
    }

    val progress = sliderProgress

    val trackBg = if (isDark) Color(0x26FFFFFF) else Color(0x14000000)
    val fillBg = if (isDark) Color(0xFFFFFFFF) else Color(0xFF202124)

    BoxWithConstraints(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(trackBg)
            .pointerInput(minVal, maxVal) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        sliderProgress = frac
                        val gamma = (minVal + frac * (maxVal - minVal)).roundToInt()
                        coroutineScope.launch {
                            viewModel.onDrag(Drag.Dragging(GammaBrightness(gamma)))
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        val gamma = (minVal + sliderProgress * (maxVal - minVal)).roundToInt()
                        coroutineScope.launch {
                            viewModel.onDrag(Drag.Stopped(GammaBrightness(gamma)))
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val newProgress = (sliderProgress + dragAmount / size.width.toFloat()).coerceIn(0f, 1f)
                        sliderProgress = newProgress
                        val gamma = (minVal + newProgress * (maxVal - minVal)).roundToInt()
                        coroutineScope.launch {
                            viewModel.onDrag(Drag.Dragging(GammaBrightness(gamma)))
                        }
                    }
                )
            }
            .pointerInput(minVal, maxVal) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    sliderProgress = frac
                    val gamma = (minVal + frac * (maxVal - minVal)).roundToInt()
                    coroutineScope.launch {
                        viewModel.onDrag(Drag.Dragging(GammaBrightness(gamma)))
                        viewModel.onDrag(Drag.Stopped(GammaBrightness(gamma)))
                    }
                }
            }
    ) {
        val width = maxWidth
        val fillWidth = width * progress

        // Filled progress track
        Box(
            modifier = Modifier
                .width(fillWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(25.dp))
                .background(fillBg)
        )

        // Sun icon on the left
        val iconTint = if (progress > 0.14f) {
            if (isDark) Color(0xFF202124) else Color(0xFFFFFFFF)
        } else {
            if (isDark) Color(0xFFFFFFFF) else Color(0xFF202124)
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_brightness_medium),
                contentDescription = "Brightness",
                modifier = Modifier.size(20.dp),
                tint = iconTint,
            )
        }
    }
}

/** Custom One UI Pill Volume Slider */
@Composable
fun OneUIVolumeSlider(
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    val minVol = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        } else {
            0
        }
    }

    var currentVol by remember {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: android.content.Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                }
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    val progress = if (isDragging) {
        dragProgress
    } else {
        if (maxVol > minVol) {
            ((currentVol - minVol).toFloat() / (maxVol - minVol)).coerceIn(0f, 1f)
        } else {
            0.5f
        }
    }

    val trackBg = if (isDark) Color(0x26FFFFFF) else Color(0x14000000)
    val fillBg = if (isDark) Color(0xFFFFFFFF) else Color(0xFF202124)

    BoxWithConstraints(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(trackBg)
            .pointerInput(minVol, maxVol) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragProgress = frac
                        val vol = (minVol + frac * (maxVol - minVol)).roundToInt()
                        currentVol = vol
                        try {
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
                        } catch (_: Exception) {}
                    },
                    onDragEnd = {
                        isDragging = false
                        val vol = (minVol + dragProgress * (maxVol - minVol)).roundToInt()
                        currentVol = vol
                        try {
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
                        } catch (_: Exception) {}
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val newProgress = (dragProgress + dragAmount / size.width.toFloat()).coerceIn(0f, 1f)
                        dragProgress = newProgress
                        val vol = (minVol + newProgress * (maxVol - minVol)).roundToInt()
                        currentVol = vol
                        try {
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
                        } catch (_: Exception) {}
                    }
                )
            }
            .pointerInput(minVol, maxVol) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    dragProgress = frac
                    val vol = (minVol + frac * (maxVol - minVol)).roundToInt()
                    currentVol = vol
                    try {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
                    } catch (_: Exception) {}
                }
            }
    ) {
        val width = maxWidth
        val fillWidth = width * progress

        // Filled progress track
        Box(
            modifier = Modifier
                .width(fillWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(25.dp))
                .background(fillBg)
        )

        // Volume icon on the left
        val iconTint = if (progress > 0.14f) {
            if (isDark) Color(0xFF202124) else Color(0xFFFFFFFF)
        } else {
            if (isDark) Color(0xFFFFFFFF) else Color(0xFF202124)
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                painter = painterResource(
                    if (currentVol == 0) R.drawable.ic_volume_media_mute else R.drawable.ic_volume_media
                ),
                contentDescription = "Volume",
                modifier = Modifier.size(20.dp),
                tint = iconTint,
            )
        }
    }
}

/** One UI Sound Mode Button (Dering, Getar, Senyap) */
@Composable
fun OneUISoundModeButton(
    isDark: Boolean,
    pillBg: Color,
    iconColor: Color,
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var ringerMode by remember { mutableIntStateOf(audioManager.ringerMode) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: android.content.Intent?) {
                if (intent?.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
                    ringerMode = audioManager.ringerMode
                }
            }
        }
        val filter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    val (iconRes, description) = when (ringerMode) {
        AudioManager.RINGER_MODE_NORMAL -> Pair(R.drawable.ic_volume_ringer, "Sound")
        AudioManager.RINGER_MODE_VIBRATE -> Pair(R.drawable.ic_volume_ringer_vibrate, "Vibrate")
        else -> Pair(R.drawable.ic_volume_ringer_mute, "Mute")
    }

    OneUICircleActionButton(
        iconRes = iconRes,
        contentDescription = description,
        isDark = isDark,
        pillBg = pillBg,
        iconColor = iconColor,
        onClick = {
            try {
                val nextMode = when (ringerMode) {
                    AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                    AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                    else -> AudioManager.RINGER_MODE_NORMAL
                }
                audioManager.ringerMode = nextMode
                ringerMode = nextMode
            } catch (_: Exception) {}
        }
    )
}

/** One UI Compact Quick Quick Settings (Tarik 1 kali) */
@Composable
fun ContentScope.OneUIQuickQuickSettingsLayout(
    qsContainerViewModel: QuickSettingsContainerViewModel,
    media: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val pillInactiveBg = if (isDark) Color(0x26FFFFFF) else Color(0x14000000)
    val pillActiveBg = if (isDark) Color(0xFFFFFFFF) else Color(0xFF202124)
    val activeContentColor = if (isDark) Color(0xFF1E2024) else Color(0xFFFFFFFF)
    val inactiveContentColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF202124)
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1E2024)
    val textSecondary = if (isDark) Color(0xB3FFFFFF) else Color(0xFF6B7280)

    val allTiles = qsContainerViewModel.tileGridViewModel.tileViewModels
    TileListener(tiles = allTiles) { true }

    val wifiTile = allTiles.firstOrNull { it.spec.spec == "internet" || it.spec.spec == "wifi" }
    val btTile = allTiles.firstOrNull { it.spec.spec == "bt" || it.spec.spec == "bluetooth" }
    val roundTiles = allTiles.filter { it != wifiTile && it != btTile }.take(6)

    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        GridAnchor()

        // 0. Jam & Tanggal dan Quick Actions (tanpa edit button di QQS)
        OneUIHeaderClockRow(
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            showEditButton = false,
            modifier = Modifier.element(OneUIElements.HeaderClock),
            onPowerClick = {
                try {
                    val intent = Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN")
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                } catch (_: Exception) {}
            },
            onSettingsClick = {
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                } catch (_: Exception) {}
            },
        )
        Spacer(Modifier.height(10.dp))

        // 1. Paling atas: 1x6 icon bulat (bukan pill)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 0 until 6) {
                val tile = roundTiles.getOrNull(i)
                OneUIQQSCircularTileItem(
                    tile = tile,
                    isDark = isDark,
                    activeBg = pillActiveBg,
                    inactiveBg = pillInactiveBg,
                    activeContentColor = activeContentColor,
                    inactiveContentColor = inactiveContentColor,
                )
            }
        }

        // 2. Di bawahnya: slider brightness custom OneUI (matching QS)
        if (qsContainerViewModel.isBrightnessSliderVisible) {
            Spacer(Modifier.height(10.dp))
            OneUIBrightnessSlider(
                viewModel = qsContainerViewModel.brightnessSliderViewModel,
                isDark = isDark,
                modifier = Modifier.fillMaxWidth().element(OneUIElements.BrightnessSlider),
            )
        }

        // 3. Di bawahnya: 2x1 pill besar (Wi-Fi & Bluetooth)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OneUIBigPillTile(
                title = "Wi-Fi",
                defaultIconRes = R.drawable.ic_statusbar_wifi,
                tile = wifiTile,
                fallbackTile = allTiles.getOrNull(0),
                modifier = Modifier.weight(1f),
                isDark = isDark,
                pillActiveBg = pillActiveBg,
                pillInactiveBg = pillInactiveBg,
                activeContentColor = activeContentColor,
                inactiveContentColor = inactiveContentColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
            )

            OneUIBigPillTile(
                title = "Bluetooth",
                defaultIconRes = R.drawable.vd_bluetooth,
                tile = btTile,
                fallbackTile = allTiles.getOrNull(1),
                modifier = Modifier.weight(1f),
                isDark = isDark,
                pillActiveBg = pillActiveBg,
                pillInactiveBg = pillInactiveBg,
                activeContentColor = activeContentColor,
                inactiveContentColor = inactiveContentColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
            )
        }

        // 4. Compact One UI Media player di bawah 2x1 pill Wi-Fi & Bluetooth
        Spacer(Modifier.height(10.dp))
        OneUICompactMediaPlayer(
            modifier = Modifier.fillMaxWidth().element(OneUIElements.MediaPlayer)
        )
    }
}

/** Single circular tile in QQS (1x6 row) */
@Composable
private fun ContentScope.OneUIQQSCircularTileItem(
    tile: TileViewModel?,
    isDark: Boolean,
    activeBg: Color,
    inactiveBg: Color,
    activeContentColor: Color,
    inactiveContentColor: Color,
    modifier: Modifier = Modifier,
) {
    if (tile == null) {
        Spacer(modifier.size(48.dp))
        return
    }

    val tileState by produceState(initialValue = tile.currentState, tile) {
        tile.state.collect { value = it }
    }

    val state = tileState
    val isActive = state?.state == STATE_ACTIVE
    val currentBg by animateColorAsState(
        if (isActive) activeBg else inactiveBg,
        label = "qqsCircleTileBg"
    )

    Element(tile.spec.toElementKey(), modifier = modifier) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(currentBg)
                .combinedClickable(
                    onClick = {
                        try {
                            tile.mainClick(null)
                        } catch (_: Exception) {}
                    },
                    onLongClick = {
                        try {
                            tile.settingsClick(null)
                        } catch (_: Exception) {}
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            val iconProvider = state?.toIconProvider()
            if (iconProvider != null) {
                SmallTileContent(
                    iconProvider = { getTileIcon(iconProvider) },
                    color = if (isActive) activeContentColor else inactiveContentColor,
                    size = { 20.dp },
                )
            }
        }
    }
}

private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap? {
    if (drawable is android.graphics.drawable.BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 48
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 48
    return try {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (_: Exception) {
        null
    }
}

private fun formatMediaTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}

@Composable
private fun OneUIWaveSeekbar(
    progressFraction: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val currentFraction = if (isDragging) dragFraction else progressFraction.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragFraction = frac
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeek(dragFraction)
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragFraction = (dragFraction + dragAmount / size.width.toFloat()).coerceIn(0f, 1f)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    dragFraction = frac
                    onSeek(frac)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val thumbX = width * currentFraction
        val waveColor = Color(0xFF00C7FF)
        val unplayedColor = Color(0x45FFFFFF)

        // 1. Draw unplayed straight track line on right
        if (thumbX < width) {
            drawLine(
                color = unplayedColor,
                start = Offset(thumbX.coerceAtLeast(0f), centerY),
                end = Offset(width, centerY),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // 2. Draw smooth organic liquid wave on left (played portion)
        if (thumbX > 0f) {
            val baselineY = centerY + 2.dp.toPx()
            val wavePath = Path()
            wavePath.moveTo(0f, baselineY)

            if (thumbX < 30.dp.toPx()) {
                wavePath.quadraticBezierTo(thumbX * 0.5f, centerY - 4.dp.toPx(), thumbX, centerY)
            } else {
                val hillPeakY = centerY - 6.5.dp.toPx()
                val valleyY = centerY + 1.5.dp.toPx()

                wavePath.cubicTo(
                    thumbX * 0.15f, hillPeakY,
                    thumbX * 0.38f, hillPeakY,
                    thumbX * 0.55f, centerY
                )
                wavePath.cubicTo(
                    thumbX * 0.70f, valleyY,
                    thumbX * 0.85f, centerY - 2.5.dp.toPx(),
                    thumbX, centerY
                )
            }

            // Fill beneath the wave for liquid look
            val fillPath = Path().apply {
                addPath(wavePath)
                lineTo(thumbX, baselineY)
                lineTo(0f, baselineY)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF00C7FF), Color(0xFF008FD6)),
                    startY = centerY - 7.dp.toPx(),
                    endY = baselineY,
                )
            )

            // Stroke the wave line
            drawPath(
                path = wavePath,
                color = waveColor,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // 3. Thumb circle at thumbX
        drawCircle(
            color = waveColor,
            radius = 7.dp.toPx(),
            center = Offset(thumbX.coerceIn(0f, width), centerY),
        )
        drawCircle(
            color = Color.White,
            radius = 2.8.dp.toPx(),
            center = Offset(thumbX.coerceIn(0f, width), centerY),
        )
    }
}

/**
 * Compact One UI Media Player Card (~138dp)
 * Matches Samsung One UI with album art background, wave seekbar, and quick media controls.
 */
@Composable
fun ContentScope.OneUICompactMediaPlayer(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mediaSessionManager = remember {
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    }

    val activeSessions = produceState<List<MediaController>>(initialValue = emptyList(), mediaSessionManager) {
        if (mediaSessionManager == null) return@produceState
        val listenerComponent = try {
            ComponentName(context, "com.android.systemui.statusbar.NotificationListenerWithPlugins")
        } catch (_: Throwable) {
            null
        }

        fun getSessions(): List<MediaController> {
            return try {
                mediaSessionManager.getActiveSessions(listenerComponent)
            } catch (_: Exception) {
                try {
                    mediaSessionManager.getActiveSessions(null)
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }

        value = getSessions()

        val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            value = controllers ?: emptyList()
        }
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(listener, listenerComponent)
        } catch (_: Exception) {
            try {
                mediaSessionManager.addOnActiveSessionsChangedListener(listener, null)
            } catch (_: Exception) {}
        }

        awaitDispose {
            try {
                mediaSessionManager.removeOnActiveSessionsChangedListener(listener)
            } catch (_: Exception) {}
        }
    }.value

    val activeController = activeSessions.firstOrNull() ?: return
    var metadata by remember(activeController) { mutableStateOf(activeController.metadata) }
    var playbackState by remember(activeController) { mutableStateOf(activeController.playbackState) }

    DisposableEffect(activeController) {
        val callback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                playbackState = state
            }
            override fun onMetadataChanged(meta: MediaMetadata?) {
                metadata = meta
            }
        }
        activeController.registerCallback(callback)
        metadata = activeController.metadata
        playbackState = activeController.playbackState
        onDispose {
            try {
                activeController.unregisterCallback(callback)
            } catch (_: Exception) {}
        }
    }

    val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        ?: "Media"
    val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
        ?: ""

    val durationMs = (metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L).coerceAtLeast(0L)

    var currentPositionMs by remember(playbackState) {
        mutableLongStateOf(playbackState?.position ?: 0L)
    }

    val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING

    LaunchedEffect(playbackState?.state, playbackState?.position, isPlaying) {
        if (isPlaying) {
            val basePos = playbackState?.position ?: 0L
            val lastUpdate = playbackState?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime()
            val speed = playbackState?.playbackSpeed ?: 1f
            while (true) {
                val elapsed = ((SystemClock.elapsedRealtime() - lastUpdate) * speed).toLong()
                currentPositionMs = (basePos + elapsed).coerceIn(0L, if (durationMs > 0) durationMs else Long.MAX_VALUE)
                delay(500)
            }
        } else {
            currentPositionMs = playbackState?.position ?: 0L
        }
    }

    val artworkBitmap = remember(metadata) {
        metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
    }

    val appIcon = remember(activeController.packageName) {
        try {
            val d = context.packageManager.getApplicationIcon(activeController.packageName)
            drawableToBitmap(d)
        } catch (_: Exception) {
            null
        }
    }

    val progressFraction = if (durationMs > 0L) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(138.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0x3514171E))
            .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(32.dp))
    ) {
        // 1. Album art background or smoky glass gradient
        if (artworkBitmap != null) {
            Image(
                bitmap = artworkBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Dark glass scrim overlay so text & controls pop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x8005070A), Color(0xBB05070A))
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0x551E222D), Color(0x4012151D))
                        )
                    )
            )
        }

        // 2. Card Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // TOP ROW: "This phone" chip | App icon + Title • Artist (CENTERED) | Output Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Device chip on the far left
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x35FFFFFF))
                        .clickable {
                            try {
                                val intent = Intent("com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG").apply {
                                    putExtra("package_name", activeController.packageName)
                                }
                                context.sendBroadcast(intent)
                            } catch (_: Exception) {}
                        }
                        .padding(horizontal = 9.dp, vertical = 3.5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "This phone",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }

                // App icon + Title • Artist CENTERED in the middle
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp)),
                        )
                        Spacer(Modifier.width(6.dp))
                    }

                    val displaySubtitle = if (artist.isNotBlank()) "$title • $artist" else title
                    Text(
                        text = displaySubtitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Output switcher circular button on the far right
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0x30FFFFFF))
                        .clickable {
                            try {
                                val intent = Intent("com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG").apply {
                                    putExtra("package_name", activeController.packageName)
                                }
                                context.sendBroadcast(intent)
                            } catch (_: Exception) {}
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_speaker_on),
                        contentDescription = "Output switcher",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White,
                    )
                }
            }

            // MIDDLE SECTION: Wave Seekbar + Timestamps Row
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                OneUIWaveSeekbar(
                    progressFraction = progressFraction,
                    onSeek = { frac ->
                        if (durationMs > 0L) {
                            val targetMs = (frac * durationMs).toLong()
                            activeController.transportControls.seekTo(targetMs)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(2.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatMediaTime(currentPositionMs),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xB3FFFFFF),
                    )

                    Text(
                        text = if (durationMs > 0L) formatMediaTime(durationMs) else "--:--",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xB3FFFFFF),
                    )
                }
            }

            // BOTTOM ROW: Shuffle | Trio (Prev, Play/Pause, Next) | Check
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Shuffle button on far left
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable {
                            try {
                                activeController.transportControls.sendCustomAction(
                                    "android.media.action.SHUFFLE",
                                    null
                                )
                            } catch (_: Exception) {}
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_oneui_media_shuffle),
                        contentDescription = "Shuffle",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xDDFFFFFF),
                    )
                }

                // Center group: Trio of circular buttons (Prev, Play/Pause, Next) - large and close together!
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Previous (Circular frosted button)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x38FFFFFF))
                            .border(1.dp, Color(0x28FFFFFF), CircleShape)
                            .clickable {
                                try {
                                    activeController.transportControls.skipToPrevious()
                                } catch (_: Exception) {}
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_media_prev),
                            contentDescription = "Previous",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White,
                        )
                    }

                    // Play / Pause (Large circular frosted button)
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0x48FFFFFF))
                            .border(1.dp, Color(0x35FFFFFF), CircleShape)
                            .clickable {
                                try {
                                    if (isPlaying) {
                                        activeController.transportControls.pause()
                                    } else {
                                        activeController.transportControls.play()
                                    }
                                } catch (_: Exception) {}
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isPlaying) R.drawable.ic_oneui_media_pause else R.drawable.ic_oneui_media_play
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(22.dp),
                            tint = Color.White,
                        )
                    }

                    // Next (Circular frosted button)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x38FFFFFF))
                            .border(1.dp, Color(0x28FFFFFF), CircleShape)
                            .clickable {
                                try {
                                    activeController.transportControls.skipToNext()
                                } catch (_: Exception) {}
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_media_next),
                            contentDescription = "Next",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White,
                        )
                    }
                }

                // Check / Favorite button on far right
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x38FFFFFF))
                        .border(1.dp, Color(0x28FFFFFF), CircleShape)
                        .clickable {
                            // Favorite toggle
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_oneui_media_check),
                        contentDescription = "Favorite",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}


/** One UI Edit Mode for Quick Settings Tiles */
@Composable
fun OneUIEditMode(
    viewModel: EditModeViewModel,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val tiles by viewModel.tiles.collectAsStateWithLifecycle(emptyList())
    val pillSpecs by rememberPillTileSpecs()

    BackHandler { viewModel.stopEditing() }
    DisposableEffect(Unit) { onDispose { viewModel.stopEditing() } }

    val activeTiles = tiles.filter { it.isCurrent }
    val availableTiles = tiles.filter { !it.isCurrent }

    val bgModifier = if (isDark) {
        Modifier.background(Color(0xE6000000))
    } else {
        Modifier.background(Color(0xE6F2F3F7))
    }

    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1E2024)
    val textSecondary = if (isDark) Color(0xB3FFFFFF) else Color(0xFF6B7280)
    val cardBg = if (isDark) Color(0x3514171E) else Color(0x66FFFFFF)
    val cardBorder = if (isDark) Color(0x24FFFFFF) else Color(0x24000000)

    var isDraggingAny by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val onDone = {
        try {
            viewModel.stopEditing()
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(bgModifier),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Sticky Top Header Bar (Tidak ikut ter-scroll)
            Spacer(Modifier.height(44.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Edit buttons",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                    )
                    Text(
                        text = "Drag freely to move. Tap - or + to remove or add.",
                        fontSize = 12.sp,
                        color = textSecondary,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Reset Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDark) Color(0x30FFFFFF) else Color(0x20000000))
                            .clickable {
                                try {
                                    viewModel.stopEditing()
                                    viewModel.startEditing()
                                } catch (_: Exception) {}
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Reset",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary,
                        )
                    }

                    // Done Button Top
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDark) Color(0xFFFFFFFF) else Color(0xFF202124))
                            .clickable(onClick = onDone)
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Done",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF202124) else Color(0xFFFFFFFF),
                        )
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState, enabled = !isDraggingAny),
            ) {
                Spacer(Modifier.height(10.dp))

                // Active Buttons Card
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Active buttons (${activeTiles.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                    )
                    Text(
                        text = "Hold & drag anywhere",
                        fontSize = 11.sp,
                        color = textSecondary,
                    )
                }
                Spacer(Modifier.height(8.dp))

                OneUIEditTilesCard(
                    tiles = activeTiles,
                    isCurrent = true,
                    isDark = isDark,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    pillSpecs = pillSpecs,
                    onDraggingStateChange = { isDraggingAny = it },
                    onAction = { tile ->
                        if (tile.isRemovable) {
                            viewModel.removeTile(tile.tileSpec)
                        }
                    },
                    onTogglePill = { tile ->
                        val isPill = pillSpecs.contains(tile.tileSpec.spec)
                        setTilePillMode(context, tile.tileSpec.spec, !isPill)
                    },
                    onReorderComplete = { newTiles ->
                        viewModel.setTiles(newTiles.map { it.tileSpec })
                    },
                )

                Spacer(Modifier.height(24.dp))

                // Available Buttons Card
                Text(
                    text = "Available buttons (${availableTiles.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Spacer(Modifier.height(8.dp))

                OneUIEditTilesCard(
                    tiles = availableTiles,
                    isCurrent = false,
                    isDark = isDark,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    pillSpecs = pillSpecs,
                    onDraggingStateChange = {},
                    onAction = { tile ->
                        viewModel.addTile(tile.tileSpec)
                    },
                    onTogglePill = {},
                    onReorderComplete = { _ -> },
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun OneUIEditTilesCard(
    tiles: List<EditTileViewModel>,
    isCurrent: Boolean,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    pillSpecs: Set<String>,
    onDraggingStateChange: (Boolean) -> Unit,
    onAction: (EditTileViewModel) -> Unit,
    onTogglePill: (EditTileViewModel) -> Unit,
    onReorderComplete: (List<EditTileViewModel>) -> Unit,
) {
    var currentTilesList by remember(tiles) { mutableStateOf(tiles) }
    var draggingTileSpec by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tiles) {
        if (draggingTileSpec == null) {
            currentTilesList = tiles
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(24.dp))
            .padding(14.dp),
    ) {
        if (currentTilesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isCurrent) "No active buttons" else "No available buttons",
                    fontSize = 13.sp,
                    color = textPrimary.copy(alpha = 0.6f),
                )
            }
        } else {
            val columns = 4
            val rows = (currentTilesList.size + columns - 1) / columns
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                for (rowIndex in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        for (colIndex in 0 until columns) {
                            val index = rowIndex * columns + colIndex
                            val tile = currentTilesList.getOrNull(index)
                            if (tile != null) {
                                key(tile.tileSpec.spec) {
                                    OneUIEditTileItem(
                                        tile = tile,
                                        currentIndex = index,
                                        totalTilesCount = currentTilesList.size,
                                        isCurrent = isCurrent,
                                        isDark = isDark,
                                        isPill = pillSpecs.contains(tile.tileSpec.spec),
                                        textPrimary = textPrimary,
                                        onDragStart = {
                                            draggingTileSpec = tile.tileSpec.spec
                                            onDraggingStateChange(true)
                                        },
                                        onDragMove = { colDelta, rowDelta ->
                                            val fromIdx = currentTilesList.indexOfFirst { it.tileSpec.spec == tile.tileSpec.spec }
                                            if (fromIdx != -1) {
                                                val toIdx = (fromIdx + rowDelta * columns + colDelta).coerceIn(0, currentTilesList.size - 1)
                                                if (toIdx != fromIdx) {
                                                    val updated = currentTilesList.toMutableList()
                                                    val movedItem = updated.removeAt(fromIdx)
                                                    updated.add(toIdx, movedItem)
                                                    currentTilesList = updated
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggingTileSpec = null
                                            onDraggingStateChange(false)
                                            onReorderComplete(currentTilesList)
                                        },
                                        onAction = { onAction(tile) },
                                        onTogglePill = { onTogglePill(tile) },
                                    )
                                }
                            } else {
                                Spacer(Modifier.width(74.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OneUIEditTileItem(
    tile: EditTileViewModel,
    currentIndex: Int,
    totalTilesCount: Int,
    isCurrent: Boolean,
    isDark: Boolean,
    isPill: Boolean,
    textPrimary: Color,
    onDragStart: () -> Unit,
    onDragMove: (Int, Int) -> Unit,
    onDragEnd: () -> Unit,
    onAction: () -> Unit,
    onTogglePill: () -> Unit,
) {
    val buttonBg = if (isDark) {
        if (isCurrent) Color(0x40FFFFFF) else Color(0x20FFFFFF)
    } else {
        if (isCurrent) Color(0x25000000) else Color(0x10000000)
    }
    val badgeBg = if (isCurrent) Color(0xFFE53935) else Color(0xFF1E88E5)
    val badgeText = if (isCurrent) "-" else "+"

    var isTileDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var accumulatedDelta by remember { mutableStateOf(Offset.Zero) }
    var cellWidthPx by remember { mutableFloatStateOf(0f) }
    var cellHeightPx by remember { mutableFloatStateOf(0f) }

    val dragModifier = if (isCurrent) {
        Modifier.pointerInput(tile.tileSpec.spec) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    isTileDragging = true
                    dragOffset = Offset.Zero
                    accumulatedDelta = Offset.Zero
                    onDragStart()
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragOffset += dragAmount
                    accumulatedDelta += dragAmount

                    val stepX = if (cellWidthPx > 0f) cellWidthPx else 190f
                    val stepY = if (cellHeightPx > 0f) cellHeightPx else 220f

                    val colDelta = (accumulatedDelta.x / stepX).roundToInt()
                    val rowDelta = (accumulatedDelta.y / stepY).roundToInt()

                    if (colDelta != 0 || rowDelta != 0) {
                        onDragMove(colDelta, rowDelta)
                        accumulatedDelta = Offset(
                            accumulatedDelta.x - colDelta * stepX,
                            accumulatedDelta.y - rowDelta * stepY,
                        )
                    }
                },
                onDragEnd = {
                    isTileDragging = false
                    dragOffset = Offset.Zero
                    accumulatedDelta = Offset.Zero
                    onDragEnd()
                },
                onDragCancel = {
                    isTileDragging = false
                    dragOffset = Offset.Zero
                    accumulatedDelta = Offset.Zero
                    onDragEnd()
                }
            )
        }
        .onGloballyPositioned { coordinates ->
            cellWidthPx = coordinates.size.width.toFloat()
            cellHeightPx = coordinates.size.height.toFloat()
        }
        .zIndex(if (isTileDragging) 100f else 0f)
        .graphicsLayer {
            translationX = if (isTileDragging) dragOffset.x else 0f
            translationY = if (isTileDragging) dragOffset.y else 0f
            scaleX = if (isTileDragging) 1.15f else 1.0f
            scaleY = if (isTileDragging) 1.15f else 1.0f
            shadowElevation = if (isTileDragging) 16f else 0f
        }
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .width(74.dp)
            .then(dragModifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Main circle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(if (isPill) RoundedCornerShape(14.dp) else CircleShape)
                    .background(buttonBg)
                    .clickable(onClick = onAction),
                contentAlignment = Alignment.Center,
            ) {
                SmallTileContent(
                    iconProvider = { tile.icon },
                    color = textPrimary,
                    size = { 22.dp },
                )
            }

            // Badge at top right (+ or -)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(badgeBg)
                    .clickable(onClick = onAction),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badgeText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 1.dp),
                )
            }

            // Top-Left badge (Only for current tiles): Form factor toggle (Pill vs 1x1)
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPill) Color(0xFF1976D2) else (if (isDark) Color(0x55FFFFFF) else Color(0x35000000)))
                        .clickable(onClick = onTogglePill)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isPill) "Pill" else "1x1",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(3.dp))

        Text(
            text = tile.label.text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            color = textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

private fun Context.getTileIcon(icon: IconProvider): ModelIcon {
    return icon.icon?.let {
        try {
            val drawable = it.getDrawable(this)
            if (drawable != null) {
                ModelIcon.Loaded(drawable, null)
            } else if (it is QSTileImpl.ResourceIcon && it.resId != 0) {
                ModelIcon.Resource(it.resId, null)
            } else {
                ModelIcon.Resource(R.drawable.ic_error_outline, null)
            }
        } catch (_: Exception) {
            ModelIcon.Resource(R.drawable.ic_error_outline, null)
        }
    } ?: ModelIcon.Resource(R.drawable.ic_error_outline, null)
}
