package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.jaidev.seeaplayer.LinkTubeActivity.Companion.myPager
import com.jaidev.seeaplayer.browseFregment.BrowseFragment

class MydbHandler(
    context: Context?,
    name: String?,
    factory: SQLiteDatabase.CursorFactory?,
    version: Int
) : SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "sites.db"
        private const val TABLE_SITES = "sites"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "url"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val query = " CREATE TABLE $TABLE_SITES (" +
                " $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                " $COLUMN_NAME TEXT" +
                ")"
        db.execSQL(query)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SITES")
        onCreate(db)
    }


    fun deleteUrl(urlName: String) {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_SITES WHERE $COLUMN_NAME='$urlName'")
        db.close()
    }

    @SuppressLint("Range")
    fun databaseToString(): List<String> {
        val db = writableDatabase
        val query = "SELECT * FROM $TABLE_SITES"
        val dbstring = mutableListOf<String>()
        val cursor: Cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                val urlString = cursor.getString(cursor.getColumnIndex(COLUMN_NAME))
                if (urlString != null) {
                    dbstring.add(urlString)
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        return dbstring
    }


    fun addUrl(website: String?) {
        var frag: BrowseFragment? = null
        try {
            frag = LinkTubeActivity.tabsList[myPager.currentItem].fragment as BrowseFragment
        } catch (_: Exception) {
        }
        val values = ContentValues()
        values.put(COLUMN_NAME,frag?.binding?.webView?.url)
        val db = writableDatabase
        db.insert(TABLE_SITES, null, values)
        db.close()
    }
}
