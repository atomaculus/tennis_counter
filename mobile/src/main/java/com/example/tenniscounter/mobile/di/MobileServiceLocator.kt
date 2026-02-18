package com.example.tenniscounter.mobile.di

import android.content.Context
import com.example.tenniscounter.mobile.data.MatchRepository
import com.example.tenniscounter.mobile.data.local.AppDatabase

object MobileServiceLocator {
    @Volatile
    private var repository: MatchRepository? = null

    fun matchRepository(context: Context): MatchRepository {
        return repository ?: synchronized(this) {
            repository ?: MatchRepository(
                AppDatabase.getInstance(context).matchDao()
            ).also { repository = it }
        }
    }
}
