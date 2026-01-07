package com.example.candroid4

import android.provider.ContactsContract.CommonDataKinds.Note
import androidx.room.Database

@Database(entities = [Note::class], version = 1, exportSchema = false)
class RoomDPHelper {
}