package com.example.tenniscounter.mobile.di

import android.content.Context
import com.example.tenniscounter.mobile.data.MatchRepository
import com.example.tenniscounter.mobile.data.local.AppDatabase
import com.example.tenniscounter.mobile.data.local.MatchDao

/**
 * Simple service locator for the mobile module.
 * All singletons are lazily initialized and thread-safe.
 */
object MobileServiceLocator {
    @Volatile private var db: AppDatabase? = null
    @Volatile private var repository: MatchRepository? = null

    private fun database(context: Context): AppDatabase {
        return db ?: synchronized(this) {
            db ?: AppDatabase.getInstance(context).also { db = it }
        }
    }

    fun matchDao(context: Context): MatchDao = database(context).matchDao()

    fun matchRepository(context: Context): MatchRepository {
        return repository ?: synchronized(this) {
            repository ?: MatchRepository(matchDao(context)).also { repository = it }
        }
    }
}
