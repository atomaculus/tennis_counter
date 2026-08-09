package com.example.tenniscounter.mobile.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MatchEntity::class, TrainingSessionEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao
    abstract fun trainingSessionDao(): TrainingSessionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tennis_counter_mobile.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .addMigrations(MIGRATION_5_6)
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matches ADD COLUMN setScoresText TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS matches_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        finalScoreText TEXT NOT NULL,
                        setScoresText TEXT,
                        photoUri TEXT,
                        idempotencyKey TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO matches_new (id, createdAt, durationSeconds, finalScoreText, setScoresText, photoUri, idempotencyKey)
                    SELECT id, createdAt, durationSeconds, finalScoreText, setScoresText, photoUri, 'legacy-' || id
                    FROM matches
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_matches_idempotencyKey ON matches_new(idempotencyKey)"
                )
                db.execSQL("DROP TABLE matches")
                db.execSQL("ALTER TABLE matches_new RENAME TO matches")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matches ADD COLUMN playerAName TEXT")
                db.execSQL("ALTER TABLE matches ADD COLUMN playerBName TEXT")
                db.execSQL("ALTER TABLE matches ADD COLUMN caloriesKcal REAL")
                db.execSQL("ALTER TABLE matches ADD COLUMN avgHeartRateBpm INTEGER")
                db.execSQL("ALTER TABLE matches ADD COLUMN maxHeartRateBpm INTEGER")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matches ADD COLUMN pointEventsJson TEXT")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS training_sessions (
                        id TEXT PRIMARY KEY NOT NULL,
                        modality TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        targetPoints INTEGER NOT NULL,
                        finalCount INTEGER NOT NULL,
                        bestStreak INTEGER NOT NULL,
                        attempts INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
