package com.example.candroid4

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface NoteDao {
    @Upsert
    fun upsert(n:Note)
    @Delete
    fun delete(n:Note)

    @Query("SELECT * FROM note")
    fun getNotes():LiveData<List<Note>>
}