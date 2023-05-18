package com.example.musicplayer;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.Settings;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 1;
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE = 2;

    private EditText urlEditText;
    private Button downloadButton;
    private TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        urlEditText = findViewById(R.id.urlEditText);
        downloadButton = findViewById(R.id.downloadButton);
        resultTextView = findViewById(R.id.resultTextView);

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(resultTextView.getVisibility() == View.VISIBLE){
                    resultTextView.setVisibility(View.GONE);
                }
                String url =  urlEditText.getText().toString();
                checkPermissionAndDownload(url);
            }
        });
    }

    private void checkPermissionAndDownload(String url) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 and above, request MANAGE_EXTERNAL_STORAGE permission
            if (Environment.isExternalStorageManager()) {
                downloadFile(url);
            } else {
                requestManageExternalStoragePermission();
            }
        } else {
            // For Android 10 and below, request WRITE_EXTERNAL_STORAGE permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(DownloadActivity.this,url,Toast.LENGTH_LONG).show();
                downloadFile(url);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            }
        }
    }

    private void requestManageExternalStoragePermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_EXTERNAL_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                String url = urlEditText.getText().toString();
                downloadFile(url);
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    private String downloadUrl; // Global variable to store the URL

    private void downloadFile(String url) {
        downloadUrl = url; // Assign the URL to the global variable
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        String title = URLUtil.guessFileName(url, null, null);
        request.setTitle(title);
        request.setDescription("Downloading File please wait .....");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title);
        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);
        Toast.makeText(DownloadActivity.this, "Downloading Started", Toast.LENGTH_LONG).show();
    }

    private BroadcastReceiver onComplete = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId != -1) {
                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = downloadManager.query(query);
                if (cursor.moveToFirst()) {
                    int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    if (cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                        String url = downloadUrl; // Retrieve the global URL variable
                        @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
                        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + title;
                        onPostExecute(filePath);
                    }
                }
                cursor.close();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(onComplete);
    }

    protected void onPostExecute(String filePath) {
        if (filePath != null) {
            resultTextView.setText("Downloaded in : " + filePath + " (Click here to read )");
            resultTextView.setVisibility(View.VISIBLE);
            resultTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DownloadActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            });
        } else {
            Toast.makeText(DownloadActivity.this, "Failed to download file", Toast.LENGTH_SHORT).show();
        }
    }


    private String getFileNameFromUrl(String url) {
        String[] segments = url.split("/");
        Toast.makeText(DownloadActivity.this, segments[segments.length - 1], Toast.LENGTH_LONG).show();
        return segments[segments.length - 1];
    }

    private void showPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Denied")
                .setMessage("Without the permission, the app cannot download files to external storage.")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String url = urlEditText.getText().toString();
                downloadFile(url);
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

}