package com.example.mediaPicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> {
    private ArrayList<SelectMedia> mData = new ArrayList<SelectMedia>();
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private static final int MAX_ENTRIES = 30;
    private List<String> mediaSelectedList = new ArrayList<>();
    private List<Integer> positionSelectedList = new ArrayList<>();
    private List<BitmapWorkerTask> taskList = new ArrayList<>();

    private LruCache<String, Bitmap> memoryCache;

    public MediaAdapter(Context context, ArrayList<SelectMedia> mData) {
        this.mData = mData;
        this.mInflater = LayoutInflater.from(context);

        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // 使用最大可用記憶體的1/8作为緩存的大小。
        int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // 重寫此方法來衡量每張圖片的大小，默認返回圖片數量
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.media_recycle_item, parent, false);
        Log.i("adapter", "onCreateViewHolder");
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
        Log.i("adapter", "onBindViewHolder with payloads" + position);
        if (!payloads.isEmpty()) {
            setUpSelectedNum(holder, position);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MediaAdapter.ViewHolder holder, int position) {
        Log.i("adapter", "onBindViewHolder " + position);
        Bitmap imageBitmap = null;
        SelectMedia cellData = mData.get(position);

        setUpVideoLayout(holder, position, cellData);
        loadBitmap(position, holder.imageView);

        setUpSelectedNum(holder, position);
    }

    private void setUpSelectedNum(@NonNull ViewHolder holder, int position) {
        holder.mediaSelectedNum.setText("");
        holder.mediaSelectedNum.setBackgroundResource(R.drawable.media_unselected_num_background);
        if (mediaSelectedList.contains(mData.get(position).selectedFilePath)) {
            String selectedNum = String.valueOf(mediaSelectedList.indexOf(mData.get(position).selectedFilePath) + 1);

            holder.mediaSelectedNum.setText(selectedNum);
            holder.mediaSelectedNum.setBackgroundResource(R.drawable.media_selected_num_background);
        }
    }

    // 根據多媒體種類進行 影片圖示/時間顯示 設定
    private void setUpVideoLayout(@NonNull ViewHolder holder, int position, SelectMedia cellData) {
        if (cellData.selectedFileType != SelectMedia.FileType.VIDEO){
            holder.videoIcon.setVisibility(View.INVISIBLE);
            holder.durationTimeText.setVisibility(View.INVISIBLE);
        } else {
            int inputTime = mData.get(position).selectedVideoDuration / 1000;
            String fmtTime = "";
            fmtTime = formatTime(inputTime, fmtTime);

            holder.videoIcon.setVisibility(View.VISIBLE);
            holder.durationTimeText.setVisibility(View.VISIBLE);
            holder.durationTimeText.setText(fmtTime);
        }
    }

    // format time
    private String formatTime(int inputTime, String fmtTime) {
        int hr = inputTime / 3600;
        int min = (inputTime % 3600) / 60;
        int sec = inputTime % 60;

        if (hr > 0) {
            fmtTime += hr + ":";
        }

        if (min < 10) {
            if (fmtTime != "") { fmtTime += "0" + min + ":"; }
            else if (min == 0) { fmtTime += "00:"; }
            else fmtTime += min + ":";
        } else {
            fmtTime += min + ":";
        }

        if (sec < 10) {
            if (fmtTime != "") { fmtTime += "0" + sec; }
        } else {
            fmtTime += sec;
        }
        return fmtTime;
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView imageView;
        ImageView videoIcon;
        TextView durationTimeText;
        TextView mediaSelectedNum;
        Boolean isSelected = false;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.mediaimage);
            videoIcon = (ImageView) itemView.findViewById(R.id.videoicon);
            durationTimeText = (TextView) itemView.findViewById(R.id.durationtime);
            mediaSelectedNum = (TextView) itemView.findViewById(R.id.media_selected_num);

            setViewWidthByHeight(imageView);
            imageView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mediaSelectedList.contains(mData.get(getAdapterPosition()).selectedFilePath)) {
                mediaSelectedList.remove(mData.get(getAdapterPosition()).selectedFilePath);
                positionSelectedList.remove(positionSelectedList.indexOf(getAdapterPosition()));
            } else {
                mediaSelectedList.add(mData.get(getAdapterPosition()).selectedFilePath);
                positionSelectedList.add(getAdapterPosition());
            }

            notifyItemChanged(getAdapterPosition(), "setSelectionNumber");
            for (int position: positionSelectedList) {
                if (position != getAdapterPosition()) {
                    notifyItemChanged(position, "update");

                }
            }

            if (mClickListener != null) {
                mClickListener.onItemClick(v, getAdapterPosition());
            }
        }
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    private void setViewWidthByHeight(ImageView mediaThumbnail) {
        final ImageView mThumView = mediaThumbnail;
        final ViewTreeObserver observer = mThumView.getViewTreeObserver();

        final ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                android.view.ViewGroup.LayoutParams lp = mThumView.getLayoutParams();
                lp.height = mThumView.getMeasuredWidth();
                mThumView.setLayoutParams(lp);

                final ViewTreeObserver vto1 = mThumView.getViewTreeObserver();
                vto1.removeOnPreDrawListener(this);

                return true;
            }
        };
        observer.addOnPreDrawListener(preDrawListener);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // 圖像的原始高度和寬度
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // 計算最大的inSampleSize值，該值為2的冪次方並保持高、寬兩者始終大於目標高、寬
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromUri(Context context, String uri, int reqWidth, int reqHeight) {
        Bitmap bitmap = null;

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(uri, options);

        // 計算 inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        bitmap = BitmapFactory.decodeFile(uri, options);

        return bitmap;
    }

    public void loadBitmap(int resId, ImageView imageView) {
        final String imageKey = String.valueOf(resId);
        final Bitmap bitmap = getBitmapFromMemCache(imageKey);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(R.drawable.image_placeholder);
            BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            taskList.add(task);
            task.execute(resId);
        }
    }

    public void cancelTask() {
        for (BitmapWorkerTask task: taskList) {
            task.cancel(false);
        }
    }

    class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {
        ImageView imageView;
        Integer position;

        public BitmapWorkerTask(ImageView imageView) {
            this.imageView = imageView;
        }

        // 在背景製作縮圖
        @Override
        protected Bitmap doInBackground(Integer... params) {

            final String uri = mData.get(params[0]).selectedFilePath;
            final SelectMedia.FileType type = mData.get(params[0]).selectedFileType;
            position = params[0];
            final Bitmap bitmap = thumbnailSource(type, uri);

            addBitmapToMemoryCache(String.valueOf(params[0]), bitmap);
            taskList.remove(this);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
//            if (bitmap != null) {
//                this.imageView.setImageBitmap(bitmap);
//            }
            taskList.remove(this);
            notifyItemChanged(position, "update data");
        }

        private Bitmap thumbnailSource(SelectMedia.FileType type, String uri) {
            Bitmap bitmap = null;

            if (type == SelectMedia.FileType.VIDEO) {
                bitmap = ThumbnailUtils.createVideoThumbnail(uri, MediaStore.Video.Thumbnails.MINI_KIND);
            } else {
                bitmap = decodeSampledBitmapFromUri(mInflater.getContext(), uri, 100, 100);
            }

            return bitmap;
        }

    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            memoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return memoryCache.get(key);
    }
}
