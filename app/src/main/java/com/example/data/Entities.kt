package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. NOTES ENTITY & DAO
// ==========================================

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // "Corporate Worship", "Sunday School", "Other Classes"
    val title: String,
    val date: String,
    val speaker: String,
    val churchName: String,
    val bibleVerses: String,
    val richTextNotes: String,
    val photoUri: String? = null,
    val tags: String = "", // comma-separated
    val isFavorite: Boolean = false,
    val syncStatus: String = "synced", // "synced" or "pending"
    val ownerEmail: String = "" // Multi-account isolation
)

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY date DESC, id DESC")
    fun getAllNotesFlow(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE ownerEmail = :ownerEmail ORDER BY date DESC, id DESC")
    fun getAllNotesForUserFlow(ownerEmail: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Int): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)
}

// ==========================================
// 2. BIBLE (NKJV) ENTITY & DAO
// ==========================================

@Entity(
    tableName = "user_verse_status",
    primaryKeys = ["book", "chapter", "verseNum", "ownerEmail"]
)
data class UserVerseStatus(
    val book: String,
    val chapter: Int,
    val verseNum: Int,
    val ownerEmail: String,
    val isBookmarked: Boolean = false,
    val highlightColor: String? = null,
    val bookmarkDate: Long = 0L
)

@Entity(tableName = "bible_verses")
data class BibleVerse(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val testament: String, // "OT" or "NT"
    val book: String,      // e.g. "Genesis", "John"
    val chapter: Int,
    val verseNum: Int,
    val text: String,
    val isBookmarked: Boolean = false,
    val highlightColor: String? = null, // Hex color or null
    val bookmarkDate: Long = 0L
)

@Entity(tableName = "kjv_bible_verses")
data class KjvBibleVerse(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val testament: String, // "OT" or "NT"
    val book: String,      // e.g. "Genesis", "John"
    val chapter: Int,
    val verseNum: Int,
    val text: String
)

@Dao
interface BibleDao {
    @Query("SELECT DISTINCT book, testament FROM bible_verses ORDER BY id ASC")
    fun getBooks(): Flow<List<BookWithTestament>>

    @Query("SELECT * FROM bible_verses WHERE book = :bookName AND chapter = :chapterNum GROUP BY verseNum ORDER BY verseNum ASC")
    fun getVersesByChapterFlow(bookName: String, chapterNum: Int): Flow<List<BibleVerse>>

    @Query("""
        SELECT bv.id, bv.testament, bv.book, bv.chapter, bv.verseNum, bv.text, 
               COALESCE(uvs.isBookmarked, 0) as isBookmarked, 
               uvs.highlightColor as highlightColor, 
               COALESCE(uvs.bookmarkDate, 0) as bookmarkDate 
        FROM bible_verses bv 
        LEFT OUTER JOIN user_verse_status uvs 
        ON bv.book = uvs.book AND bv.chapter = uvs.chapter AND bv.verseNum = uvs.verseNum AND uvs.ownerEmail = :ownerEmail 
        WHERE bv.book = :bookName AND bv.chapter = :chapterNum 
        GROUP BY bv.book, bv.chapter, bv.verseNum 
        ORDER BY bv.verseNum ASC
    """)
    fun getVersesByChapterForUserFlow(bookName: String, chapterNum: Int, ownerEmail: String): Flow<List<BibleVerse>>

    @Query("""
        SELECT uvs.rowid as id,
               COALESCE(bv.testament, COALESCE(kbv.testament, 'NT')) as testament,
               uvs.book as book,
               uvs.chapter as chapter,
               uvs.verseNum as verseNum,
               COALESCE(bv.text, COALESCE(kbv.text, 'Scripture verse text...')) as text,
               uvs.isBookmarked as isBookmarked, 
               uvs.highlightColor as highlightColor, 
               uvs.bookmarkDate as bookmarkDate 
        FROM user_verse_status uvs 
        LEFT OUTER JOIN bible_verses bv 
          ON uvs.book = bv.book AND uvs.chapter = bv.chapter AND uvs.verseNum = bv.verseNum 
        LEFT OUTER JOIN kjv_bible_verses kbv 
          ON uvs.book = kbv.book AND uvs.chapter = kbv.chapter AND uvs.verseNum = kbv.verseNum
        WHERE uvs.ownerEmail = :ownerEmail AND uvs.isBookmarked = 1 
        GROUP BY uvs.book, uvs.chapter, uvs.verseNum 
        ORDER BY uvs.bookmarkDate DESC
    """)
    fun getBookmarkedVersesForUser(ownerEmail: String): Flow<List<BibleVerse>>

    @Query("""
        SELECT uvs.rowid as id,
               COALESCE(bv.testament, COALESCE(kbv.testament, 'NT')) as testament,
               uvs.book as book,
               uvs.chapter as chapter,
               uvs.verseNum as verseNum,
               COALESCE(bv.text, COALESCE(kbv.text, 'Scripture verse text...')) as text,
               uvs.isBookmarked as isBookmarked, 
               uvs.highlightColor as highlightColor, 
               uvs.bookmarkDate as bookmarkDate 
        FROM user_verse_status uvs 
        LEFT OUTER JOIN bible_verses bv 
          ON uvs.book = bv.book AND uvs.chapter = bv.chapter AND uvs.verseNum = bv.verseNum 
        LEFT OUTER JOIN kjv_bible_verses kbv 
          ON uvs.book = kbv.book AND uvs.chapter = kbv.chapter AND uvs.verseNum = kbv.verseNum
        WHERE uvs.ownerEmail = :ownerEmail AND uvs.highlightColor IS NOT NULL
        GROUP BY uvs.book, uvs.chapter, uvs.verseNum 
        ORDER BY uvs.bookmarkDate DESC
    """)
    fun getHighlightedVersesForUser(ownerEmail: String): Flow<List<BibleVerse>>

    @Query("SELECT * FROM bible_verses WHERE isBookmarked = 1 ORDER BY bookmarkDate DESC")
    fun getBookmarkedVerses(): Flow<List<BibleVerse>>

    @Query("SELECT * FROM bible_verses WHERE highlightColor IS NOT NULL")
    fun getHighlightedVerses(): Flow<List<BibleVerse>>

    @Query("SELECT * FROM bible_verses WHERE text LIKE '%' || :query || '%' OR book LIKE '%' || :query || '%' LIMIT 100")
    suspend fun searchVerses(query: String): List<BibleVerse>

    @Update
    suspend fun updateVerse(verse: BibleVerse)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserVerseStatus(status: UserVerseStatus)

    @Query("SELECT * FROM user_verse_status WHERE book = :book AND chapter = :chapter AND verseNum = :verse AND ownerEmail = :ownerEmail LIMIT 1")
    suspend fun getSingleUserVerseStatus(book: String, chapter: Int, verse: Int, ownerEmail: String): UserVerseStatus?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(verses: List<BibleVerse>)

    @Query("SELECT COUNT(*) FROM bible_verses")
    suspend fun getVersesCount(): Int

    @Query("SELECT COUNT(DISTINCT verseNum) FROM bible_verses WHERE book = :bookName AND chapter = :chapterNum")
    suspend fun getChapterVersesCount(bookName: String, chapterNum: Int): Int

    @Query("DELETE FROM bible_verses WHERE book = :bookName AND chapter = :chapterNum")
    suspend fun deleteChapterVerses(bookName: String, chapterNum: Int)

    @Query("DELETE FROM bible_verses WHERE isBookmarked = 0 AND highlightColor IS NULL AND (chapter > 1 OR verseNum > 1)")
    suspend fun clearIncompleteChapters()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKjvVerses(verses: List<KjvBibleVerse>)

    @Query("SELECT COUNT(DISTINCT verseNum) FROM kjv_bible_verses WHERE book = :bookName AND chapter = :chapterNum")
    suspend fun getKjvChapterVersesCount(bookName: String, chapterNum: Int): Int

    @Query("DELETE FROM kjv_bible_verses WHERE book = :bookName AND chapter = :chapterNum")
    suspend fun deleteKjvChapterVerses(bookName: String, chapterNum: Int)

    @Query("""
        SELECT kbv.id, kbv.testament, kbv.book, kbv.chapter, kbv.verseNum, kbv.text, 
               COALESCE(uvs.isBookmarked, 0) as isBookmarked, 
               uvs.highlightColor as highlightColor, 
               COALESCE(uvs.bookmarkDate, 0) as bookmarkDate 
        FROM kjv_bible_verses kbv 
        LEFT OUTER JOIN user_verse_status uvs 
        ON kbv.book = uvs.book AND kbv.chapter = uvs.chapter AND kbv.verseNum = uvs.verseNum AND uvs.ownerEmail = :ownerEmail 
        WHERE kbv.book = :bookName AND kbv.chapter = :chapterNum 
        GROUP BY kbv.book, kbv.chapter, kbv.verseNum 
        ORDER BY kbv.verseNum ASC
    """)
    fun getKjvVersesByChapterForUserFlow(bookName: String, chapterNum: Int, ownerEmail: String): Flow<List<BibleVerse>>
}

data class BookWithTestament(
    val book: String,
    val testament: String
)

// ==========================================
// 3. CHURCH ACTIVITIES ENTITY & DAO
// ==========================================

@Entity(tableName = "activities")
data class ChurchActivity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val date: String,     // YYYY-MM-DD
    val time: String,     // HH:MM
    val location: String,
    val notes: String = "",
    val notificationEnabled: Boolean = true,
    val isRecurring: Boolean = false,
    val recurrenceDay: String = "", // e.g. "Sunday", "Wednesday"
    val ownerEmail: String = "" // Added for multi-account isolation!
)

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY date ASC, time ASC")
    fun getAllActivitiesFlow(): Flow<List<ChurchActivity>>

    @Query("SELECT * FROM activities WHERE ownerEmail = :ownerEmail ORDER BY date ASC, time ASC")
    fun getAllActivitiesForUserFlow(ownerEmail: String): Flow<List<ChurchActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ChurchActivity): Long

    @Update
    suspend fun updateActivity(activity: ChurchActivity)

    @Delete
    suspend fun deleteActivity(activity: ChurchActivity)

    @Query("SELECT COUNT(*) FROM activities")
    suspend fun getActivitiesCount(): Int
}

// ==========================================
// 4. BAPTIST CHURCHES DIRECTORY ENTITY & DAO
// ==========================================

@Entity(tableName = "baptist_churches")
data class BaptistChurch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val province: String,
    val address: String,
    val pastorName: String,
    val contactNumber: String,
    val worshipSchedule: String,
    val isApproved: Boolean = false,
    val submittedBy: String = "Local Database",
    val latitude: Double = 14.5995,
    val longitude: Double = 120.9842,
    val cityMunicipality: String = "",
    val description: String = "",
    val timestamp: Long = 0L,
    val isDeletePending: Boolean = false,
    val replacesChurchId: Int? = null,
    val facebookUrl: String = "" // Added Facebook Link!
)

@Dao
interface ChurchDao {
    @Query("SELECT * FROM baptist_churches WHERE isApproved = 1 ORDER BY timestamp DESC, id DESC")
    fun getApprovedChurchesFlow(): Flow<List<BaptistChurch>>

    @Query("SELECT * FROM baptist_churches ORDER BY isApproved ASC, timestamp DESC, id DESC")
    fun getAllChurchesFlow(): Flow<List<BaptistChurch>> // Admin view

    @Query("SELECT * FROM baptist_churches WHERE id = :id LIMIT 1")
    suspend fun getChurchById(id: Int): BaptistChurch?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChurch(church: BaptistChurch)

    @Update
    suspend fun updateChurch(church: BaptistChurch)

    @Delete
    suspend fun deleteChurch(church: BaptistChurch)

    @Query("SELECT COUNT(*) FROM baptist_churches")
    suspend fun getChurchesCount(): Int
}
