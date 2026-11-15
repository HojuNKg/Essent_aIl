package com.HojuNK.essent_all.ui.screens

import android.content.Context
import android.view.Surface as ViewSurface
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.HojuNK.essent_all.AppInfo
import com.HojuNK.essent_all.ClipboardHistoryManager
import com.HojuNK.essent_all.ClipboardItem
import com.HojuNK.essent_all.Config
import com.HojuNK.essent_all.EventsViewMode
import com.HojuNK.essent_all.ListSortOption
import com.HojuNK.essent_all.KeyMap
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.ui.components.ClipboardCard
import com.HojuNK.essent_all.ui.components.TypewriterTitle
import com.HojuNK.essent_all.ui.components.ViewAndSortDialog
import com.HojuNK.essent_all.ui.editor.NotesEditor
import kotlinx.coroutines.launch

enum class NoteFilter(val title: String) {
    ALL("Все"),
    CLIPBOARD("Буфер"),
    NOTES("Заметки"),
    FAVORITES("Избранное"),
    LINKS("Ссылки"),
    FORMATTED("С форматированием"),
    PLAIN_TEXT("Чистый текст"),
    SHORT("Короткие"),
    LONG("Длинные")
}

fun hasLink(text: String): Boolean {
    val urlRegex = Regex("""(https?://|www\.)\S+""", RegexOption.IGNORE_CASE)
    return urlRegex.containsMatchIn(text)
}

fun hasFormatting(text: String): Boolean {
    val formatRegex = Regex("""<[^>]+>|•|☐|☑|\[\s*\]|\[x\]|^\d+\.\s""", RegexOption.MULTILINE)
    return formatRegex.containsMatchIn(text)
}

fun stripHtmlAndStructures(text: String): String {
    if (text.trim().startsWith("{") && text.trim().endsWith("}")) {
        try {
            val rootObj = org.json.JSONObject(text)
            val blocksArr = rootObj.optJSONArray("blocks")
            if (blocksArr != null) {
                val sb = StringBuilder()
                for (i in 0 until blocksArr.length()) {
                    val blockObj = blocksArr.getJSONObject(i)
                    val type = blockObj.optString("type", "text")
                    if (type == "text") {
                        val jsonStr = blockObj.optString("content", "")
                        val plainText = try {
                            val obj = org.json.JSONObject(jsonStr)
                            obj.optString("text", "")
                        } catch (e: Exception) {
                            jsonStr.replace(Regex("""<[^>]+>"""), "")
                        }
                        if (plainText.isNotBlank()) {
                            if (sb.isNotEmpty()) sb.append("\n")
                            sb.append(plainText)
                        }
                    } else if (type == "image") {
                        if (sb.isNotEmpty()) sb.append("\n")
                        sb.append("[Изображение]")
                    }
                }
                return sb.toString()
            }
        } catch (e: Exception) {}
    }
    return text.replace(Regex("""<[^>]+>"""), "")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(
    keyMap: KeyMap,
    apps: List<AppInfo>?,
    onOpenSettings: () -> Unit,
    onShowAccessibilityError: () -> Unit,
    onBack: (() -> Unit)? = null,
    onEditorToggle: ((Boolean) -> Unit)? = null,
    isSelectionMode: Boolean = false,
    onSelectionModeChange: (Boolean) -> Unit = {},
    onSelectedCountChanged: (Int) -> Unit = {},
    onSelectAllActionChanged: (() -> Unit) -> Unit = {},
    isSearchActive: Boolean = false,
    onSearchActiveChange: (Boolean) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val rotation = context.display?.rotation ?: ViewSurface.ROTATION_0
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val scope = rememberCoroutineScope()

    var history by remember {
        mutableStateOf(
            try {
                ClipboardHistoryManager.getHistory(context)
            } catch (e: Exception) {
                emptyList()
            }
        )
    }

    LaunchedEffect(Unit) {
        try {
            ClipboardHistoryManager.syncSystemClipboard(context)
            history = ClipboardHistoryManager.getHistory(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        ClipboardHistoryManager.historyUpdatedFlow.collect {
            history = ClipboardHistoryManager.getHistory(context)
        }
    }

    var selectedFilters by remember { mutableStateOf(setOf<NoteFilter>()) }
    var searchQuery by remember { mutableStateOf("") }
    var showViewAndSortDialog by remember { mutableStateOf(false) }

    var selectedDetailItem by remember { mutableStateOf<ClipboardItem?>(null) }
    var isCreatingNewItem by remember { mutableStateOf(false) }

    var selectedItemIds by remember { mutableStateOf(setOf<String>()) }
    var expandedMenuId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isSelectionMode) {
        if (!isSelectionMode) {
            selectedItemIds = emptySet()
        }
    }

    LaunchedEffect(selectedItemIds.size) {
        onSelectedCountChanged(selectedItemIds.size)
    }

    val yellowColor = Color(0xFFD700)
    val grayBorder = Color(0xFF333333)

    val displayItems = remember(history, keyMap.notesSortOption, keyMap.notesSortAscending) {
        val comparator: Comparator<ClipboardItem> = when (keyMap.notesSortOption) {
            ListSortOption.CREATION_DATE -> compareBy { it.timestamp }
            ListSortOption.ALPHABETICAL -> compareBy { stripHtmlAndStructures(it.content).lowercase() }
            ListSortOption.RANKING -> compareBy { it.timestamp }
        }
        val finalComp = if (keyMap.notesSortAscending) comparator else comparator.reversed()
        history.sortedWith(finalComp)
    }

    val filteredItems = remember(displayItems, selectedFilters, searchQuery) {
        var items = displayItems

        if (selectedFilters.isNotEmpty()) {
            items = items.filter { item ->
                var matches = true
                for (filter in selectedFilters) {
                    val pass = when (filter) {
                        NoteFilter.ALL -> true
                        NoteFilter.CLIPBOARD -> !item.isNote
                        NoteFilter.NOTES -> item.isNote
                        NoteFilter.FAVORITES -> item.isPinned
                        NoteFilter.LINKS -> hasLink(item.content)
                        NoteFilter.FORMATTED -> hasFormatting(item.content)
                        NoteFilter.PLAIN_TEXT -> !hasFormatting(item.content)
                        NoteFilter.SHORT -> stripHtmlAndStructures(item.content).lines().filter { l -> l.isNotBlank() }.size <= 5
                        NoteFilter.LONG -> stripHtmlAndStructures(item.content).lines().filter { l -> l.isNotBlank() }.size > 5
                    }
                    if (!pass) {
                        matches = false
                        break
                    }
                }
                matches
            }
        }

        if (searchQuery.isNotBlank()) {
            items = items.filter { it.content.contains(searchQuery, ignoreCase = true) }
        }

        items
    }

    val handleSelectAll = {
        val isAllSelected = filteredItems.isNotEmpty() && filteredItems.all { selectedItemIds.contains(it.id) }
        selectedItemIds = if (isAllSelected) emptySet() else filteredItems.map { it.id }.toSet()
    }

    LaunchedEffect(filteredItems, selectedItemIds) {
        onSelectAllActionChanged(handleSelectAll)
    }

    val isEditorActive = isCreatingNewItem || selectedDetailItem != null

    LaunchedEffect(isEditorActive) {
        onEditorToggle?.invoke(isEditorActive)
    }

    DisposableEffect(Unit) {
        onDispose {
            onEditorToggle?.invoke(false)
        }
    }

    if (isCreatingNewItem || selectedDetailItem != null) {
        NotesEditor(
            noteItem = selectedDetailItem,
            isCreatingNew = isCreatingNewItem,
            onBack = {
                selectedDetailItem = null
                isCreatingNewItem = false
            },
            onNoteSaved = {
                history = ClipboardHistoryManager.getHistory(context)
            },
            onNoteSavedWithId = { newId ->
                history = ClipboardHistoryManager.getHistory(context)
                val createdItem = history.find { it.id == newId }
                selectedDetailItem = createdItem
                isCreatingNewItem = false
            }
        )
        return
    }

    BackHandler(enabled = true) {
        if (isSelectionMode) {
            onSelectionModeChange(false)
            selectedItemIds = emptySet()
        } else if (isSearchActive) {
            onSearchActiveChange(false)
            searchQuery = ""
        } else if (onBack != null) {
            onBack()
        }
    }

    val useDark = com.HojuNK.essent_all.LocalUseDark.current
    val topBarBg = if (useDark) Color.Black else Color.White
    val textColor = if (useDark) Color.White else Color.Black
    val mutedColor = if (useDark) Color.Gray else Color.DarkGray
    val cardBg = if (useDark) Color.Black else Color.White
    val innerCardBg = if (useDark) Color(0xFF1A1A1A) else Color(0xFFF0F0F0)
    val borderColor = if (useDark) Color.Gray.copy(alpha = 0.3f) else Color.LightGray

    val filtersRowContent: @Composable () -> Unit = {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(NoteFilter.entries.toTypedArray(), key = { it.name }) { filter ->
                val isAll = filter == NoteFilter.ALL
                val isSelected = if (isAll) selectedFilters.isEmpty() else selectedFilters.contains(filter)
                val showCross = isAll && selectedFilters.isNotEmpty()

                Surface(
                    onClick = {
                        if (isAll) {
                            selectedFilters = emptySet()
                        } else {
                            selectedFilters = if (selectedFilters.contains(filter)) {
                                selectedFilters - filter
                            } else {
                                selectedFilters + filter
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) textColor else innerCardBg,
                    border = if (!isSelected) BorderStroke(1.dp, borderColor) else null,
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (showCross) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Reset filters",
                                tint = mutedColor,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = filter.title,
                                color = if (isSelected) cardBg else textColor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = topBarBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                TopAppBar(
                    title = {
                        if (isSelectionMode) {
                            Text(stringResource(R.string.selected_count, selectedItemIds.size), color = textColor, fontWeight = FontWeight.Bold)
                        } else if (isSearchActive) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                placeholder = { Text(stringResource(R.string.clipboard_search_hint), color = mutedColor, fontSize = 14.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = yellowColor,
                                    unfocusedBorderColor = grayBorder,
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    cursorColor = yellowColor
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                        } else {
                            TypewriterTitle(targetText = "Заметки", animate = false)
                        }
                    },
                    modifier = Modifier
                        .drawBehind {
                            val blurHeight = 45.dp.toPx()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(topBarBg, topBarBg.copy(alpha = 0.5f)),
                                    startY = 0f,
                                    endY = size.height
                                ),
                                size = size
                            )
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(topBarBg.copy(alpha = 0.5f), Color.Transparent),
                                    startY = size.height,
                                    endY = size.height + blurHeight
                                ),
                                topLeft = Offset(0f, size.height),
                                size = Size(size.width, blurHeight)
                            )
                        },
                    navigationIcon = {
                        if (isSelectionMode) {
                            IconButton(onClick = { onSelectionModeChange(false); selectedItemIds = emptySet() }) {
                                Icon(Icons.Default.Close, null, tint = textColor)
                            }
                        } else if (isSearchActive) {
                            IconButton(onClick = { onSearchActiveChange(false); searchQuery = "" }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                            }
                        } else if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                            }
                        }
                    },
                    actions = {
                        if (isSelectionMode) {
                            val isAllSelected = filteredItems.isNotEmpty() && filteredItems.all { selectedItemIds.contains(it.id) }
                            IconButton(onClick = {
                                selectedItemIds = if (isAllSelected) emptySet() else filteredItems.map { it.id }.toSet()
                            }) {
                                Icon(if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll, null, tint = textColor)
                            }
                        } else {
                            if (!isSearchActive) {
                                IconButton(onClick = { onSelectionModeChange(true) }) {
                                    Icon(Icons.Default.Checklist, null, tint = textColor)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .combinedClickable(
                                            onClick = {
                                                val nextMode = if (keyMap.notesViewMode == EventsViewMode.STRIPES) EventsViewMode.RECTANGLES else EventsViewMode.STRIPES
                                                scope.launch { Config.setNotesViewMode(context, nextMode) }
                                            },
                                            onLongClick = {
                                                showViewAndSortDialog = true
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (keyMap.notesViewMode == EventsViewMode.STRIPES) Icons.Default.ViewStream else Icons.Default.GridView,
                                        contentDescription = "View Mode",
                                        tint = textColor
                                    )
                                }
                                IconButton(onClick = { onSearchActiveChange(true) }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = textColor)
                                }
                                IconButton(onClick = onOpenSettings) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = textColor)
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        actionIconContentColor = textColor,
                        navigationIconContentColor = textColor,
                        titleContentColor = textColor
                    )
                )
            }
        },
        floatingActionButtonPosition = if (isLandscape && rotation == ViewSurface.ROTATION_270) FabPosition.Start else FabPosition.End,
        floatingActionButton = {
            if (!isSelectionMode && !isSearchActive) {
                FloatingActionButton(
                    onClick = {
                        isCreatingNewItem = true
                    },
                    containerColor = if (useDark) Color.Gray else Color.DarkGray,
                    contentColor = Color.White,
                    modifier = Modifier.padding(
                        bottom = if (isLandscape) 24.dp else (if (onBack != null) 24.dp else 120.dp),
                        start = if (isLandscape && rotation == ViewSurface.ROTATION_270) 135.dp else 0.dp,
                        end = if (isLandscape && rotation == ViewSurface.ROTATION_90) 135.dp else 0.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Note"
                    )
                }
            }
        }
    ) { paddingValues ->
        val topPadding = if (configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            paddingValues.calculateTopPadding()
        } else {
            0.dp
        }

        val startPadding = if (isLandscape) {
            if (rotation == ViewSurface.ROTATION_270) 130.dp else 80.dp
        } else 0.dp

        val endPadding = if (isLandscape) {
            if (rotation == ViewSurface.ROTATION_90) 130.dp else 80.dp
        } else 0.dp

        val bottomPadding = paddingValues.calculateBottomPadding()

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isSearchActive && isLandscape) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp + startPadding, vertical = 8.dp)
                            .height(50.dp),
                        placeholder = { Text(stringResource(R.string.clipboard_search_hint), color = Color.Gray, fontSize = 14.sp) },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { onSearchActiveChange(false); searchQuery = "" }) {
                                Icon(Icons.Default.Close, null, tint = Color.White)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = yellowColor,
                            unfocusedBorderColor = grayBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = yellowColor
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                if (keyMap.notesViewMode == EventsViewMode.STRIPES) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            top = topPadding + 8.dp,
                            bottom = bottomPadding + 16.dp + (if (isLandscape) 0.dp else (if (onBack != null) 24.dp else 120.dp)),
                            start = 16.dp + startPadding,
                            end = 16.dp + endPadding
                        )
                    ) {
                        item(key = "filters_row") {
                            filtersRowContent()
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        if (filteredItems.isEmpty()) {
                            item(key = "empty_state") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.clipboard_empty),
                                        color = Color.Gray,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        } else {
                            items(filteredItems, key = { it.id }) { item ->
                                val isSelected = selectedItemIds.contains(item.id)
                                ClipboardCard(
                                    item = item,
                                    isMenuExpanded = expandedMenuId == item.id,
                                    isNoteStyle = true,
                                    maxLines = 5,
                                    onToggleMenu = {
                                        if (isSelectionMode) {
                                            selectedItemIds = if (isSelected) selectedItemIds - item.id else selectedItemIds + item.id
                                        } else {
                                            expandedMenuId = if (expandedMenuId == item.id) null else item.id
                                        }
                                    },
                                    onTap = {
                                        if (isSelectionMode) {
                                            selectedItemIds = if (isSelected) selectedItemIds - item.id else selectedItemIds + item.id
                                        } else {
                                            selectedDetailItem = item
                                        }
                                    },
                                    onCopy = {
                                        ClipboardHistoryManager.copyToClipboard(context, item)
                                        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                        expandedMenuId = null
                                    },
                                    onPaste = {
                                        ClipboardHistoryManager.copyToClipboard(context, item)
                                        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                        expandedMenuId = null
                                    },
                                    onDelete = {
                                        ClipboardHistoryManager.removeItem(context, item.id)
                                        history = ClipboardHistoryManager.getHistory(context)
                                        expandedMenuId = null
                                    },
                                    onPin = {
                                        ClipboardHistoryManager.togglePin(context, item.id)
                                        history = ClipboardHistoryManager.getHistory(context)
                                        expandedMenuId = null
                                    }
                                )
                            }
                        }
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp,
                        contentPadding = PaddingValues(
                            top = topPadding + 8.dp,
                            bottom = bottomPadding + 16.dp + (if (isLandscape) 0.dp else (if (onBack != null) 24.dp else 120.dp)),
                            start = 16.dp + startPadding,
                            end = 16.dp + endPadding
                        )
                    ) {
                        item(span = StaggeredGridItemSpan.FullLine, key = "filters_row_grid") {
                            filtersRowContent()
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        if (filteredItems.isEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine, key = "empty_state_grid") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.clipboard_empty),
                                        color = Color.Gray,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        } else {
                            items(filteredItems, key = { it.id }) { item ->
                                val isSelected = selectedItemIds.contains(item.id)
                                ClipboardCard(
                                    item = item,
                                    isMenuExpanded = expandedMenuId == item.id,
                                    isNoteStyle = true,
                                    maxLines = 7,
                                    onToggleMenu = {
                                        if (isSelectionMode) {
                                            selectedItemIds = if (isSelected) selectedItemIds - item.id else selectedItemIds + item.id
                                        } else {
                                            expandedMenuId = if (expandedMenuId == item.id) null else item.id
                                        }
                                    },
                                    onTap = {
                                        if (isSelectionMode) {
                                            selectedItemIds = if (isSelected) selectedItemIds - item.id else selectedItemIds + item.id
                                        } else {
                                            selectedDetailItem = item
                                        }
                                    },
                                    onCopy = {
                                        ClipboardHistoryManager.copyToClipboard(context, item)
                                        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                        expandedMenuId = null
                                    },
                                    onPaste = {
                                        ClipboardHistoryManager.copyToClipboard(context, item)
                                        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                        expandedMenuId = null
                                    },
                                    onDelete = {
                                        ClipboardHistoryManager.removeItem(context, item.id)
                                        history = ClipboardHistoryManager.getHistory(context)
                                        expandedMenuId = null
                                    },
                                    onPin = {
                                        ClipboardHistoryManager.togglePin(context, item.id)
                                        history = ClipboardHistoryManager.getHistory(context)
                                        expandedMenuId = null
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Нижняя панель массового выбора (Batch operations)
            if (isSelectionMode) {
                NotesSelectionBottomPanel(
                    selectedCount = selectedItemIds.size,
                    onPinToggle = {
                        selectedItemIds.forEach { id ->
                            ClipboardHistoryManager.togglePin(context, id)
                        }
                        history = ClipboardHistoryManager.getHistory(context)
                        onSelectionModeChange(false)
                        selectedItemIds = emptySet()
                    },
                    onDelete = {
                        selectedItemIds.forEach { id ->
                            ClipboardHistoryManager.removeItem(context, id)
                        }
                        history = ClipboardHistoryManager.getHistory(context)
                        onSelectionModeChange(false)
                        selectedItemIds = emptySet()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = bottomPadding + 24.dp)
                )
            }
        }
    }

    if (showViewAndSortDialog) {
        ViewAndSortDialog(
            viewMode = keyMap.notesViewMode,
            onViewModeChange = { mode -> scope.launch { Config.setNotesViewMode(context, mode) } },
            sortOption = keyMap.notesSortOption,
            onSortOptionChange = { option -> scope.launch { Config.setNotesSortOption(context, option) } },
            sortAscending = keyMap.notesSortAscending,
            onSortAscendingToggle = { scope.launch { Config.setNotesSortAscending(context, !keyMap.notesSortAscending) } },
            showRankingOption = false,
            onDismiss = { showViewAndSortDialog = false }
        )
    }
}

@Composable
fun NotesSelectionBottomPanel(
    selectedCount: Int,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val useDark = com.HojuNK.essent_all.LocalUseDark.current
    val innerCardBg = if (useDark) Color(0xFF1A1A1A) else Color(0xFFF0F0F0)
    val borderColor = if (useDark) Color.Gray.copy(alpha = 0.3f) else Color.LightGray

    Surface(
        color = innerCardBg,
        shape = RoundedCornerShape(50.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .width(260.dp)
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SelectionActionButton(
                icon = Icons.Filled.Star,
                label = "Избранное",
                enabled = selectedCount > 0,
                onClick = onPinToggle
            )
            SelectionActionButton(
                icon = Icons.Default.Delete,
                label = "Удалить",
                enabled = selectedCount > 0,
                onClick = onDelete
            )
        }
    }
}
