package com.diego.vitther.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PiezaEntity::class, CapsulaEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VittherDatabase : RoomDatabase() {

    abstract fun piezaDao(): PiezaDao

    abstract fun capsulaDao(): CapsulaDao

    companion object {
        private const val NOMBRE = "vitther.db"

        fun crear(context: Context): VittherDatabase =
            Room.databaseBuilder(context, VittherDatabase::class.java, NOMBRE)
                .build()
    }
}
