/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.launch

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(val database: SleepDatabaseDao, application: Application) : AndroidViewModel(application) {

    private var currentNight = MutableLiveData<SleepNight?>()
    private val allNights = database.getAllSleepRecord()

    //transform nights into string
    val allNightsString = Transformations.map(allNights) { night ->
        formatNights(night, application.resources)
    }

    init {
        initializeCurrentNight()
    }

    private fun initializeCurrentNight(){ viewModelScope.launch { currentNight.value = getCurrentNightFromDatabase() } }

    private suspend fun getCurrentNightFromDatabase(): SleepNight? {

        var night = database.getLatestSleepRecord()

        if (night?.endTime != night?.startTime){ night = null }

        return night

    }

    fun onStartTracking() {

        viewModelScope.launch {
            val newSleep = SleepNight()
            insertSleep(newSleep)
            currentNight.value = getCurrentNightFromDatabase()
        }

    }

    fun onStopTracking() {
        viewModelScope.launch {
            val oldNight = currentNight.value ?: return@launch
            oldNight.endTime = System.currentTimeMillis()
            updateSleep(oldNight)
        }
    }

    fun onClear() {
        viewModelScope.launch {
            clearSleep()
            currentNight.value = null
        }
    }

    private suspend fun insertSleep(sleep: SleepNight){

        database.insertSleep(sleep)

    }

    private suspend fun updateSleep(night: SleepNight) {
        database.updateSleep(night)
    }

    suspend fun clearSleep() {
        database.clearSleepRecord()
    }

}

