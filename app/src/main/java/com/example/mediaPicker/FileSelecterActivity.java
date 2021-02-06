package com.example.mediaPicker;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class FileSelecterActivity extends AppCompatActivity implements FileListItemAdapter.ItemClickListener {
    ArrayList<SelectMedia> inputMediaArrayList = new ArrayList<>();

    private static final int READ_REQUEST_CODE = 42;
    ListView listView;  //這是listview啊!
    List<String> my_list = new ArrayList<>(); //這是測試用的資料容器
    FileListItemAdapter fileListItemAdapter;
    private SearchView search;
    private Filter filter;
    private String searchWord="";
    private TextView fileSelectedNum;
    private TextView sendTextView;
    private TextView cancelTextView;

    ArrayList<String> inputData = new ArrayList<String>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_list_view);
        fileSelectedNum = (TextView) findViewById(R.id.selected_number_text_view);
        fileSelectedNum.setVisibility(View.INVISIBLE);
        sendTextView = (TextView) findViewById(R.id.send_button);
        cancelTextView = (TextView) findViewById(R.id.cancel_button);
        listView = (ListView)findViewById(R.id.file_listview);
        fileListItemAdapter = new FileListItemAdapter(getApplicationContext(), R.layout.file_list_item, inputMediaArrayList);
        fileListItemAdapter.setClickListener(this);
        listView.setAdapter(fileListItemAdapter);
        // 給listview 設置過濾器
        listView.setTextFilterEnabled(true);

        sendTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println(fileListItemAdapter.getFileSelectedList());
            }
        });

        cancelTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        search = (SearchView) findViewById(R.id.searchview);
        search.setIconifiedByDefault(false);// 關閉icon切換
        search.setFocusable(false); // 不要進畫面就跳出輸入鍵盤
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String queryWord) {
                Log.d("Ray", "onQueryTextSubmit");
                searchWord = queryWord;
                search.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String queryWord) {
                searchWord = queryWord;
                if (queryWord == null || queryWord.trim().length() < 1) {
                    // 清除listView的篩選結果
                    searchWord = "";
                    if (filter != null)
                        filter.filter(null);
                } else {
                    // 使用使用者輸入的資料對listView項目進行篩選
                    if (filter != null)
                        filter.filter(queryWord);
                }
                return true;
            }
        });

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search.setIconified(false);
            }
        });


        getLocalFileUri();
    }

    private void initView() {

    }

    @Override
    public void onItemClick(View view, int position) {
        if (fileListItemAdapter.getSelectedCount() > 0) {
            fileSelectedNum.setVisibility(View.VISIBLE);
            fileSelectedNum.setText(String.valueOf(fileListItemAdapter.getSelectedCount()));
        } else {
            fileSelectedNum.setVisibility(View.INVISIBLE);
            fileSelectedNum.setText(String.valueOf(fileListItemAdapter.getSelectedCount()));
        }
        System.out.println(position);
    }

    private void setSearchFunction() {
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                fileListItemAdapter.getFilter().filter(newText);

                return true;
            }
        });
    }

    private void getLocalFileUri() {
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

        String selection = MediaStore.Files.FileColumns.MIME_TYPE
                + "!=" + "'null'";

        System.out.println(selection);
        String order = MediaStore.Files.FileColumns.DATE_ADDED + " DESC";

        CursorLoader cursorLoader = new CursorLoader(this, queryUri, projection, selection, null, order);
        Cursor cursor = cursorLoader.loadInBackground();

        // MEDIA_TYPE: IMAGE = 1; VIDEO = 3;

        int indexFileTitle = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.TITLE);
        int indexFilePath = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
        int indexFileTpye = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE);
        int indexFileMime = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);
        int indexVideoDuration = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION);
        int indexFileCreateTime = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED);

        while (cursor.moveToNext()) {
            inputData.add(cursor.getString(indexFilePath));

            String title = cursor.getString(indexFileTitle);
            String uri = cursor.getString(indexFilePath);
            String mime = cursor.getString(indexFileMime);
            Integer duration = cursor.getInt(indexVideoDuration);
            Integer createtime = cursor.getInt(indexFileCreateTime);
            if (!uri.contains("/.thumbnails")) {
                SelectMedia file;
                String currentDate = new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date(createtime * 1000L));
                file = new SelectMedia(title, SelectMedia.FileType.FILE, uri, mime, currentDate, null);
                inputMediaArrayList.add(file);
            }
        }
        cursor.close();
    }
}
