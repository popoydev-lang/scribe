package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Note::class, BibleVerse::class, KjvBibleVerse::class, ChurchActivity::class, BaptistChurch::class, UserVerseStatus::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun bibleDao(): BibleDao
    abstract fun activityDao(): ActivityDao
    abstract fun churchDao(): ChurchDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "baptist_notes_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateBible(database.bibleDao())
                    populateChurches(database.churchDao())
                    populateActivities(database.activityDao())
                }
            }
        }

        private suspend fun populateBible(bibleDao: BibleDao) {
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

        private suspend fun populateChurches(churchDao: ChurchDao) {
            val classicChurches = listOf(
                BaptistChurch(
                    name = "Faithway Fundamental Baptist Church",
                    province = "Metro Manila",
                    address = "Sande St, Tondo, Manila, Metro Manila",
                    pastorName = "Dr. Robert Rodriguez",
                    contactNumber = "02-8254-1234",
                    worshipSchedule = "Sunday Worship: 9:00 AM, 11:00 AM, 5:00 PM\nWednesday Prayer: 7:00 PM",
                    isApproved = true,
                    submittedBy = "Local Database",
                    cityMunicipality = "Manila",
                    description = "A thriving Bible-believing local church in the heart of Tondo, preaching the doctrines of grace and serving the community since 1982.",
                    timestamp = System.currentTimeMillis(),
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
                    submittedBy = "Local Database",
                    cityMunicipality = "Cebu City",
                    description = "Witnessing independent Baptist church dedicated to preaching the Bible, making disciples, and ministering to Cebu families.",
                    timestamp = System.currentTimeMillis(),
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
                    submittedBy = "Local Database",
                    cityMunicipality = "Davao City",
                    description = "Growing fellowship focused on sound biblical preaching, Christ-centered family service, and localized Bible studies.",
                    timestamp = System.currentTimeMillis(),
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
                    submittedBy = "Local Database",
                    cityMunicipality = "Baguio",
                    description = "A welcoming congregation nestled in sweet Baguio pine trees, holding fast to conservative Bible worship and evangelism.",
                    timestamp = System.currentTimeMillis(),
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
                    submittedBy = "Local Database",
                    cityMunicipality = "Iloilo City",
                    description = "Fundamental baptist lighthouse propagating scripture truth across Panay island with missionary zeal and local ministries.",
                    timestamp = System.currentTimeMillis(),
                    facebookUrl = ""
                ),
                BaptistChurch(
                    name = "Grace Baptist Church Subic",
                    province = "Zambales",
                    address = "Rizal Highway, Subic Bay Freeport Zone",
                    pastorName = "Pastor Gideon Mercer",
                    contactNumber = "047-252-8888",
                    worshipSchedule = "Sunday Worship: 10:00 AM, 6:00 PM",
                    isApproved = false,
                    submittedBy = "guest_scribe_99@gmail.com",
                    cityMunicipality = "Subic",
                    description = "Caring fellowship focusing on God's word, prayer meetings, and sound evangelism around the Freeport.",
                    timestamp = System.currentTimeMillis(),
                    facebookUrl = "https://facebook.com/gracesubic"
                ),
                BaptistChurch(
                    name = "Hope Baptist Mission Davao",
                    province = "Davao del Sur",
                    address = "Kabacan Road, Ecoland, Davao City",
                    pastorName = "Pastor Silas Vance",
                    contactNumber = "0917-888-2233",
                    worshipSchedule = "Sunday Worship: 1:30 PM",
                    isApproved = false,
                    submittedBy = "mindanao_evangelist@gmail.com",
                    cityMunicipality = "Davao City",
                    description = "Local mission plant actively sharing hope through reformed Bible study, tract distribution, and morning worship.",
                    timestamp = System.currentTimeMillis(),
                    facebookUrl = "https://facebook.com/hopebaptistdavao"
                )
            )
            for (church in classicChurches) {
                churchDao.insertChurch(church)
            }
        }

        private suspend fun populateActivities(activityDao: ActivityDao) {
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
}
