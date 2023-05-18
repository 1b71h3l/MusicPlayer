package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class Database extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Favorites.db";
    private static final int DATABASE_VERSION = 1;

    // Table name and column names
    private static final String TABLE_FAVORITES = "favorites";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_TITLE = "title";


    // Create table query
    private static final String CREATE_TABLE_FAVORITES = "CREATE TABLE " + TABLE_FAVORITES + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_TITLE + " TEXT UNIQUE "
            + ")";

    public Database (Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the favorites table
        db.execSQL(CREATE_TABLE_FAVORITES);
    }

    public long createFavorite (String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        long id = db.insert(TABLE_FAVORITES, null, values);
        return id;
    }
    @SuppressLint("Range")
    public List<Song> getAllSongs() {
        List<Song> songs = new ArrayList<Song>();
        String selectQuery = "SELECT * FROM " + TABLE_FAVORITES;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                Song song = new Song();
                song.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
                songs.add(song);
            } while (cursor.moveToNext());
        }
        cursor.close(); // Close the cursor
        return songs;}

    public void deleteFavorite(String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FAVORITES, COLUMN_TITLE + " = ?", new String[]{title});
        db.close(); // Close the database connection
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if it exists
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
        // Recreate the table
        onCreate(db);
    }
}
