package com.hfad.criminalintent

import android.app.Application
import com.hfad.criminalintent.database.CrimeRepository

class CrimeIntentApplication:Application() {
    override fun onCreate() {
        super.onCreate()
        CrimeRepository.initialize(this)
    }
}