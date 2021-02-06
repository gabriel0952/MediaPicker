package com.example.mediaPicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

class SelectMedia {
    public enum FileType {
        IMAGE, VIDEO, FILE
    }

    String selectedTitle;
    FileType selectedFileType;
    String selectedFilePath;
    String selectedFileMIME;
    String selectedFileCreateTime;
    Integer selectedVideoDuration;


    public SelectMedia(String selectedTitle, FileType selectedFileType, String selectedFilePath, String selectedFileMIME, String selectedFileCreateTime, Integer selectedVideoDuration) {
        this.selectedTitle = selectedTitle;
        this.selectedFileType = selectedFileType;
        this.selectedFilePath = selectedFilePath;
        this.selectedFileMIME = selectedFileMIME;
        this.selectedFileCreateTime = selectedFileCreateTime;
        this.selectedVideoDuration = selectedVideoDuration;
    }
}

public class MainActivity extends AppCompatActivity implements MediaAdapter.ItemClickListener {
    MediaAdapter adapter;
    ArrayList<SelectMedia> inputMediaArrayList = new ArrayList<>();
    Button reloadButton, clearButton, openFileSelecterButton;

    ArrayList<String> inputData = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionCheck();

        reloadButton = (Button) findViewById(R.id.reloadButton);
        reloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.cancelTask();
                inputMediaArrayList.clear();
                adapter.notifyDataSetChanged();
                setUpGridLayout();
                getLocalMediaUri();
            }
        });

        clearButton = (Button) findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.cancelTask();
                inputMediaArrayList.clear();
                adapter.notifyDataSetChanged();
            }
        });

        openFileSelecterButton = (Button) findViewById(R.id.open_file_selecter);
        openFileSelecterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, FileSelecterActivity.class);
                startActivity(intent);
//                MainActivity.this.finish();
            }
        });

        setUpGridLayout();
        getLocalMediaUri();
    }

    private void permissionCheck() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
    }

    private void getLocalMediaUri() {
        Uri queryUri = MediaStore.Files.getContentUri("external");

        String[] projection = {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                "duration",
                MediaStore.Files.FileColumns.TITLE
        };

        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

        String order = MediaStore.Files.FileColumns.DATE_ADDED + " DESC";

        CursorLoader cursorLoader = new CursorLoader(this, queryUri, projection, selection, null, order);
        Cursor cursor = cursorLoader.loadInBackground();

        // MEDIA_TYPE: IMAGE = 1; VIDEO = 3;
        int indexFileTitle = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.TITLE);
        int indexFilePath = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
        int indexFileTpye = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE);
        int indexFileMime = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);
        int indexVideoDuration = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION);

        while (cursor.moveToNext()) {
            inputData.add(cursor.getString(indexFilePath));

            String title = cursor.getString(indexFileTitle);
            String uri = cursor.getString(indexFilePath);
            String type = cursor.getString(indexFileTpye);
            String mime = cursor.getString(indexFileMime);
            Integer duration = cursor.getInt(indexVideoDuration);
            SelectMedia file;
            if (type.equals("1")) {
                file = new SelectMedia(title, SelectMedia.FileType.IMAGE, uri, mime, null, null);
            } else {
                file = new SelectMedia(title, SelectMedia.FileType.VIDEO, uri, mime, null, duration);
            }
            inputMediaArrayList.add(file);
        }
        cursor.close();
    }

    private void setUpGridLayout() {
        RecyclerView recyclerView = findViewById(R.id.mediarecycleview);
        int numberOfColumns = 3;
        GridLayoutManager manager = new GridLayoutManager(this, numberOfColumns);
        recyclerView.setLayoutManager(manager);
        adapter = new MediaAdapter(this, inputMediaArrayList);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(View view, int position) {
        System.out.println(position);
    }
}
