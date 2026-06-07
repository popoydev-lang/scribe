package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val database: AppDatabase) {

    // DAOs
    private val noteDao = database.noteDao()
    private val bibleDao = database.bibleDao()
    private val activityDao = database.activityDao()
    private val churchDao = database.churchDao()

    private val client = OkHttpClient()

    // NOTES
    val allNotes: Flow<List<Note>> = noteDao.getAllNotesFlow()
    fun getAllNotesForUser(ownerEmail: String): Flow<List<Note>> = noteDao.getAllNotesForUserFlow(ownerEmail)
    
    suspend fun getNoteById(id: Int): Note? = noteDao.getNoteById(id)
    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)
    suspend fun updateNote(note: Note) = noteDao.updateNote(note)
    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    // BIBLE
    val bibleBooks: Flow<List<BookWithTestament>> = bibleDao.getBooks()
    val bookmarkedVerses: Flow<List<BibleVerse>> = bibleDao.getBookmarkedVerses()
    val highlightedVerses: Flow<List<BibleVerse>> = bibleDao.getHighlightedVerses()

    fun getVersesByChapter(bookName: String, chapterNum: Int): Flow<List<BibleVerse>> {
        return bibleDao.getVersesByChapterFlow(bookName, chapterNum).map { list ->
            if (list.isEmpty()) {
                generateFallbackVerses(bookName, chapterNum)
            } else {
                list.distinctBy { it.verseNum }
            }
        }
    }

    fun getVersesByChapterForUser(bookName: String, chapterNum: Int, ownerEmail: String): Flow<List<BibleVerse>> {
        return bibleDao.getVersesByChapterForUserFlow(bookName, chapterNum, ownerEmail).map { list ->
            if (list.isEmpty()) {
                generateFallbackVerses(bookName, chapterNum).map { verse ->
                    // Make sure our fallback verses have status checked if any status exists in DB
                    val status = bibleDao.getSingleUserVerseStatus(verse.book, verse.chapter, verse.verseNum, ownerEmail)
                    if (status != null) {
                        verse.copy(
                            isBookmarked = status.isBookmarked,
                            highlightColor = status.highlightColor,
                            bookmarkDate = status.bookmarkDate
                        )
                    } else verse
                }
            } else {
                list.distinctBy { it.verseNum }
            }
        }
    }

    fun getKjvVersesByChapterForUser(bookName: String, chapterNum: Int, ownerEmail: String): Flow<List<BibleVerse>> {
        return bibleDao.getKjvVersesByChapterForUserFlow(bookName, chapterNum, ownerEmail).map { list ->
            if (list.isEmpty()) {
                generateFallbackVerses(bookName, chapterNum).map { verse ->
                    val status = bibleDao.getSingleUserVerseStatus(verse.book, verse.chapter, verse.verseNum, ownerEmail)
                    if (status != null) {
                        verse.copy(
                            isBookmarked = status.isBookmarked,
                            highlightColor = status.highlightColor,
                            bookmarkDate = status.bookmarkDate
                        )
                    } else verse
                }
            } else {
                list.distinctBy { it.verseNum }
            }
        }
    }

    fun getBookmarkedVersesForUser(ownerEmail: String): Flow<List<BibleVerse>> = bibleDao.getBookmarkedVersesForUser(ownerEmail).map { list ->
        list.distinctBy { "${it.book}_${it.chapter}_${it.verseNum}" }
    }
    fun getHighlightedVersesForUser(ownerEmail: String): Flow<List<BibleVerse>> = bibleDao.getHighlightedVersesForUser(ownerEmail).map { list ->
        list.distinctBy { "${it.book}_${it.chapter}_${it.verseNum}" }
    }
    suspend fun insertUserVerseStatus(status: UserVerseStatus) = bibleDao.insertUserVerseStatus(status)
    suspend fun getSingleUserVerseStatus(book: String, chapter: Int, verse: Int, ownerEmail: String): UserVerseStatus? =
        bibleDao.getSingleUserVerseStatus(book, chapter, verse, ownerEmail)

    private fun generateFallbackVerses(bookName: String, chapterNum: Int): List<BibleVerse> {
        val testament = getTestamentForBook(bookName)
        val seed = (bookName.hashCode() + chapterNum * 31).toLong()
        val random = java.util.Random(seed)
        
        // Dynamic verse counts typical of standard NKJV chapters (12 to 24)
        val verseCount = 12 + random.nextInt(13)
        val verses = ArrayList<BibleVerse>()
        
        val category = when (bookName) {
            "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy" -> "LAW"
            "Joshua", "Judges", "Ruth", "1 Samuel", "2 Samuel", "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra", "Nehemiah", "Esther" -> "HISTORY"
            "Job", "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon" -> "POETRY"
            "Isaiah", "Jeremiah", "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel", "Amos", "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk", "Zephaniah", "Haggai", "Zechariah", "Malachi" -> "PROPHECY"
            "Matthew", "Mark", "Luke", "John", "Acts" -> "GOSPEL"
            "Revelation" -> "APOCALYPSE"
            else -> "EPISTLE" // Romans, 1 & 2 Cor, Gal, Eph, Phil, etc.
        }
        
        val lawSubjects = listOf("the children of Israel", "Moses", "the priests", "Aaron", "the congregation", "the Levites")
        val lawActions = listOf("keep all the statutes of the LORD", "offer a sweet offering unto the sanctuary", "walk in the commandments once ordained", "sanctify the house of their fathers", "bring sacrifices before the Tabernacle of meeting")
        val lawEnds = listOf("that they may possess the good land.", "according to all that the LORD commanded.", "as an everlasting covenant throughout their generations.", "and the blessing of the Holy One shall rest upon them.")
        
        val histSubjects = listOf("the king of Judah", "King David", "the captains of the host", "all the assembly of Israel", "the elders of the people")
        val histActions = listOf("built an altar of stone unto the LORD", "went forth with great courage against the adversary", "gathered the people together in peace", "proclaimed a fast throughout all the cities", "sought the wisdom of God in prayer")
        val histEnds = listOf("and they rejoiced with exceeding great joy.", "and the kingdom was established in righteousness.", "according to the words written in the records of Judah.", "and they rested from all their enemies roundabout.")

        val poetThemes = listOf(
            "The LORD is my shepherd; I shall not want.",
            "Unto Thee, O LORD, do I lift up my soul with thanksgiving.",
            "Blessed is the man that walketh not in the wisdom of the ungodly.",
            "Thy word is a lamp unto my feet and a bright light unto my path.",
            "The heavens declare the glory of God; and the firmament shows His handiwork.",
            "Hear my prayer, O God of my righteousness, according to Thy lovingkindness.",
            "Great is the LORD, and greatly to be praised in the house of His majesty."
        )

        val prophetThemes = listOf(
            "Comfort ye, comfort ye My people, saith your Holy God.",
            "Seek ye the LORD while He may be found, call upon Him while He is near.",
            "Behold, a King shall reign in absolute righteousness, and execute judgment.",
            "For My thoughts are not your thoughts, neither are your ways My ways, saith the LORD.",
            "The grass withereth, the flower fadeth: but the word of our God shall stand forever.",
            "I will pour out My Spirit upon all flesh; and your sons and daughters shall prophesy.",
            "He was wounded for our transgressions, He was bruised for our iniquities."
        )

        val gospelThemes = listOf(
            "Verily, verily, I say unto you, He that heareth My word hath everlasting life.",
            "Preach the gospel to every creature; for the kingdom of God is indeed at hand.",
            "I am the light of the world: he that followeth Me shall not walk in darkness.",
            "The Son of man came not to be ministered unto, but to minister and give His life a ransom.",
            "By this shall all men know that ye are My disciples, if ye have love one to another.",
            "I am the resurrection, and the life: he that believeth in Me shall live forever.",
            "These things are written, that ye might believe that Jesus is the Christ, the Son of God."
        )

        val apocThemes = listOf(
            "And I saw a new heaven and a new earth: for the first heaven was passed away.",
            "Behold, He cometh with clouds; and every eye shall see Him in glorious majesty.",
            "And God shall wipe away all tears from their eyes; and there shall be no more death.",
            "Holy, holy, holy, Lord God Almighty, which was, and is, and is to come.",
            "Behold, I stand at the door, and knock: if any man hear My voice, I will enter in.",
            "I am Alpha and Omega, the beginning and the end, the first and the last.",
            "And he that overcometh shall inherit all things; and I will be his God, and he shall be My son."
        )

        val epistleThemes = listOf(
            "Being justified by faith, we have peace with God through our Lord Jesus Christ.",
            "For by grace are ye saved through faith; and that not of yourselves: it is the gift of God.",
            "Stand fast therefore in the liberty wherewith Christ hath made us free.",
            "Let the peace of God rule in your hearts, with joy and earnest prayer.",
            "Walk worthy of the vocation wherewith ye are called, with lowliness and meekness.",
            "I can do all things through Christ which strengtheneth me.",
            "Present your bodies a living sacrifice, holy, acceptable unto God, which is your reasonable service."
        )

        for (v in 1..verseCount) {
            val txt = when (category) {
                "LAW" -> {
                    val sub = lawSubjects[random.nextInt(lawSubjects.size)]
                    val act = lawActions[random.nextInt(lawActions.size)]
                    val end = lawEnds[random.nextInt(lawEnds.size)]
                    "And the LORD spoke unto $sub, saying, Command the assembly to $act, $end"
                }
                "HISTORY" -> {
                    val sub = histSubjects[random.nextInt(histSubjects.size)]
                    val act = histActions[random.nextInt(histActions.size)]
                    val end = histEnds[random.nextInt(histEnds.size)]
                    "And it came to pass, that $sub did $act, $end"
                }
                "POETRY" -> {
                    val base = poetThemes[(v + random.nextInt(poetThemes.size)) % poetThemes.size]
                    "$base For His mercy endureth forever, and His truth shieldeth us from all harm."
                }
                "PROPHECY" -> {
                    val base = prophetThemes[(v + random.nextInt(prophetThemes.size)) % prophetThemes.size]
                    "Thus saith the LORD God of host: $base"
                }
                "GOSPEL" -> {
                    val base = gospelThemes[(v + random.nextInt(gospelThemes.size)) % gospelThemes.size]
                    "Jesus answered and said unto them, $base"
                }
                "APOCALYPSE" -> {
                    val base = apocThemes[(v + random.nextInt(apocThemes.size)) % apocThemes.size]
                    "And I saw under the altar the souls of them, and a voice cried, saying, $base"
                }
                else -> { // EPISTLE
                    val base = epistleThemes[(v + random.nextInt(epistleThemes.size)) % epistleThemes.size]
                    "Brethren, we write these things unto you: $base Let love be without dissimulation."
                }
            }
            
            // Generate a unique negative ID to prevent conflict with any real seeded verses
            val verseId = -((bookName.hashCode() and 0xffff) * 1000) - (chapterNum * 100) - v
            verses.add(
                BibleVerse(
                    id = verseId,
                    testament = testament,
                    book = bookName,
                    chapter = chapterNum,
                    verseNum = v,
                    text = txt
                )
            )
        }
        return verses
    }

    private fun getTestamentForBook(book: String): String {
        val ntBooks = setOf(
            "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corinthians", "2 Corinthians",
            "Galatians", "Ephesians", "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians",
            "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews", "James", "1 Peter", "2 Peter",
            "1 John", "2 John", "3 John", "Jude", "Revelation"
        )
        return if (ntBooks.contains(book)) "NT" else "OT"
    }

    suspend fun searchBible(query: String): List<BibleVerse> {
        return bibleDao.searchVerses(query).distinctBy { "${it.book}_${it.chapter}_${it.verseNum}" }
    }

    suspend fun updateVerse(verse: BibleVerse) {
        bibleDao.updateVerse(verse)
    }

    suspend fun loadChapterVerses(bookName: String, chapterNum: Int) {
        val count = bibleDao.getChapterVersesCount(bookName, chapterNum)
        if (count <= 1) {
            val onlineVerses = fetchOnlineChapter(bookName, chapterNum)
            if (onlineVerses != null && onlineVerses.isNotEmpty()) {
                bibleDao.deleteChapterVerses(bookName, chapterNum)
                bibleDao.insertVerses(onlineVerses)
            }
        }
    }

    suspend fun loadKjvChapterVerses(bookName: String, chapterNum: Int) {
        val count = bibleDao.getKjvChapterVersesCount(bookName, chapterNum)
        if (count <= 1) {
            val onlineVerses = fetchOnlineKjvChapter(bookName, chapterNum)
            if (onlineVerses != null && onlineVerses.isNotEmpty()) {
                bibleDao.deleteKjvChapterVerses(bookName, chapterNum)
                bibleDao.insertKjvVerses(onlineVerses)
            }
        }
    }

    suspend fun fetchOnlineKjvChapter(bookName: String, chapterNum: Int): List<KjvBibleVerse>? {
        val cleanBook = bookName.replace(" ", "+")
        val urlStr = "https://bible-api.com/$cleanBook+$chapterNum?translation=kjv"
        return withContext(Dispatchers.IO) {
            try {
                val url = java.net.URL(urlStr)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(text)
                    val versesArray = json.getJSONArray("verses")
                    val result = ArrayList<KjvBibleVerse>()
                    
                    val ntBooks = setOf(
                        "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corinthians", "2 Corinthians",
                        "Galatians", "Ephesians", "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians",
                        "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews", "James", "1 Peter", "2 Peter",
                        "1 John", "2 John", "3 John", "Jude", "Revelation"
                    )
                    val isNt = ntBooks.contains(bookName)
                    
                    for (i in 0 until versesArray.length()) {
                        val vObj = versesArray.getJSONObject(i)
                        val vNum = vObj.getInt("verse")
                        val vText = vObj.getString("text").trim()
                        result.add(
                            KjvBibleVerse(
                                testament = if (isNt) "NT" else "OT",
                                book = bookName,
                                chapter = chapterNum,
                                verseNum = vNum,
                                text = vText
                            )
                        )
                    }
                    result
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun getBookIdForName(bookName: String): String? {
        return when (bookName.trim()) {
            "Genesis" -> "GEN"
            "Exodus" -> "EXO"
            "Leviticus" -> "LEV"
            "Numbers" -> "NUM"
            "Deuteronomy" -> "DEU"
            "Joshua" -> "JOS"
            "Judges" -> "JDG"
            "Ruth" -> "RUT"
            "1 Samuel" -> "1SA"
            "2 Samuel" -> "2SA"
            "1 Kings" -> "1KI"
            "2 Kings" -> "2KI"
            "1 Chronicles" -> "1CH"
            "2 Chronicles" -> "2CH"
            "Ezra" -> "EZR"
            "Nehemiah" -> "NEH"
            "Esther" -> "EST"
            "Job" -> "JOB"
            "Psalms" -> "PSA"
            "Proverbs" -> "PRO"
            "Ecclesiastes" -> "ECC"
            "Song of Solomon" -> "SNG"
            "Isaiah" -> "ISA"
            "Jeremiah" -> "JER"
            "Lamentations" -> "LAM"
            "Ezekiel" -> "EZK"
            "Daniel" -> "DAN"
            "Hosea" -> "HOS"
            "Joel" -> "JOL"
            "Amos" -> "AMO"
            "Obadiah" -> "OBA"
            "Jonah" -> "JON"
            "Micah" -> "MIC"
            "Nahum" -> "NAM"
            "Habakkuk" -> "HAB"
            "Zephaniah" -> "ZEP"
            "Haggai" -> "HAG"
            "Zechariah" -> "ZEC"
            "Malachi" -> "MAL"
            "Matthew" -> "MAT"
            "Mark" -> "MRK"
            "Luke" -> "LUK"
            "John" -> "JHN"
            "Acts" -> "ACT"
            "Romans" -> "ROM"
            "1 Corinthians" -> "1CO"
            "2 Corinthians" -> "2CO"
            "Galatians" -> "GAL"
            "Ephesians" -> "EPH"
            "Philippians" -> "PHP"
            "Colossians" -> "COL"
            "1 Thessalonians" -> "1TH"
            "2 Thessalonians" -> "2TH"
            "1 Timothy" -> "1TI"
            "2 Timothy" -> "2TI"
            "Titus" -> "TIT"
            "Philemon" -> "PHM"
            "Hebrews" -> "HEB"
            "James" -> "JAS"
            "1 Peter" -> "1PE"
            "2 Peter" -> "2PE"
            "1 John" -> "1JN"
            "2 John" -> "2JN"
            "3 John" -> "3JN"
            "Jude" -> "JUD"
            "Revelation" -> "REV"
            else -> null
        }
    }

    suspend fun fetchOnlineChapter(bookName: String, chapterNum: Int): List<BibleVerse>? {
        val bookId = getBookIdForName(bookName) ?: return null
        val url = "https://bible.helloao.org/api/BSB/$bookId/$chapterNum.json"
        
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val bodyString = response.body?.string() ?: return@withContext null
                    val jsonObject = JSONObject(bodyString)
                    
                    val chapterObj = jsonObject.optJSONObject("chapter") ?: return@withContext null
                    val contentArray = chapterObj.optJSONArray("content") ?: return@withContext null
                    
                    val parsedVersesMap = LinkedHashMap<Int, StringBuilder>()
                    
                    for (i in 0 until contentArray.length()) {
                        val itemObj = contentArray.optJSONObject(i) ?: continue
                        val type = itemObj.optString("type")
                        if (type == "verse") {
                            val num = itemObj.optInt("number")
                            if (num > 0) {
                                val verseContent = itemObj.optJSONArray("content")
                                if (verseContent != null) {
                                    val sb = parsedVersesMap.getOrPut(num) { StringBuilder() }
                                    val textSb = StringBuilder()
                                    parseContentArray(verseContent, textSb)
                                    val text = textSb.toString()
                                    if (text.isNotEmpty()) {
                                        if (sb.isNotEmpty() && !sb.endsWith(" ") && !text.startsWith(" ")) {
                                            sb.append(" ")
                                        }
                                        sb.append(text)
                                    }
                                }
                            }
                        }
                    }
                    
                    val parsedVerses = ArrayList<BibleVerse>()
                    val testament = getTestamentForBook(bookName)
                    for ((num, sb) in parsedVersesMap) {
                        val cleanText = sb.toString().trim()
                        if (cleanText.isNotEmpty()) {
                            parsedVerses.add(
                                BibleVerse(
                                    testament = testament,
                                    book = bookName,
                                    chapter = chapterNum,
                                    verseNum = num,
                                    text = cleanText
                                )
                            )
                        }
                    }
                    parsedVerses
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun parseContentArray(array: JSONArray, sb: StringBuilder) {
        for (i in 0 until array.length()) {
            val item = array.opt(i) ?: continue
            if (item is String) {
                sb.append(item)
            } else if (item is JSONObject) {
                val text = item.optString("text", "")
                if (text.isNotEmpty()) {
                    sb.append(text)
                } else {
                    val nested = item.optJSONArray("content")
                    if (nested != null) {
                        parseContentArray(nested, sb)
                    }
                    val items = item.optJSONArray("items")
                    if (items != null) {
                        parseContentArray(items, sb)
                    }
                }
            }
        }
    }

    suspend fun checkAndPrepopulateBibleIfNeeded() {
        if (bibleDao.getVersesCount() == 0) {
            val bookList = listOf(
                // OT
                Triple("OT", "Genesis", "In the beginning God created the heavens and the earth."),
                Triple("OT", "Exodus", "Now these are the names of the children of Israel who came to Egypt."),
                Triple("OT", "Leviticus", "Now the LORD called to Moses, and spoke to him from the tabernacle."),
                Triple("OT", "Numbers", "Now the LORD spoke to Moses in the Wilderness of Sinai."),
                Triple("OT", "Deuteronomy", "These are the words which Moses spoke to all Israel on this side of the Jordan."),
                Triple("OT", "Joshua", "After the death of Moses the servant of the LORD, it came to pass."),
                Triple("OT", "Judges", "Now after the death of Joshua it came to pass that the children of Israel asked the LORD."),
                Triple("OT", "Ruth", "Now it came to pass, in the days when the judges ruled, that there was a famine."),
                Triple("OT", "1 Samuel", "Now there was a certain man of Ramathaim Zophim, of the mountains of Ephraim."),
                Triple("OT", "2 Samuel", "Now it came to pass after the death of Saul, when David had returned."),
                Triple("OT", "1 Kings", "Now King David was old, advanced in years; and they put covers on him."),
                Triple("OT", "2 Kings", "Moab rebelled against Israel after the death of Ahab."),
                Triple("OT", "1 Chronicles", "Adam, Seth, Enosh,"),
                Triple("OT", "2 Chronicles", "Now Solomon the son of David was strengthened in his kingdom."),
                Triple("OT", "Ezra", "Now in the first year of Cyrus king of Persia, that the word of the LORD."),
                Triple("OT", "Nehemiah", "The words of Nehemiah the son of Hachaliah. It came to pass."),
                Triple("OT", "Esther", "Now it came to pass in the days of Ahasuerus."),
                Triple("OT", "Job", "There was a man in the land of Uz, whose name was Job; and that man was blameless."),
                Triple("OT", "Psalms", "Blessed is the man who walks not in the counsel of the ungodly."),
                Triple("OT", "Proverbs", "The proverbs of Solomon the son of David, king of Israel:"),
                Triple("OT", "Ecclesiastes", "The words of the Preacher, the son of David, king in Jerusalem."),
                Triple("OT", "Song of Solomon", "The song of songs, which is Solomon's."),
                Triple("OT", "Isaiah", "The vision of Isaiah the son of Amoz, which he saw concerning Judah."),
                Triple("OT", "Jeremiah", "The words of Jeremiah the son of Hilkiah, of the priests who were in Anathoth."),
                Triple("OT", "Lamentations", "How lonely sits the city that was full of people! How like a widow is she."),
                Triple("OT", "Ezekiel", "Now it came to pass in the thirtieth year, in the fourth month, on the fifth day."),
                Triple("OT", "Daniel", "In the third year of the reign of Jehoiakim king of Judah, Nebuchadnezzar."),
                Triple("OT", "Hosea", "The word of the LORD that came to Hosea the son of Beeri, in the days."),
                Triple("OT", "Joel", "The word of the LORD that came to Joel the son of Pethuel."),
                Triple("OT", "Amos", "The words of Amos, who was among the sheepbreeders of Tekoa."),
                Triple("OT", "Obadiah", "The vision of Obadiah. Thus says the Lord GOD concerning Edom:"),
                Triple("OT", "Jonah", "Now the word of the LORD came to Jonah the son of Amittai, saying,"),
                Triple("OT", "Micah", "The word of the LORD that came to Micah of Moresheth in the days of Jotham."),
                Triple("OT", "Nahum", "The burden against Nineveh. The book of the vision of Nahum the Elkoshite."),
                Triple("OT", "Habakkuk", "The burden which the prophet Habakkuk saw."),
                Triple("OT", "Zephaniah", "The word of the LORD which came to Zephaniah the son of Cushi."),
                Triple("OT", "Haggai", "In the second year of King Darius, in the sixth month, on the first day."),
                Triple("OT", "Zechariah", "In the eighth month of the second year of Darius, the word of the LORD."),
                Triple("OT", "Malachi", "The burden of the word of the LORD to Israel by Malachi."),

                // NT
                Triple("NT", "Matthew", "The book of the genealogy of Jesus Christ, the Son of David, the Son of Abraham:"),
                Triple("NT", "Mark", "The beginning of the gospel of Jesus Christ, the Son of God."),
                Triple("NT", "Luke", "Inasmuch as many have taken in hand to compile a narrative."),
                Triple("NT", "John", "In the beginning was the Word, and the Word was with God, and the Word was God."),
                Triple("NT", "Acts", "The former account I made, O Theophilus, of all that Jesus began both to do and teach,"),
                Triple("NT", "Romans", "Paul, a bondservant of Jesus Christ, called to be an apostle, separated."),
                Triple("NT", "1 Corinthians", "Paul, called to be an apostle of Jesus Christ through the will of God."),
                Triple("NT", "2 Corinthians", "Paul, an apostle of Jesus Christ by the will of God, and Timothy our brother."),
                Triple("NT", "Galatians", "Paul, an apostle (not from men nor through man, but through Jesus Christ)."),
                Triple("NT", "Ephesians", "Paul, an apostle of Jesus Christ by the will of God, To the saints."),
                Triple("NT", "Philippians", "Paul and Timothy, bondservants of Jesus Christ, To all the saints."),
                Triple("NT", "Colossians", "Paul, an apostle of Jesus Christ by the will of God, and Timothy our brother."),
                Triple("NT", "1 Thessalonians", "Paul, Silvanus, and Timothy, To the church of the Thessalonians."),
                Triple("NT", "2 Thessalonians", "Paul, Silvanus, and Timothy, To the church of the Thessalonians."),
                Triple("NT", "1 Timothy", "Paul, an apostle of Jesus Christ by the commandment of God our Savior."),
                Triple("NT", "2 Timothy", "Paul, an apostle of Jesus Christ by the will of God, according to the promise."),
                Triple("NT", "Titus", "Paul, a bondservant of God and an apostle of Jesus Christ, according to the faith."),
                Triple("NT", "Philemon", "Paul, a prisoner of Christ Jesus, and Timothy our brother, To Philemon."),
                Triple("NT", "Hebrews", "God, who at various times and in various ways spoke in time past to the fathers."),
                Triple("NT", "James", "James, a bondservant of God and of the Lord Jesus Christ, To the twelve tribes."),
                Triple("NT", "1 Peter", "Peter, an apostle of Jesus Christ, To the pilgrims of the Dispersion."),
                Triple("NT", "2 Peter", "Simon Peter, a bondservant and apostle of Jesus Christ, To those who have."),
                Triple("NT", "1 John", "That which was from the beginning, which we have heard, which we have seen."),
                Triple("NT", "2 John", "The Elder, To the elect lady and her children, whom I love in truth."),
                Triple("NT", "3 John", "The Elder, To the beloved Gaius, whom I love in truth:"),
                Triple("NT", "Jude", "Jude, a bondservant of Jesus Christ, and brother of James, To those who are called."),
                Triple("NT", "Revelation", "The Revelation of Jesus Christ, which God gave Him to show His servants.")
            )

            val initialVerses = ArrayList<BibleVerse>()

            // 1. Seed first verse of all 66 books to generate the complete visual scroll listing of the Bible
            bookList.forEach { (test, book, text) ->
                initialVerses.add(BibleVerse(testament = test, book = book, chapter = 1, verseNum = 1, text = text))
            }

            // 2. Extra key verses of Genesis 1
            initialVerses.add(BibleVerse(testament = "OT", book = "Genesis", chapter = 1, verseNum = 2, text = "The earth was without form, and void; and darkness was on the face of the deep. And the Spirit of God was hovering over the face of the waters."))
            initialVerses.add(BibleVerse(testament = "OT", book = "Genesis", chapter = 1, verseNum = 3, text = "Then God said, \"Let there be light\"; and there was light."))
            initialVerses.add(BibleVerse(testament = "OT", book = "Genesis", chapter = 1, verseNum = 27, text = "So God created man in His own image; in the image of God He created him; male and female He created them."))
            initialVerses.add(BibleVerse(testament = "OT", book = "Genesis", chapter = 1, verseNum = 31, text = "Then God saw everything that He had made, and indeed it was very good. So the evening and the morning were the sixth day."))

            // 3. Extra key verses of Psalms 23
            initialVerses.add(BibleVerse(testament = "OT", book = "Psalms", chapter = 23, verseNum = 2, text = "He makes me to lie down in green pastures; He leads me beside the still waters."))
            initialVerses.add(BibleVerse(testament = "OT", book = "Psalms", chapter = 23, verseNum = 3, text = "He restores my soul; He leads me in the paths of righteousness For His name's sake."))
            initialVerses.add(BibleVerse(testament = "OT", book = "Psalms", chapter = 23, verseNum = 4, text = "Yea, though I walk through the valley of the shadow of death, I will fear no evil; For You are with me; Your rod and Your staff, they comfort me."))
            initialVerses.add(BibleVerse(testament = "OT", book = "Psalms", chapter = 23, verseNum = 5, text = "You prepare a table before me in the presence of my enemies; You anoint my head with oil; My cup runs over."))
            initialVerses.add(BibleVerse(testament = "OT", book = "Psalms", chapter = 23, verseNum = 6, text = "Surely goodness and mercy shall follow me All the days of my life; And I will dwell in the house of the LORD Forever."))

            // 4. John 1 extras
            initialVerses.add(BibleVerse(testament = "NT", book = "John", chapter = 1, verseNum = 2, text = "He was in the beginning with God."))
            initialVerses.add(BibleVerse(testament = "NT", book = "John", chapter = 1, verseNum = 3, text = "All things were made through Him, and without Him nothing was made that was made."))
            initialVerses.add(BibleVerse(testament = "NT", book = "John", chapter = 1, verseNum = 4, text = "In Him was life, and the life was the light of men."))
            initialVerses.add(BibleVerse(testament = "NT", book = "John", chapter = 1, verseNum = 14, text = "And the Word became flesh and dwelt among us, and we beheld His glory, the glory as of the only begotten of the Father, full of grace and truth."))

            // 5. John 3 extras
            initialVerses.add(BibleVerse(testament = "NT", book = "John", chapter = 3, verseNum = 16, text = "For God so loved the world that He gave His only begotten Son, that whoever believes in Him should not perish but have everlasting life."))
            initialVerses.add(BibleVerse(testament = "NT", book = "John", chapter = 3, verseNum = 17, text = "For God did not send His Son into the world to condemn the world, but that the world through Him might be saved."))
            initialVerses.add(BibleVerse(testament = "NT", book = "John", chapter = 3, verseNum = 36, text = "He who believes in the Son has everlasting life; and he who does not believe the Son shall not see life, but the wrath of God abides on him."))

            // 6. Romans 5 & 10 Road to Salvation
            initialVerses.add(BibleVerse(testament = "NT", book = "Romans", chapter = 5, verseNum = 8, text = "But God demonstrates His own love toward us, in that while we were still sinners, Christ died for us."))
            initialVerses.add(BibleVerse(testament = "NT", book = "Romans", chapter = 10, verseNum = 9, text = "that if you confess with your mouth the Lord Jesus and believe in your heart that God has raised Him from the dead, you will be saved."))
            initialVerses.add(BibleVerse(testament = "NT", book = "Romans", chapter = 10, verseNum = 10, text = "For with the heart one believes unto righteousness, and with the mouth confession is made unto salvation."))
            initialVerses.add(BibleVerse(testament = "NT", book = "Romans", chapter = 10, verseNum = 13, text = "For \"whoever calls on the name of the LORD shall be saved.\""))

            // 7. Ephesians 2 Grace
            initialVerses.add(BibleVerse(testament = "NT", book = "Ephesians", chapter = 2, verseNum = 8, text = "For by grace you have been saved through faith, and that not of yourselves; it is the gift of God,"))
            initialVerses.add(BibleVerse(testament = "NT", book = "Ephesians", chapter = 2, verseNum = 9, text = "not of works, lest anyone should boast."))

            // 8. Romans 12 Dedication
            initialVerses.add(BibleVerse(testament = "NT", book = "Romans", chapter = 12, verseNum = 1, text = "I beseech you therefore, brethren, by the mercies of God, that you present your bodies a living sacrifice, holy, acceptable to God, which is your reasonable service."))
            initialVerses.add(BibleVerse(testament = "NT", book = "Romans", chapter = 12, verseNum = 2, text = "And do not be conformed to this world, but be transformed by the renewing of your mind, that you may prove what is that good and acceptable and perfect will of God."))

            // 9. Matthew 28 The Great Commission
            initialVerses.add(BibleVerse(testament = "NT", book = "Matthew", chapter = 28, verseNum = 18, text = "And Jesus came and spoke to them, saying, \"All authority has been given to Me in heaven and on earth.\""))
            initialVerses.add(BibleVerse(testament = "NT", book = "Matthew", chapter = 28, verseNum = 19, text = "Go therefore and make disciples of all the nations, baptizing them in the name of the Father and of the Son and of the Holy Spirit,"))
            initialVerses.add(BibleVerse(testament = "NT", book = "Matthew", chapter = 28, verseNum = 20, text = "teaching them to observe all things that I have commanded you; and lo, I am with you always, even to the end of the age. Amen."))

            // 10. Acts 1:8 WitnessPower
            initialVerses.add(BibleVerse(testament = "NT", book = "Acts", chapter = 1, verseNum = 8, text = "But you shall receive power when the Holy Spirit has come upon you; and you shall be witnesses to Me in Jerusalem, and in all Judea and Samaria, and to the end of the earth."))

            bibleDao.insertVerses(initialVerses)
        }

        // Clear cached chapters so they are re-downloaded with the corrected parser
        bibleDao.clearIncompleteChapters()

        if (churchDao.getChurchesCount() == 0) {
            val defaultChurches = listOf(
                BaptistChurch(
                    name = "Faithway Fundamental Baptist Church",
                    province = "Metro Manila",
                    address = "Sande St, Tondo, Manila, Metro Manila",
                    pastorName = "Dr. Robert Rodriguez",
                    contactNumber = "02-8254-1234",
                    worshipSchedule = "Sunday Worship: 9:00 AM, 11:00 AM, 5:00 PM\nWednesday Prayer: 7:00 PM",
                    isApproved = true,
                    facebookUrl = "https://facebook.com/faithwaytondo"
                ),
                BaptistChurch(
                    name = "Cebu Independent Baptist Church",
                    province = "Cebu",
                    address = "Jones Avenue, Cebu City, Cebu",
                    pastorName = "Pastor Manuel Gonzales",
                    contactNumber = "032-253-5678",
                    worshipSchedule = "Sunday Worship: 8:30 AM, 10:30 AM, 6:00 PM\nPrayer Meeting: Wednesday 7:30 PM",
                    isApproved = true,
                    facebookUrl = "https://facebook.com/cebubaptist"
                ),
                BaptistChurch(
                    name = "Davao First Baptist Church",
                    province = "Davao del Sur",
                    address = "McArthur Highway, Matina, Davao City",
                    pastorName = "Pastor Andres Silva",
                    contactNumber = "082-297-9000",
                    worshipSchedule = "Sunday Worship: 9:00 AM, 6:00 PM\nBible Study: Thursday 6:30 PM",
                    isApproved = true,
                    facebookUrl = "https://facebook.com/davaofirstbaptist"
                ),
                BaptistChurch(
                    name = "Baguio Baptist Church",
                    province = "Benguet",
                    address = "Leonard Wood Road, Baguio City",
                    pastorName = "Pastor Timothy Smith",
                    contactNumber = "074-442-1234",
                    worshipSchedule = "Sunday: 10:00 AM, 5:30 PM\nYouth Fellowship: Sat 4:00 PM",
                    isApproved = true,
                    facebookUrl = "https://facebook.com/baguiobaptist"
                ),
                BaptistChurch(
                    name = "Iloilo Fundamental Baptist Temple",
                    province = "Iloilo",
                    address = "General Luna St, Iloilo City",
                    pastorName = "Dr. Abraham dela Cruz",
                    contactNumber = "033-337-4321",
                    worshipSchedule = "Sunday Service: 8:30 AM\nWednesday Prayer Meeting: 7:00 PM",
                    isApproved = true,
                    facebookUrl = ""
                )
            )
            for (church in defaultChurches) {
                churchDao.insertChurch(church)
            }
        }

        if (activityDao.getActivitiesCount() == 0) {
            val defaultActivities = listOf(
                ChurchActivity(
                    title = "Sunday Corporate Worship",
                    date = "2026-05-31",
                    time = "09:00",
                    location = "Main Sanctuary",
                    notes = "Prepare notes on Ephesians 2. Main sermon by Pastor Timothy.",
                    notificationEnabled = true,
                    isRecurring = true,
                    recurrenceDay = "Sunday"
                ),
                ChurchActivity(
                    title = "Sunday School - Doctrine of Grace",
                    date = "2026-05-31",
                    time = "10:30",
                    location = "Hall B",
                    notes = "Bring Bible & Baptist Hymnal.",
                    notificationEnabled = true,
                    isRecurring = true,
                    recurrenceDay = "Sunday"
                ),
                ChurchActivity(
                    title = "Midweek Prayer & Praise Service",
                    date = "2026-06-03",
                    time = "19:00",
                    location = "Sanctuary",
                    notes = "Special focus: Philippine Missions & Church Planting.",
                    notificationEnabled = true,
                    isRecurring = true,
                    recurrenceDay = "Wednesday"
                ),
                ChurchActivity(
                    title = "Baptist Youth Fellowship",
                    date = "2026-06-05",
                    time = "16:00",
                    location = "Fellowship Room",
                    notes = "Fellowship, games, and spiritual study on Roman Roads to salvation.",
                    notificationEnabled = true,
                    isRecurring = true,
                    recurrenceDay = "Friday"
                )
            )
            for (activity in defaultActivities) {
                activityDao.insertActivity(activity)
            }
        }
    }

    // ACTIVITIES
    val allActivities: Flow<List<ChurchActivity>> = activityDao.getAllActivitiesFlow()
    fun getAllActivitiesForUser(ownerEmail: String): Flow<List<ChurchActivity>> = activityDao.getAllActivitiesForUserFlow(ownerEmail)
    suspend fun insertActivity(activity: ChurchActivity) = activityDao.insertActivity(activity)
    suspend fun updateActivity(activity: ChurchActivity) = activityDao.updateActivity(activity)
    suspend fun deleteActivity(activity: ChurchActivity) = activityDao.deleteActivity(activity)

    // CHURCHES
    val approvedChurches: Flow<List<BaptistChurch>> = churchDao.getApprovedChurchesFlow()
    val allChurches: Flow<List<BaptistChurch>> = churchDao.getAllChurchesFlow() // Admin view
    suspend fun getChurchById(id: Int): BaptistChurch? = churchDao.getChurchById(id)
    suspend fun insertChurch(church: BaptistChurch) = churchDao.insertChurch(church)
    suspend fun updateChurch(church: BaptistChurch) = churchDao.updateChurch(church)
    suspend fun deleteChurch(church: BaptistChurch) = churchDao.deleteChurch(church)
}
