package com.HojuNK.essent_all.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.*
import com.HojuNK.essent_all.model.*
import com.HojuNK.essent_all.util.*
import com.HojuNK.essent_all.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    keyMap: KeyMap,
    apps: List<AppInfo>?,
    onOpenSettings: () -> Unit,
    onShowAccessibilityError: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

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
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Недавние, 1: Все, 2: Заметки, 3: Избранное
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var selectedDetailItem by remember { mutableStateOf<ClipboardItem?>(null) }
    var isEditingDetail by remember { mutableStateOf(false) }
    var isCreatingNewItem by remember { mutableStateOf(false) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedItemIds by remember { mutableStateOf(setOf<String>()) }
    var expandedMenuId by remember { mutableStateOf<String?>(null) }

    val yellowColor = Color(0xFFD700)
    val darkBg = Color(0xFF000000)
    val grayBorder = Color(0xFF333333)

    var editingTextFieldValue by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    BackHandler(enabled = isSelectionMode || isSearchActive || selectedDetailItem != null || isCreatingNewItem) {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedItemIds = emptySet()
        } else if (isCreatingNewItem) {
            isCreatingNewItem = false
        } else if (isEditingDetail) {
            isEditingDetail = false
        } else if (selectedDetailItem != null) {
            selectedDetailItem = null
        } else if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        }
    }

    val displayItems = remember(history) {
        history.sortedWith(
            compareByDescending<ClipboardItem> { it.isPinned }
                .thenByDescending { it.timestamp }
        )
    }

    val tabFiltered = remember(displayItems, selectedTab) {
        when (selectedTab) {
            0 -> displayItems.filter { it.isPinned || (System.currentTimeMillis() - it.timestamp) < 24 * 3600 * 1000L }
            1 -> displayItems
            2 -> displayItems.filter { it.isNote }
            3 -> displayItems.filter { it.isPinned }
            else -> displayItems
        }
    }

    val filteredItems = remember(tabFiltered, searchQuery) {
        if (searchQuery.isBlank()) tabFiltered
        else tabFiltered.filter { it.content.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg),
        containerColor = darkBg,
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text(stringResource(R.string.selected_count, selectedItemIds.size), color = Color.White, fontWeight = FontWeight.Bold)
                    } else if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            placeholder = { Text(stringResource(R.string.clipboard_search_hint), color = Color.Gray, fontSize = 14.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = yellowColor,
                                unfocusedBorderColor = grayBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = yellowColor
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                    } else {
                        TypewriterTitle(targetText = stringResource(R.string.nav_more), animate = false)
                    }
                },
                modifier = Modifier
                    .drawBehind {
                        val blurHeight = 45.dp.toPx()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black, Color.Black.copy(alpha = 0.5f)),
                                startY = 0f,
                                endY = size.height
                            ),
                            size = size
                        )
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent),
                                startY = size.height,
                                endY = size.height + blurHeight
                            ),
                            topLeft = Offset(0f, size.height),
                            size = Size(size.width, blurHeight)
                        )
                    },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { isSelectionMode = false; selectedItemIds = emptySet() }) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    } else {
                        IconButton(onClick = {
                            if (isCreatingNewItem) isCreatingNewItem = false
                            else if (isEditingDetail) isEditingDetail = false
                            else if (selectedDetailItem != null) selectedDetailItem = null
                            else if (isSearchActive) { isSearchActive = false; searchQuery = "" }
                            else onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        val isAllSelected = filteredItems.isNotEmpty() && filteredItems.all { selectedItemIds.contains(it.id) }
                        IconButton(onClick = {
                            selectedItemIds = if (isAllSelected) emptySet() else filteredItems.map { it.id }.toSet()
                        }) {
                            Icon(if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll, null, tint = Color.White)
                        }
                    } else {
                        if (!isSearchActive && !isCreatingNewItem && selectedDetailItem == null) {
                            IconButton(onClick = { isSelectionMode = true }) {
                                Icon(Icons.Default.Checklist, null, tint = Color.White)
                            }
                            IconButton(onClick = {
                                val nextMode = if (keyMap.notesViewMode == EventsViewMode.STRIPES) EventsViewMode.RECTANGLES else EventsViewMode.STRIPES
                                scope.launch { Config.setNotesViewMode(context, nextMode) }
                            }) {
                                Icon(
                                    imageVector = if (keyMap.notesViewMode == EventsViewMode.STRIPES) Icons.Default.ViewStream else Icons.Default.GridView,
                                    contentDescription = "View Mode",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = yellowColor)
                            }
                            IconButton(onClick = {
                                editingTextFieldValue = TextFieldValue("")
                                isCreatingNewItem = true
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "New Note", tint = yellowColor)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (!isSelectionMode && !isSearchActive && !isCreatingNewItem && selectedDetailItem == null) {
                FloatingActionButton(
                    onClick = {
                        editingTextFieldValue = TextFieldValue("")
                        isCreatingNewItem = true
                    },
                    containerColor = yellowColor,
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Note")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isCreatingNewItem -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Новая заметка", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = {
                                    val finalContent = editingTextFieldValue.text
                                    if (finalContent.isNotBlank()) {
                                        ClipboardHistoryManager.addItemAndGetId(context, finalContent, "text", isNote = true)
                                        history = ClipboardHistoryManager.getHistory(context)
                                        isCreatingNewItem = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = yellowColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Сохранить", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = editingTextFieldValue,
                            onValueChange = { editingTextFieldValue = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .focusRequester(focusRequester),
                            placeholder = { Text("Введите текст заметки...", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = yellowColor,
                                unfocusedBorderColor = grayBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = yellowColor
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    }
                }
                selectedDetailItem != null -> {
                    val item = selectedDetailItem!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEditingDetail) "Редактирование" else "Детали заметки",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isEditingDetail) {
                                    Button(
                                        onClick = {
                                            val updated = item.copy(content = editingTextFieldValue.text)
                                            ClipboardHistoryManager.updateItem(context, updated)
                                            history = ClipboardHistoryManager.getHistory(context)
                                            selectedDetailItem = updated
                                            isEditingDetail = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = yellowColor),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Сохранить", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            editingTextFieldValue = TextFieldValue(item.content)
                                            isEditingDetail = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Править", color = Color.White)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        if (isEditingDetail) {
                            OutlinedTextField(
                                value = editingTextFieldValue,
                                onValueChange = { editingTextFieldValue = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = yellowColor,
                                    unfocusedBorderColor = grayBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = yellowColor
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1A1A1A),
                                border = BorderStroke(1.dp, grayBorder)
                            ) {
                                Box(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                                    Text(
                                        text = htmlToAnnotatedString(item.content),
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            edgePadding = 16.dp
                        ) {
                            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Недавние") })
                            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Все") })
                            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Заметки") })
                            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Избранное") })
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (filteredItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.clipboard_empty),
                                    color = Color.Gray,
                                    fontSize = 15.sp
                                )
                            }
                        } else {
                            if (keyMap.notesViewMode == EventsViewMode.STRIPES) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    items(filteredItems, key = { it.id }) { item ->
                                        val isSelected = selectedItemIds.contains(item.id)
                                        ClipboardCard(
                                            item = item,
                                            isMenuExpanded = expandedMenuId == item.id,
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
                            } else {
                                LazyVerticalStaggeredGrid(
                                    columns = StaggeredGridCells.Fixed(2),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalItemSpacing = 8.dp,
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    items(filteredItems, key = { it.id }) { item ->
                                        val isSelected = selectedItemIds.contains(item.id)
                                        ClipboardCard(
                                            item = item,
                                            isMenuExpanded = expandedMenuId == item.id,
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
                }
            }
        }
    }
}
