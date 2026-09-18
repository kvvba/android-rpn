package com.jakub.rpncalculator.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jakub.rpncalculator.interfaces.CalculatorDao
import com.jakub.rpncalculator.models.History

@Database(entities = [History::class], version = 1)
abstract class CalculatorDatabase : RoomDatabase() {

    abstract fun CalculatorDao(): CalculatorDao

    companion object {
        private var db: CalculatorDatabase? = null

        fun getInstance(context: Context): CalculatorDatabase {
            if (db == null) {
                synchronized(CalculatorDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(
                            context.applicationContext,
                            CalculatorDatabase::class.java,
                            "calculator.db"
                        )
                            .build()
                        db!!.openHelper.setWriteAheadLoggingEnabled(true)
                    }
                }
            }
            return db!!
        }

        fun destroyInstance() {
            db = null
        }
    }
}
