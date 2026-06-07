package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BibleVerse
import com.example.data.BookWithTestament
import com.example.ui.state.AppViewModel
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.CinzelBoldFamily
import com.example.ui.theme.InterFontFamily


@OptIn(ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BibleSection(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val books by viewModel.bibleBooks.collectAsStateWithLifecycle()
    val verses by viewModel.currentVerses.collectAsStateWithLifecycle()
    val kjvVerses by viewModel.kjvVerses.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarkedVerses.collectAsStateWithLifecycle()
    val highlights by viewModel.highlightedVerses.collectAsStateWithLifecycle()
    val selectedBookState by viewModel.selectedBookFlow.collectAsStateWithLifecycle()
    val selectedChapterState by viewModel.selectedChapterFlow.collectAsStateWithLifecycle()
    
    var bookmarkSortAscending by remember { mutableStateOf(true) }
    val bookOrderMap = remember {
        ALL_BIBLE_BOOKS.mapIndexed { index, b -> b.book to index }.toMap()
    }
    val sortedBookmarks = remember(bookmarks, bookmarkSortAscending) {
        if (bookmarkSortAscending) {
            bookmarks.sortedWith(compareBy<BibleVerse> { bookOrderMap[it.book] ?: 999 }
                .thenBy { it.chapter }
                .thenBy { it.verseNum })
        } else {
            bookmarks.sortedWith(compareByDescending<BibleVerse> { bookOrderMap[it.book] ?: 999 }
                .thenByDescending { it.chapter }
                .thenByDescending { it.verseNum })
        }
    }
    
    var viewMode by remember { mutableStateOf("READ") } // "READ", "BOOKMARKS", "HIGHLIGHTS", "SEARCH"
    var showBookPicker by remember { mutableStateOf(false) }
    var expandedBookInPicker by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(showBookPicker) {
        if (showBookPicker) {
            expandedBookInPicker = selectedBookState
            val currentBookData = ALL_BIBLE_BOOKS.find { it.book == selectedBookState }
            if (currentBookData != null) {
                viewModel.selectedTestamentFilter = currentBookData.testament
            }
        }
    }
    var selectedVerseForAction by remember { mutableStateOf<BibleVerse?>(null) }
    
    // Highlight colors definitions
    val highlightColors = listOf(
        Pair("Yellow", Color(0xFFFEF08A)), // light semi-trans yellow
        Pair("Rose", Color(0xFFFECDD3)),   // light rose
        Pair("Blue", Color(0xFFBFDBFE)),   // light blue
        Pair("Green", Color(0xFFBBF7D0)),  // light green
        Pair("None", Color.Transparent)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Core Section Top Bar
        AnimatedVisibility(
            visible = viewModel.isBibleHeaderVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(0.dp, 0.dp, 20.dp, 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Navigation controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showBookPicker = true }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Book,
                                contentDescription = "Bible",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$selectedBookState $selectedChapterState",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = "Select Book",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Adjustable sizing & View Mode toggles
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { viewModel.bibleFontSize = (viewModel.bibleFontSize - 1.5f).coerceAtLeast(12f) }) {
                                Icon(Icons.Outlined.TextFormat, contentDescription = "Decrease Font Size", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { viewModel.bibleFontSize = (viewModel.bibleFontSize + 1.5f).coerceAtMost(28f) }) {
                                Icon(Icons.Filled.TextFormat, contentDescription = "Increase Font Size", modifier = Modifier.size(24.dp))
                            }
                            IconButton(
                                onClick = { viewMode = if (viewMode == "SEARCH") "READ" else "SEARCH" },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (viewMode == "SEARCH") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                                )
                            ) {
                                Icon(Icons.Filled.Search, contentDescription = "Search Bible")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    var translationDropdownExpanded by remember { mutableStateOf(false) }

                    // Filters tabs for fast lookup
                    ScrollableTabRow(
                        selectedTabIndex = when(viewMode) {
                            "READ" -> 0
                            "BOOKMARKS" -> 1
                            "HIGHLIGHTS" -> 2
                            else -> 0
                        },
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = viewMode == "READ",
                            onClick = { 
                                viewMode = "READ"
                                translationDropdownExpanded = true
                            },
                            text = {
                                Box(modifier = Modifier.wrapContentSize()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(if (viewModel.selectedTranslation == "KJV") "KJV Version" else "BSB Version")
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDropDown,
                                            contentDescription = "Select Translation",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = translationDropdownExpanded,
                                        onDismissRequest = { translationDropdownExpanded = false },
                                        modifier = Modifier.background(androidx.compose.ui.graphics.Color.White)
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "BSB Version",
                                                    fontWeight = if (viewModel.selectedTranslation == "BSB") FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 12.sp,
                                                    color = if (viewModel.selectedTranslation == "BSB") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                viewModel.selectedTranslation = "BSB"
                                                translationDropdownExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "KJV Version",
                                                    fontWeight = if (viewModel.selectedTranslation == "KJV") FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 12.sp,
                                                    color = if (viewModel.selectedTranslation == "KJV") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                viewModel.selectedTranslation = "KJV"
                                                viewModel.fetchKjvVersesOnline(selectedBookState, selectedChapterState)
                                                translationDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        )
                        Tab(
                            selected = viewMode == "BOOKMARKS",
                            onClick = { viewMode = "BOOKMARKS" },
                            text = { Text("Bookmarks (${bookmarks.size})") }
                        )
                        Tab(
                            selected = viewMode == "HIGHLIGHTS",
                            onClick = { viewMode = "HIGHLIGHTS" },
                            text = { Text("Highlights (${highlights.size})") }
                        )
                    }
                }
            }
        }

        // Active screens depending on choice
        Box(modifier = Modifier.weight(1.0f)) {
            when (viewMode) {
                "READ" -> {
                    val activeVersesList = if (viewModel.selectedTranslation == "KJV") kjvVerses else verses

                    if (viewModel.selectedTranslation == "KJV" && viewModel.isFetchingKjv) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Fetching KJV translation...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else if (viewModel.selectedTranslation == "KJV" && viewModel.kjvFetchError != null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                                Text(
                                    text = viewModel.kjvFetchError ?: "Connection error",
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Button(
                                    onClick = { viewModel.fetchKjvVersesOnline(selectedBookState, selectedChapterState) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Retry Connection", color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    } else if (activeVersesList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Book, contentDescription = "Empty", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (viewModel.selectedTranslation == "KJV") "Failed to fetch KJV scriptures." else "Select a book study above to begin reading.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        val listState = rememberLazyListState(
                            initialFirstVisibleItemIndex = viewModel.lastBibleScrollIndex,
                            initialFirstVisibleItemScrollOffset = viewModel.lastBibleScrollOffset
                        )

                        LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
                            val currentIndex = listState.firstVisibleItemIndex
                            val currentOffset = listState.firstVisibleItemScrollOffset
                            viewModel.lastBibleScrollIndex = currentIndex
                            viewModel.lastBibleScrollOffset = currentOffset

                            viewModel.isBibleHeaderVisible = (currentIndex == 0 && currentOffset < 20)
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(activeVersesList) { verse ->
                                val textBgColor = if (verse.highlightColor != null) {
                                    try {
                                        Color(android.graphics.Color.parseColor(verse.highlightColor))
                                    } catch (e: Exception) {
                                        Color.Transparent
                                    }
                                } else {
                                    Color.Transparent
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { selectedVerseForAction = verse },
                                            onLongClick = {
                                                clipboardManager.setText(AnnotatedString("${verse.book} ${verse.chapter}:${verse.verseNum} - ${verse.text} (${viewModel.selectedTranslation})"))
                                                Toast.makeText(context, "Copied Verse to Clipboard", Toast.LENGTH_SHORT).show()
                                            }
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (textBgColor != Color.Transparent) textBgColor.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = "${verse.verseNum}",
                                            fontSize = (viewModel.bibleFontSize - 2f).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.width(30.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = verse.text,
                                                fontSize = viewModel.bibleFontSize.sp,
                                                lineHeight = (viewModel.bibleFontSize * 1.45f).sp,
                                                fontFamily = LoraFontFamily,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                        if (verse.isBookmarked) {
                                            Icon(
                                                Icons.Filled.Bookmark,
                                                contentDescription = "Bookmarked",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(260.dp))
                            }
                        }
                    }
                }
                "BOOKMARKS" -> {
                    if (bookmarks.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Outlined.BookmarkBorder,
                            title = "No Bookmarks Yet",
                            description = "Tap on any verse to bookmark your favorite passages."
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Professional Sort Toolbar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (bookmarkSortAscending) Icons.Filled.SortByAlpha else Icons.Filled.Sort,
                                        contentDescription = "Canonical order icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (bookmarkSortAscending) "Canonical Book Order (A-Z equivalent)" else "Canonical Book Order (Z-A equivalent)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                TextButton(
                                    onClick = { bookmarkSortAscending = !bookmarkSortAscending },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                                ) {
                                    Text(
                                        text = if (bookmarkSortAscending) "Ascending (Gen-Rev)" else "Descending (Rev-Gen)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (bookmarkSortAscending) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                        contentDescription = "Toggle sort direction",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                            )
                            
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(sortedBookmarks) { verse ->
                                    BookmarkHighlightRow(
                                        verse = verse,
                                        onGoTo = {
                                            viewModel.selectedBook = verse.book
                                            viewModel.selectedChapter = verse.chapter
                                            viewMode = "READ"
                                        },
                                        onRemove = { viewModel.toggleBookmark(verse) }
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(260.dp))
                                }
                            }
                        }
                    }
                }
                "HIGHLIGHTS" -> {
                    if (highlights.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Outlined.BorderColor,
                            title = "No Highlights Yet",
                            description = "Color code meaningful verses to support your sermon note tracking."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(highlights) { verse ->
                                val colorLabelColor = if (verse.highlightColor != null) {
                                    try {
                                        Color(android.graphics.Color.parseColor(verse.highlightColor))
                                    } catch (e: Exception) {
                                        Color.Transparent
                                    }
                                } else {
                                    Color.Transparent
                                }
                                BookmarkHighlightRow(
                                    verse = verse,
                                    colorLabelColor = colorLabelColor,
                                    onGoTo = {
                                        viewModel.selectedBook = verse.book
                                        viewModel.selectedChapter = verse.chapter
                                        viewMode = "READ"
                                    },
                                    onRemove = { viewModel.applyHighlight(verse, null) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(260.dp))
                            }
                        }
                    }
                }
                "SEARCH" -> {
                    BibleSearchView(viewModel = viewModel)
                }
            }
        }
    }

    // Book & Chapter Picker Modal Dialog (Beautiful Polished White Card Design)
    if (showBookPicker) {
        val dialogBg = Color.White
        val lightBlueBorder = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF60A5FA).copy(alpha = 0.45f))

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showBookPicker = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(540.dp)
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = dialogBg),
                border = lightBlueBorder,
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Bible Book & Chapter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = CinzelBoldFamily,
                            color = Color(0xFF1E3A8A) // Match Royal Blue brand color
                        )
                        IconButton(
                            onClick = { showBookPicker = false },
                            modifier = Modifier
                                .background(Color(0xFFF1F5F9), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close Picker",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    val availableBooks = ALL_BIBLE_BOOKS
                    
                    Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        // Testament Segmented Control (Pill-shaped, gorgeous iOS-style toggle)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeFilter = viewModel.selectedTestamentFilter
                            
                            // Old Testament Segment
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (activeFilter == "OT") Color.White else Color.Transparent
                                    )
                                    .clickable { viewModel.selectedTestamentFilter = "OT" }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Old Testament",
                                    fontSize = 13.sp,
                                    fontWeight = if (activeFilter == "OT") FontWeight.Bold else FontWeight.Medium,
                                    color = if (activeFilter == "OT") Color(0xFF1E3A8A) else Color(0xFF64748B),
                                    fontFamily = InterFontFamily
                                )
                            }
                            
                            // New Testament Segment
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (activeFilter == "NT") Color.White else Color.Transparent
                                    )
                                    .clickable { viewModel.selectedTestamentFilter = "NT" }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "New Testament",
                                    fontSize = 13.sp,
                                    fontWeight = if (activeFilter == "NT") FontWeight.Bold else FontWeight.Medium,
                                    color = if (activeFilter == "NT") Color(0xFF1E3A8A) else Color(0xFF64748B),
                                    fontFamily = InterFontFamily
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val filteredBooks = availableBooks.filter { it.testament == viewModel.selectedTestamentFilter }
                        val listState = rememberLazyListState()

                        LaunchedEffect(showBookPicker, viewModel.selectedTestamentFilter, expandedBookInPicker) {
                            if (showBookPicker) {
                                val targetBook = expandedBookInPicker ?: selectedBookState
                                val index = filteredBooks.indexOfFirst { it.book == targetBook }
                                if (index >= 0) {
                                    // Scroll selected book to the top of viewport, centering the expanded chapter panel beneath it
                                    listState.animateScrollToItem(index)
                                }
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredBooks) { bookData ->
                                val isExpanded = expandedBookInPicker == bookData.book
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isExpanded) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (isExpanded) Color(0xFF60A5FA).copy(alpha = 0.60f) else Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandedBookInPicker = if (isExpanded) null else bookData.book
                                            }
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.Book,
                                                    contentDescription = null,
                                                    tint = if (isExpanded) Color(0xFF1E3A8A) else Color(0xFF3B82F6),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = bookData.book,
                                                    fontSize = 15.sp,
                                                    fontWeight = if (isExpanded) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = Color.Black,
                                                    fontFamily = InterFontFamily
                                                )
                                            }
                                            
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                                tint = if (isExpanded) Color(0xFF3B82F6) else Color(0xFF64748B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // If selected book, reveal chapters selector inline
                                        if (isExpanded) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            HorizontalDivider(color = Color(0xFF60A5FA).copy(alpha = 0.35f))
                                            Spacer(modifier = Modifier.height(10.dp))
                                            
                                            val chaptersCount = when (bookData.book) {
                                                "Genesis" -> 50
                                                "Exodus" -> 40
                                                "Leviticus" -> 27
                                                "Numbers" -> 36
                                                "Deuteronomy" -> 34
                                                "Joshua" -> 24
                                                "Judges" -> 21
                                                "Ruth" -> 4
                                                "1 Samuel" -> 31
                                                "2 Samuel" -> 24
                                                "1 Kings" -> 22
                                                "2 Kings" -> 25
                                                "1 Chronicles" -> 29
                                                "2 Chronicles" -> 36
                                                "Ezra" -> 10
                                                "Nehemiah" -> 13
                                                "Esther" -> 10
                                                "Job" -> 42
                                                "Psalms" -> 150
                                                "Proverbs" -> 31
                                                "Ecclesiastes" -> 12
                                                "Song of Solomon" -> 8
                                                "Isaiah" -> 66
                                                "Jeremiah" -> 52
                                                "Lamentations" -> 5
                                                "Ezekiel" -> 48
                                                "Daniel" -> 12
                                                "Hosea" -> 14
                                                "Joel" -> 3
                                                "Amos" -> 9
                                                "Obadiah" -> 1
                                                "Jonah" -> 4
                                                "Micah" -> 7
                                                "Nahum" -> 3
                                                "Habakkuk" -> 3
                                                "Zephaniah" -> 3
                                                "Haggai" -> 2
                                                "Zechariah" -> 14
                                                "Malachi" -> 4
                                                "Matthew" -> 28
                                                "Mark" -> 16
                                                "Luke" -> 24
                                                "John" -> 21
                                                "Acts" -> 28
                                                "Romans" -> 16
                                                "1 Corinthians" -> 16
                                                "2 Corinthians" -> 13
                                                "Galatians" -> 6
                                                "Ephesians" -> 6
                                                "Philippians" -> 4
                                                "Colossians" -> 4
                                                "1 Thessalonians" -> 5
                                                "2 Thessalonians" -> 3
                                                "1 Timothy" -> 6
                                                "2 Timothy" -> 4
                                                "Titus" -> 3
                                                "Philemon" -> 1
                                                "Hebrews" -> 13
                                                "James" -> 5
                                                "1 Peter" -> 5
                                                "2 Peter" -> 3
                                                "1 John" -> 5
                                                "2 John" -> 1
                                                "3 John" -> 1
                                                "Jude" -> 1
                                                "Revelation" -> 22
                                                else -> 10
                                            }
                                            
                                            val columns = 6
                                            val chunkedChapters = (1..chaptersCount).toList().chunked(columns)
                                            
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                for (rowChapters in chunkedChapters) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        for (colIndex in 0 until columns) {
                                                            if (colIndex < rowChapters.size) {
                                                                val ch = rowChapters[colIndex]
                                                                val isSelected = selectedBookState == bookData.book && selectedChapterState == ch
                                                                
                                                                 Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .aspectRatio(1f)
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .background(
                                                                            if (isSelected) 
                                                                                Color(0xFF60A5FA) // Radiant light blue accent for selected chapter!
                                                                            else 
                                                                                Color(0xFFF1F5F9) // Subtle cool grey backing for readability
                                                                        )
                                                                        .clickable {
                                                                            viewModel.selectedBook = bookData.book
                                                                            viewModel.selectedChapter = ch
                                                                            showBookPicker = false
                                                                        },
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        text = "$ch",
                                                                        color = if (isSelected) Color.White else Color(0xFF334155),
                                                                        fontSize = 12.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontFamily = InterFontFamily
                                                                    )
                                                                }
                                                            } else {
                                                                // Empty filler Box so that rows align perfectly like a grid!
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .aspectRatio(1f)
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
                    }
                }
            }
        }
    }

    // Verse Action Sheets (Bookmark or Highlight picker)
    if (selectedVerseForAction != null) {
        val activeVerse = selectedVerseForAction!!
        ModalBottomSheet(
            onDismissRequest = { selectedVerseForAction = null },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = "${activeVerse.book} ${activeVerse.chapter}:${activeVerse.verseNum}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = "\"${activeVerse.text}\"",
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = LoraFontFamily,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Highlights color row picker
                Text("Highlight Passage", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    highlightColors.forEach { (name, color) ->
                        val isSelected = if (color == Color.Transparent) activeVerse.highlightColor == null else activeVerse.highlightColor == String.format("#%06X", 0xFFFFFF and color.value.toInt())
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (color == Color.Transparent) MaterialTheme.colorScheme.surfaceVariant else color)
                                .clickable {
                                    val hexString = if (color == Color.Transparent) null else String.format("#%06X", 0xFFFFFF and color.value.toInt())
                                    viewModel.applyHighlight(activeVerse, hexString)
                                    selectedVerseForAction = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = "Selected Highlight", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            } else if (color == Color.Transparent) {
                                Icon(Icons.Outlined.FormatColorReset, contentDescription = "Clear Highlight", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Quick actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Bookmark toggle
                    Button(
                        onClick = {
                            viewModel.toggleBookmark(activeVerse)
                            selectedVerseForAction = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeVerse.isBookmarked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (activeVerse.isBookmarked) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            if (activeVerse.isBookmarked) Icons.Filled.BookmarkRemove else Icons.Filled.Bookmark,
                            contentDescription = "Bookmark",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (activeVerse.isBookmarked) "Remove Bookmark" else "Bookmark")
                    }

                    // Copy quick action
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString("${activeVerse.book} ${activeVerse.chapter}:${activeVerse.verseNum} - ${activeVerse.text}"))
                            Toast.makeText(context, "Copied Verse", Toast.LENGTH_SHORT).show()
                            selectedVerseForAction = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Verse")
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarkHighlightRow(
    verse: BibleVerse,
    colorLabelColor: Color? = null,
    onGoTo: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onGoTo),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (colorLabelColor != null) {
                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(colorLabelColor))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "${verse.book} ${verse.chapter}:${verse.verseNum}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row {
                    IconButton(onClick = onGoTo, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = "Go to Chapter", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\"${verse.text}\"",
                fontSize = 14.sp,
                fontFamily = LoraFontFamily,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun BibleSearchView(viewModel: AppViewModel) {
    var textQuery by remember { mutableStateOf(viewModel.bibleSearchQuery) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = textQuery,
            onValueChange = {
                textQuery = it
                viewModel.searchBible(it)
            },
            placeholder = { Text("Search scriptures e.g. grace, love, Tondo") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            trailingIcon = {
                if (textQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        textQuery = ""
                        viewModel.searchBible("")
                    }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (viewModel.bibleSearchResults.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Filled.FindInPage, contentDescription = "No Results", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (textQuery.isBlank()) "Type keywords to search through local books" else "No matching scripture found containing \"$textQuery\"",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            Text(
                text = "Results found: ${viewModel.bibleSearchResults.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.bibleSearchResults) { verse ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectedBook = verse.book
                                viewModel.selectedChapter = verse.chapter
                                // Action to jump done automatically by user
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${verse.book} ${verse.chapter}:${verse.verseNum}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString("${verse.book} ${verse.chapter}:${verse.verseNum} - ${verse.text}"))
                                        Toast.makeText(context, "Copied verse ref", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = verse.text,
                                fontSize = 14.sp,
                                fontFamily = LoraFontFamily,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(260.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

private val ALL_BIBLE_BOOKS = listOf(
    // Old Testament (39 books)
    BookWithTestament("Genesis", "OT"),
    BookWithTestament("Exodus", "OT"),
    BookWithTestament("Leviticus", "OT"),
    BookWithTestament("Numbers", "OT"),
    BookWithTestament("Deuteronomy", "OT"),
    BookWithTestament("Joshua", "OT"),
    BookWithTestament("Judges", "OT"),
    BookWithTestament("Ruth", "OT"),
    BookWithTestament("1 Samuel", "OT"),
    BookWithTestament("2 Samuel", "OT"),
    BookWithTestament("1 Kings", "OT"),
    BookWithTestament("2 Kings", "OT"),
    BookWithTestament("1 Chronicles", "OT"),
    BookWithTestament("2 Chronicles", "OT"),
    BookWithTestament("Ezra", "OT"),
    BookWithTestament("Nehemiah", "OT"),
    BookWithTestament("Esther", "OT"),
    BookWithTestament("Job", "OT"),
    BookWithTestament("Psalms", "OT"),
    BookWithTestament("Proverbs", "OT"),
    BookWithTestament("Ecclesiastes", "OT"),
    BookWithTestament("Song of Solomon", "OT"),
    BookWithTestament("Isaiah", "OT"),
    BookWithTestament("Jeremiah", "OT"),
    BookWithTestament("Lamentations", "OT"),
    BookWithTestament("Ezekiel", "OT"),
    BookWithTestament("Daniel", "OT"),
    BookWithTestament("Hosea", "OT"),
    BookWithTestament("Joel", "OT"),
    BookWithTestament("Amos", "OT"),
    BookWithTestament("Obadiah", "OT"),
    BookWithTestament("Jonah", "OT"),
    BookWithTestament("Micah", "OT"),
    BookWithTestament("Nahum", "OT"),
    BookWithTestament("Habakkuk", "OT"),
    BookWithTestament("Zephaniah", "OT"),
    BookWithTestament("Haggai", "OT"),
    BookWithTestament("Zechariah", "OT"),
    BookWithTestament("Malachi", "OT"),

    // New Testament (27 books)
    BookWithTestament("Matthew", "NT"),
    BookWithTestament("Mark", "NT"),
    BookWithTestament("Luke", "NT"),
    BookWithTestament("John", "NT"),
    BookWithTestament("Acts", "NT"),
    BookWithTestament("Romans", "NT"),
    BookWithTestament("1 Corinthians", "NT"),
    BookWithTestament("2 Corinthians", "NT"),
    BookWithTestament("Galatians", "NT"),
    BookWithTestament("Ephesians", "NT"),
    BookWithTestament("Philippians", "NT"),
    BookWithTestament("Colossians", "NT"),
    BookWithTestament("1 Thessalonians", "NT"),
    BookWithTestament("2 Thessalonians", "NT"),
    BookWithTestament("1 Timothy", "NT"),
    BookWithTestament("2 Timothy", "NT"),
    BookWithTestament("Titus", "NT"),
    BookWithTestament("Philemon", "NT"),
    BookWithTestament("Hebrews", "NT"),
    BookWithTestament("James", "NT"),
    BookWithTestament("1 Peter", "NT"),
    BookWithTestament("2 Peter", "NT"),
    BookWithTestament("1 John", "NT"),
    BookWithTestament("2 John", "NT"),
    BookWithTestament("3 John", "NT"),
    BookWithTestament("Jude", "NT"),
    BookWithTestament("Revelation", "NT")
)
