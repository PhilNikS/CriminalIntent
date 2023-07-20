package com.hfad.criminalintent.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

private const val DATABASE_NAME="crime-database"
class CrimeRepository private constructor(
    context: Context,
    private val coroutineScope: CoroutineScope = GlobalScope) {

    private val database:CrimeDatabase = Room.databaseBuilder(
        context.applicationContext,
        CrimeDatabase::class.java,
        DATABASE_NAME)
        .addMigrations(migration_1_2, migration_2_3)
        .build()

    companion object{
        private var INSTANCE:CrimeRepository?=null

        fun initialize(context: Context){
            if(INSTANCE == null){
                INSTANCE = CrimeRepository(context)
            }
        }
        fun get():CrimeRepository{
            return INSTANCE?:throw IllegalStateException("Repository must be initialized")
        }
    }

    fun getCrimes(): Flow<List<Crime>> =database.crimeDao().getCrimes()

    suspend fun getCrime(id: UUID): Crime = database.crimeDao().getCrime(id)

    fun updateCrimeDB(crime: Crime){
        coroutineScope.launch {
            database.crimeDao().updateCrime(crime)
        }
    }

    suspend fun addCrime(crime: Crime){
        database.crimeDao().addCrime(crime)
    }

    suspend fun deleteCrime(crime: Crime){

        database.crimeDao().deleteCrime(crime)
        Log.d("Crime Repository deleting","Works")
    }



}