package com.example.mediaPicker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class FileListItemAdapter extends ArrayAdapter implements Filterable {
    List<SelectMedia> item;
    List<String> originalitem;

    private ItemClickListener mClickListener;

    private static final int MAX_ENTRIES = 10;
    private List<String> fileSelectedList = new ArrayList<>();
    private List<Integer> positionSelectedList = new ArrayList<>();

    private LayoutInflater mLayout;

    public FileListItemAdapter(@NonNull Context context, int resource, List<SelectMedia> item) {
        super(context, resource);
        this.item = item;
        this.mLayout = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);;
    }

    @Override
    public int getCount() {
        return item.size();
    }

    @Override
    public Object getItem(int position) {
        return item.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void refresh(ArrayList<String[]> list) {
        notifyDataSetChanged();
    }

    public final class ViewHolder {
        TextView fileName;
        TextView fileCreateTime;
        TextView fileSelectedMark;
        ImageView fileTypeIcon;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mLayout.inflate(R.layout.file_list_item,parent,false);
            holder = new ViewHolder();

            holder.fileName = (TextView) convertView.findViewById(R.id.file_name_textview);
            holder.fileCreateTime = (TextView) convertView.findViewById(R.id.file_create_time_textview);
            holder.fileSelectedMark = (TextView) convertView.findViewById(R.id.file_selected_mark_view);
            holder.fileTypeIcon = (ImageView) convertView.findViewById(R.id.file_type_imageview);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.fileName.setText(item.get(position).selectedTitle);
        holder.fileCreateTime.setText(item.get(position).selectedFileCreateTime);

        if (item.get(position).selectedFileMIME.equals("application/pdf")) {
            holder.fileTypeIcon.setImageResource(R.drawable.files);
        } else if (item.get(position).selectedFileMIME.equals("image/jpeg")) {
            holder.fileTypeIcon.setImageResource(R.drawable.img);
        } else {
            holder.fileTypeIcon.setImageResource(R.drawable.mp4);
        }

        if (positionSelectedList.contains(position)) {
            holder.fileSelectedMark.setBackgroundResource(R.drawable.file_selecter_mark_background);
        } else {
            holder.fileSelectedMark.setBackgroundResource(R.drawable.file_selecter_unmark_background);
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!fileSelectedList.contains(item.get(position).selectedFilePath)) {
                    if (fileSelectedList.size() > MAX_ENTRIES-1) {
                        return;
                    }
                    fileSelectedList.add(item.get(position).selectedFilePath);
                    positionSelectedList.add(position);
                    holder.fileSelectedMark.setBackgroundResource(R.drawable.file_selecter_mark_background);
                } else {
                    fileSelectedList.remove(item.get(position).selectedFilePath);
                    positionSelectedList.remove(positionSelectedList.indexOf(position));
                    holder.fileSelectedMark.setBackgroundResource(R.drawable.file_selecter_unmark_background);
                }

                if (mClickListener != null) {
                    mClickListener.onItemClick(v, position);
                }
            }
        });

        return convertView;
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                constraint = constraint.toString();
                FilterResults result = new FilterResults();
                if(originalitem == null){
                    synchronized (this){
//                        originalitem = new ArrayList<String>(item.get());
                        // 若originalitem 沒有資料，會複製一份item的過來.
                    }
                }
                if(constraint != null && constraint.toString().length()>0){
                    ArrayList<String> filteredItem = new ArrayList<String>();
                    for(int i=0;i<originalitem.size();i++){
                        String title = originalitem.get(i).toString();
                        if(title.contains(constraint)){
                            filteredItem.add(title);
                        }
                    }
                    result.count = filteredItem.size();
                    result.values = filteredItem;
                }else{
                    synchronized (this){
                        ArrayList<String> list = new ArrayList<String>(originalitem);
                        result.values = list;
                        result.count = list.size();

                    }
                }

                return result;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
//                item = (ArrayList<String>)results.values;
                if(results.count>0){
                    notifyDataSetChanged();
                }else{
                    notifyDataSetInvalidated();
                }
            }
        };

        return filter;
    }

    public int getSelectedCount() {
        return fileSelectedList.size();
    }

    public List<String> getFileSelectedList() {
        return fileSelectedList;
    }
}
