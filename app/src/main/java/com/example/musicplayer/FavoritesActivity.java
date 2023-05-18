package com.example.musicplayer;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class FavoritesActivity extends AppCompatActivity implements FavoritesAdapter.OnItemClickListener, FavoritesAdapter.OnItemLongClickListener {

    private RecyclerView recyclerViewFavorites;
    private Database database;
    private FavoritesAdapter favoritesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        Toast.makeText(FavoritesActivity.this, "Click on a song to read it \n LongClick to delete it", Toast.LENGTH_LONG).show();

        recyclerViewFavorites = findViewById(R.id.recyclerViewFavorites);
        recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(this));

        database = new Database(this);
        favoritesAdapter = new FavoritesAdapter();
        favoritesAdapter.setOnItemClickListener(this);
        favoritesAdapter.setOnItemLongClickListener(this);

        recyclerViewFavorites.setAdapter(favoritesAdapter);
        loadFavorites();
    }

    private void loadFavorites() {
        List<Song> favoritesList = database.getAllSongs();
        favoritesAdapter.setSongs(favoritesList);
    }

    private void deleteSong(Song song) {
        database.deleteFavorite((String) song.getTitle());
        loadFavorites();
    }
    @Override
    public void onItemClick(int position) {
        // Handle item click here
        // For example, start the MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onItemLongClick(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(FavoritesActivity.this);
        builder.setTitle("Delete From favorites");
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage("Are you sure you want to delete this song from your favorites?");
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Song song = favoritesAdapter.getSongs().get(position);
                deleteSong(song);
            }
        });
        builder.show();
    }

}