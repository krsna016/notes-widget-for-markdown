package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.util.AppLog
import com.marfolog.noteswidgetformarkdown.data.preferences.NoteCardSettingsStore
import com.marfolog.noteswidgetformarkdown.data.repository.GitSyncStatusReader
import com.marfolog.noteswidgetformarkdown.data.repository.ObsidianVaultLocator
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardAppearance
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardColor
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.domain.usecase.GetNotesUseCase
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.ui.theme.WidgetPalette
import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import kotlinx.coroutines.flow.firstOrNull
import org.koin.java.KoinJavaComponent.get

class NotesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val startedAt = System.currentTimeMillis()
        AppLog.d("Render", "provideGlance entered at $startedAt")
        val backgroundPercent = WidgetBackground.percent(context)
        val widgetColors = WidgetPalette.colors(WidgetBackground.color(context))

        // loadWidgetData used to run before provideContent, so the first frame anyone saw was
        // already the finished one — on a slow SAF folder that is a multi-second blank widget
        // with no sign anything is happening, easy to mistake for the refresh being frozen.
        // Composing immediately and loading inside a LaunchedEffect puts a spinner on screen
        // for that window instead. Seeding from WidgetDataCache means that spinner only ever
        // covers the whole widget on the very first render this process has ever done — every
        // refresh after that keeps showing the last known content while it reloads quietly.
        provideContent {
            var data by remember { mutableStateOf(WidgetDataCache.last) }
            var isLoading by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                val fresh = loadWidgetData(context)
                WidgetDataCache.last = fresh
                data = fresh
                isLoading = false
                AppLog.d("Render", "loadWidgetData done in ${System.currentTimeMillis() - startedAt}ms")
            }
            WidgetColorTheme(widgetColors) {
                val current = data
                if (current == null) {
                    LoadingRoot(backgroundPercent)
                } else {
                    WidgetRoot(
                        current.state,
                        current.vaultName,
                        current.noteFolderPath,
                        current.cardSettings,
                        current.bottomBarChip,
                        isLoading,
                        current.extraNoteFolders,
                        backgroundPercent
                    )
                }
            }
        }
        AppLog.d("Render", "provideGlance rendered in ${System.currentTimeMillis() - startedAt}ms")
    }

    companion object {
        private const val TAG = "NotesWidget"

        suspend fun updateAll(context: Context) {
            val startedAt = System.currentTimeMillis()
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(NotesWidget::class.java)
            ids.forEach { id ->
                // See forceFreshGlanceSession: makes update() start a new session instead of
                // trusting one that may be wedged. A tried-and-failed 600ms retry used to be
                // here instead — logged proof it did not help is in issue #4.
                forceFreshGlanceSession(context, manager, id)
                NotesWidget().update(context, id)
            }
            // There used to be a second refresh here too: a plain APPWIDGET_UPDATE broadcast,
            // sent as a fallback for when update() alone could not be trusted to land. Now that
            // forceFreshGlanceSession makes update() itself reliable, that fallback only added a
            // redundant, visibly delayed second recompose — the loading spinner flashing across
            // the whole widget a moment after it had already shown the right data.
            // Both flavours show the same folder, so they always refresh together.
            NotesListWidget.updateAll(context)
            AppLog.d("Render", "updateAll requested for ${ids.size} widgets in ${System.currentTimeMillis() - startedAt}ms")
        }
    }
}

// region Root

@Composable
private fun WidgetRoot(
    state: WidgetState,
    vaultName: String?,
    noteFolderPath: String?,
    cardSettings: Map<String, NoteCardAppearance>,
    bottomBarChip: BottomBarChip,
    isLoading: Boolean = false,
    extraNoteFolders: Map<String, String> = emptyMap(),
    backgroundPercent: Int = WidgetBackground.DEFAULT_PERCENT
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(32.dp)
            .background(widgetBackground(backgroundPercent))
            .padding(10.dp)
    ) {
        when (state) {
            is WidgetState.Uninitialized -> UninitializedContent()
            is WidgetState.PermissionLost -> PermissionLostContent()
            is WidgetState.Success -> ConfiguredContent(state.notes, vaultName, noteFolderPath, cardSettings, bottomBarChip, isLoading, extraNoteFolders, backgroundPercent)
            is WidgetState.Empty -> ConfiguredContentEmpty(vaultName, noteFolderPath, bottomBarChip, isLoading, backgroundPercent)
            is WidgetState.Error -> ConfiguredContentError(state.message, vaultName, noteFolderPath, bottomBarChip, isLoading, backgroundPercent)
        }
    }
}

// endregion

// region Loading State

/**
 * First frame of every provideGlance call, shown for as long as [loadWidgetData] takes (SAF
 * folder reads have been over a second on device). Without this the widget was blank — same
 * area, same background — for that whole stretch, which reads as frozen rather than working.
 */
@Composable
private fun LoadingRoot(backgroundPercent: Int) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(32.dp)
            .background(widgetBackground(backgroundPercent))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = ChromeColors.progress)
    }
}

// endregion

// region Uninitialized State

@Composable
private fun UninitializedContent() {
    val setupIntent = SetupActivity.intent(LocalContext.current, UsageEvent.FROM_WIDGET)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(32.dp)
            .background(GlanceTheme.colors.surfaceVariant)
            .clickable(actionStartActivity(setupIntent))
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_setup),
            contentDescription = LocalContext.current.getString(R.string.action_setup),
            modifier = GlanceModifier.size(48.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
        )
        Spacer(modifier = GlanceModifier.height(16.dp))
        Text(
            text = LocalContext.current.getString(R.string.widget_setup_required),
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = GlanceTheme.colors.onSurfaceVariant
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = LocalContext.current.getString(R.string.widget_setup_hint),
            style = TextStyle(
                fontSize = 14.sp,
                color = GlanceTheme.colors.onSurfaceVariant
            )
        )
    }
}

// endregion

// region Permission Lost State

@Composable
private fun PermissionLostContent() {
    val setupIntent = SetupActivity.intent(LocalContext.current, UsageEvent.FROM_WIDGET)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(32.dp)
            .background(GlanceTheme.colors.surfaceVariant)
            .clickable(actionStartActivity(setupIntent))
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_error),
            contentDescription = LocalContext.current.getString(R.string.action_error),
            modifier = GlanceModifier.size(48.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.error)
        )
        Spacer(modifier = GlanceModifier.height(16.dp))
        Text(
            text = LocalContext.current.getString(R.string.widget_permission_lost),
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = GlanceTheme.colors.error
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = LocalContext.current.getString(R.string.widget_permission_hint),
            style = TextStyle(
                fontSize = 14.sp,
                color = GlanceTheme.colors.onSurfaceVariant
            )
        )
    }
}

// endregion

// region Configured States (Content + Refresh + FAB layers)

@Composable
private fun ConfiguredContent(
    notes: List<NoteSummary>,
    vaultName: String?,
    noteFolderPath: String?,
    cardSettings: Map<String, NoteCardAppearance>,
    bottomBarChip: BottomBarChip,
    isLoading: Boolean = false,
    extraNoteFolders: Map<String, String> = emptyMap(),
    backgroundPercent: Int = WidgetBackground.DEFAULT_PERCENT
) {
    ConfiguredScaffold(vaultName, noteFolderPath, bottomBarChip, backgroundPercent, isLoading) {
        SingleNoteView(notes, vaultName, noteFolderPath, cardSettings, extraNoteFolders)
    }
}

@Composable
private fun ConfiguredContentEmpty(
    vaultName: String?,
    noteFolderPath: String?,
    bottomBarChip: BottomBarChip,
    isLoading: Boolean = false,
    backgroundPercent: Int = WidgetBackground.DEFAULT_PERCENT
) {
    ConfiguredScaffold(vaultName, noteFolderPath, bottomBarChip, backgroundPercent, isLoading) {
        EmptyContent(backgroundPercent)
    }
}

@Composable
private fun ConfiguredContentError(
    message: String,
    vaultName: String?,
    noteFolderPath: String?,
    bottomBarChip: BottomBarChip,
    isLoading: Boolean = false,
    backgroundPercent: Int = WidgetBackground.DEFAULT_PERCENT
) {
    ConfiguredScaffold(vaultName, noteFolderPath, bottomBarChip, backgroundPercent, isLoading) {
        ErrorContent(message, backgroundPercent)
    }
}

// endregion

// region Single Note View & Notes List

@Composable
private fun SingleNoteView(
    notes: List<NoteSummary>,
    vaultName: String?,
    noteFolderPath: String?,
    cardSettings: Map<String, NoteCardAppearance>,
    extraNoteFolders: Map<String, String>
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
    val selectedUri = prefs.getString(SetupActivity.KEY_SELECTED_NOTE_URI, null)

    val note = notes.firstOrNull { it.fileUri == selectedUri } ?: notes.firstOrNull()
    if (note == null) {
        EmptyContent(WidgetBackground.DEFAULT_PERCENT)
        return
    }

    val appearance = cardSettings[note.fileUri] ?: NoteCardAppearance()
    val noteFolder = noteFolderPathFor(note, noteFolderPath, extraNoteFolders)
    val openIntent = widgetNoteOpenIntent(context, note, vaultName, noteFolder)
    val ribbonColor = ColorProvider(appearance.accentRibbonColor())

    // Slider state
    val totalNotes = notes.size
    val currentSelectedIdx = notes.indexOfFirst { it.fileUri == note.fileUri }.coerceAtLeast(0)
    var sliderOffset = prefs.getInt(SelectWidgetNoteAction.KEY_SLIDER_OFFSET, currentSelectedIdx)
    if (sliderOffset !in 0 until totalNotes) {
        sliderOffset = currentSelectedIdx
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(appearance.backgroundColor())
            .padding(12.dp)
    ) {
        // TOP SLIDER: Cards of all notes with slide arrows
        if (totalNotes > 1) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left slide button
                val prevOffset = (sliderOffset - 1 + totalNotes) % totalNotes
                val prevAction = actionRunCallback<SelectWidgetNoteAction>(
                    actionParametersOf(SelectWidgetNoteAction.PARAM_SLIDER_OFFSET to prevOffset)
                )
                Box(
                    modifier = GlanceModifier
                        .size(24.dp)
                        .cornerRadius(12.dp)
                        .background(ColorProvider(Color(0x28888888)))
                        .clickable(prevAction),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "‹",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = appearance.contentColor()
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(5.dp))

                // Up to 3 visible note cards in the slider window
                val visibleCount = minOf(3, totalNotes)
                for (i in 0 until visibleCount) {
                    val noteIdx = (sliderOffset + i) % totalNotes
                    val sliderNote = notes[noteIdx]
                    val isCurrent = (sliderNote.fileUri == note.fileUri)
                    val cardClickAction = actionRunCallback<SelectWidgetNoteAction>(
                        actionParametersOf(
                            SelectWidgetNoteAction.PARAM_NOTE_URI to sliderNote.fileUri,
                            SelectWidgetNoteAction.PARAM_SLIDER_OFFSET to sliderOffset
                        )
                    )

                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(28.dp)
                            .cornerRadius(10.dp)
                            .background(
                                if (isCurrent) ColorProvider(Color(0xFF7C3AED)) else ColorProvider(Color(0x22888888))
                            )
                            .clickable(cardClickAction)
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sliderNote.title,
                            style = TextStyle(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                color = if (isCurrent) ColorProvider(Color.White) else appearance.contentColor()
                            ),
                            maxLines = 1
                        )
                    }

                    if (i < visibleCount - 1) {
                        Spacer(modifier = GlanceModifier.width(4.dp))
                    }
                }

                Spacer(modifier = GlanceModifier.width(5.dp))

                // Right slide button
                val nextOffset = (sliderOffset + 1) % totalNotes
                val nextAction = actionRunCallback<SelectWidgetNoteAction>(
                    actionParametersOf(SelectWidgetNoteAction.PARAM_SLIDER_OFFSET to nextOffset)
                )
                Box(
                    modifier = GlanceModifier
                        .size(24.dp)
                        .cornerRadius(12.dp)
                        .background(ColorProvider(Color(0x28888888)))
                        .clickable(nextAction),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "›",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = appearance.contentColor()
                        )
                    )
                }
            }
        }

        // MAIN NOTE AREA
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight(),
            verticalAlignment = Alignment.Top
        ) {
        // Luxury Left-Edge Accent Ribbon with Physical Metallic Side Clip
        Column(
            modifier = GlanceModifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_clip),
                contentDescription = "Widget Clip",
                modifier = GlanceModifier.width(16.dp).height(44.dp)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Box(
                modifier = GlanceModifier
                    .width(4.dp)
                    .defaultWeight()
                    .cornerRadius(2.dp)
                    .background(ribbonColor)
            ) {}
        }

        Spacer(modifier = GlanceModifier.width(10.dp))

        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
        ) {
            // Title Header (Clickable to open in Obsidian)
            Text(
                text = note.title,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = appearance.textSize.titleSp.sp,
                    color = appearance.contentColor()
                ),
                maxLines = 1,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity(openIntent))
            )

            Spacer(modifier = GlanceModifier.height(6.dp))

            val lines = note.preview.lines().filter { it.isNotBlank() }
            val taskRegex = Regex("""^(\s*)([☑☐])\s+(.*)$""")
            val listRegex = Regex("""^(\s*)([•\*\-]\s+|\d+\.\s+)(.*)$""")

            LazyColumn(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
            ) {
                items(count = lines.size, itemId = { it.toLong() }) { index ->
                    val line = lines[index]
                    when {
                        line.startsWith("§H1§") -> {
                            val text = line.removePrefix("§H1§")
                            Column(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 4.dp)
                                    .clickable(actionStartActivity(openIntent))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = GlanceModifier
                                            .width(4.dp)
                                            .height(20.dp)
                                            .cornerRadius(2.dp)
                                            .background(ribbonColor)
                                    ) {}
                                    Spacer(modifier = GlanceModifier.width(8.dp))
                                    Text(
                                        text = text,
                                        style = TextStyle(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (appearance.textSize.titleSp + 2).sp,
                                            fontFamily = FontFamily.Serif,
                                            color = appearance.contentColor()
                                        ),
                                        maxLines = 2
                                    )
                                }
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                Box(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(ColorProvider(Color(0x30888888)))
                                ) {}
                            }
                        }
                        line.startsWith("§H2§") -> {
                            val text = line.removePrefix("§H2§")
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 3.dp)
                                    .clickable(actionStartActivity(openIntent)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✦ ",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = ribbonColor
                                    )
                                )
                                Text(
                                    text = text,
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = appearance.textSize.titleSp.sp,
                                        fontFamily = FontFamily.Serif,
                                        color = appearance.contentColor()
                                    ),
                                    maxLines = 2
                                )
                            }
                        }
                        line.startsWith("§H3§") -> {
                            val text = line.removePrefix("§H3§")
                            Text(
                                text = text,
                                style = TextStyle(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = (appearance.textSize.previewSp + 2).sp,
                                    fontFamily = FontFamily.Serif,
                                    color = appearance.contentColor()
                                ),
                                maxLines = 2,
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, bottom = 2.dp)
                                    .clickable(actionStartActivity(openIntent))
                            )
                        }
                        line.startsWith("§H4§") -> {
                            val text = line.removePrefix("§H4§")
                            Text(
                                text = text.uppercase(),
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (appearance.textSize.previewSp - 2).sp,
                                    color = ColorProvider(Color(0xFF9E9E9E))
                                ),
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(top = 5.dp, bottom = 2.dp)
                                    .clickable(actionStartActivity(openIntent))
                            )
                        }
                        line == "§HR§" -> {
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = GlanceModifier
                                        .defaultWeight()
                                        .height(1.dp)
                                        .background(ColorProvider(Color(0x28888888)))
                                ) {}
                                Text(
                                    text = "  ❦  ",
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        color = ColorProvider(Color(0x55888888))
                                    )
                                )
                                Box(
                                    modifier = GlanceModifier
                                        .defaultWeight()
                                        .height(1.dp)
                                        .background(ColorProvider(Color(0x28888888)))
                                ) {}
                            }
                        }
                        line.startsWith("§QUOTE§") -> {
                            val text = line.removePrefix("§QUOTE§")
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .cornerRadius(8.dp)
                                    .background(ColorProvider(Color(0x14888888)))
                                    .clickable(actionStartActivity(openIntent))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = GlanceModifier
                                        .width(3.dp)
                                        .height(20.dp)
                                        .cornerRadius(1.5.dp)
                                        .background(ColorProvider(Color(0xFF38BDF8)))
                                ) {}
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                Text(
                                    text = text,
                                    style = TextStyle(
                                        fontStyle = FontStyle.Italic,
                                        fontSize = appearance.textSize.previewSp.sp,
                                        fontFamily = FontFamily.Serif,
                                        color = appearance.contentColor()
                                    ),
                                    maxLines = 4,
                                    modifier = GlanceModifier.defaultWeight()
                                )
                            }
                        }
                        line.startsWith("§CODE§") -> {
                            val text = line.removePrefix("§CODE§")
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .cornerRadius(6.dp)
                                    .background(ColorProvider(Color(0x20000000)))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = text,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = (appearance.textSize.previewSp - 2).sp,
                                        color = appearance.contentColor()
                                    ),
                                    maxLines = 2
                                )
                            }
                        }
                        taskRegex.find(line) != null -> {
                            val taskMatch = taskRegex.find(line)!!
                            val (indent, symbol, taskText) = taskMatch.destructured
                            val isChecked = (symbol == "☑")
                            val toggleAction = actionRunCallback<ToggleTaskAction>(
                                actionParametersOf(
                                    ToggleTaskAction.PARAM_NOTE_URI to note.fileUri,
                                    ToggleTaskAction.PARAM_TASK_TEXT to taskText.trim(),
                                    ToggleTaskAction.PARAM_IS_CHECKED to isChecked
                                )
                            )

                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable(toggleAction),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (indent.isNotEmpty()) {
                                    Spacer(modifier = GlanceModifier.width((indent.length * 6).dp))
                                }
                                Text(
                                    text = if (isChecked) "☑ " else "☐ ",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = appearance.textSize.previewSp.sp,
                                        color = if (isChecked) ColorProvider(Color(0xFF10B981)) else appearance.contentColor()
                                    )
                                )
                                Spacer(modifier = GlanceModifier.width(4.dp))
                                Text(
                                    text = taskText,
                                    style = TextStyle(
                                        fontSize = appearance.textSize.previewSp.sp,
                                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (isChecked) ColorProvider(Color(0xFF9E9E9E)) else appearance.contentColor()
                                    ),
                                    maxLines = 3,
                                    modifier = GlanceModifier.defaultWeight()
                                )
                            }
                        }
                        listRegex.find(line) != null -> {
                            val listMatch = listRegex.find(line)!!
                            val (indent, marker, content) = listMatch.destructured
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable(actionStartActivity(openIntent)),
                                verticalAlignment = Alignment.Top
                            ) {
                                if (indent.isNotEmpty()) {
                                    Spacer(modifier = GlanceModifier.width((indent.length * 6).dp))
                                }
                                Text(
                                    text = marker,
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = appearance.textSize.previewSp.sp,
                                        color = ribbonColor
                                    )
                                )
                                Spacer(modifier = GlanceModifier.width(4.dp))
                                Text(
                                    text = content,
                                    style = TextStyle(
                                        fontSize = appearance.textSize.previewSp.sp,
                                        color = appearance.contentColor()
                                    ),
                                    maxLines = 3,
                                    modifier = GlanceModifier.defaultWeight()
                                )
                            }
                        }
                        else -> {
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable(actionStartActivity(openIntent))
                            ) {
                                Text(
                                    text = line,
                                    style = TextStyle(
                                        fontSize = appearance.textSize.previewSp.sp,
                                        color = appearance.contentColor()
                                    ),
                                    maxLines = 4
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun NotesList(
    notes: List<NoteSummary>,
    vaultName: String?,
    noteFolderPath: String?,
    cardSettings: Map<String, NoteCardAppearance>,
    extraNoteFolders: Map<String, String>
) {
    LazyColumn(
        modifier = GlanceModifier
            .fillMaxSize()
    ) {
        items(notes, itemId = { note ->
            val appearance = cardSettings[note.fileUri] ?: NoteCardAppearance()
            "${note.id}:${appearance.size.storageValue}:${appearance.color.storageValue}:${appearance.textSize.storageValue}:${appearance.customColorHex.orEmpty()}"
                .hashCode()
                .toLong()
        }) { note ->
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                NoteCard(
                    note,
                    vaultName,
                    noteFolderPathFor(note, noteFolderPath, extraNoteFolders),
                    cardSettings[note.fileUri] ?: NoteCardAppearance()
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: NoteSummary,
    vaultName: String?,
    noteFolderPath: String?,
    appearance: NoteCardAppearance
) {
    val openIntent = widgetNoteOpenIntent(LocalContext.current, note, vaultName, noteFolderPath)
    val ribbonColor = ColorProvider(appearance.accentRibbonColor())

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(appearance.size.heightDp.dp)
            .cornerRadius(28.dp)
            .background(appearance.backgroundColor())
            .clickable(actionStartActivity(openIntent))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Luxury Left-Edge Accent Ribbon
        Box(
            modifier = GlanceModifier
                .width(4.dp)
                .fillMaxHeight()
                .cornerRadius(2.dp)
                .background(ribbonColor)
        ) {}

        Spacer(modifier = GlanceModifier.width(10.dp))

        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
        ) {
            Text(
                text = note.title,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = appearance.textSize.titleSp.sp,
                    color = appearance.contentColor()
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = note.preview,
                style = TextStyle(
                    fontSize = appearance.textSize.previewSp.sp,
                    color = appearance.contentColor()
                ),
                maxLines = appearance.size.previewMaxLines
            )
        }
    }
}

// endregion

// region Empty & Error Content

@Composable
private fun EmptyContent(backgroundPercent: Int, modifier: GlanceModifier = GlanceModifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        WidgetSurface(backgroundPercent, cornerRadius = 12.dp, horizontalPadding = 12.dp, verticalPadding = 8.dp) {
            Text(
                text = LocalContext.current.getString(R.string.widget_no_notes),
                style = TextStyle(
                    fontSize = 14.sp,
                    color = ChromeColors.content
                )
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, backgroundPercent: Int, modifier: GlanceModifier = GlanceModifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        WidgetSurface(backgroundPercent, cornerRadius = 12.dp, horizontalPadding = 12.dp, verticalPadding = 8.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    provider = ImageProvider(R.drawable.ic_error),
                    contentDescription = LocalContext.current.getString(R.string.action_error),
                    modifier = GlanceModifier.size(32.dp),
                    colorFilter = ColorFilter.tint(ChromeColors.error)
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = message,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = ChromeColors.error
                    )
                )
            }
        }
    }
}

// endregion
