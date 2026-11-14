package com.example.tenniscounter.mobile.di

import android.content.Context
import com.example.tenniscounter.mobile.data.MatchRepository
import com.example.tenniscounter.mobile.data.local.AppDatabase
import com.example.tenniscounter.mobile.data.local.MatchDao
import com.example.tenniscounter.mobile.data.local.TrainingSessionDao
import com.example.tenniscounter.mobile.garmin.GarminConnectivityManager
import com.example.tenniscounter.mobile.garmin.GarminMatchConfigSender

/**
 * Simple service locator for the mobile module.
 * All singletons are lazily initialized and thread-safe.
 */
object MobileServiceLocator {
    @Volatile private var db: AppDatabase? = null
    @Volatile private var repository: MatchRepository? = null
    @Volatile private var garminManager: GarminConnectivityManager? = null
    @Volatile private var garminConfigSender: GarminMatchConfigSender? = null

    private fun database(context: Context): AppDatabase {
        return db ?: synchronized(this) {
            db ?: AppDatabase.getInstance(context).also { db = it }
        }
    }

    fun matchDao(context: Context): MatchDao = database(context).matchDao()

    fun trainingSessionDao(context: Context): TrainingSessionDao = database(context).trainingSessionDao()

    fun matchRepository(context: Context): MatchRepository {
        return repository ?: synchronized(this) {
            repository ?: MatchRepository(matchDao(context)).also { repository = it }
        }
    }

    fun garminConnectivityManager(context: Context): GarminConnectivityManager {
        return garminManager ?: synchronized(this) {
            garminManager ?: GarminConnectivityManager(context.applicationContext)
                .also { garminManager = it }
        }
    }

    fun garminMatchConfigSender(context: Context): GarminMatchConfigSender {
        return garminConfigSender ?: synchronized(this) {
            garminConfigSender ?: GarminMatchConfigSender(context.applicationContext)
                .also { garminConfigSender = it }
        }
    }
}
