package com.yuyang.wirelesstransmission.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.yuyang.wirelesstransmission.R;
import com.yuyang.wirelesstransmission.transmission.ClientConnection.FileInfo_Sync;
import com.yuyang.wirelesstransmission.transmission.FileInfo;
import com.yuyang.wirelesstransmission.transmission.FileInfo_rec;
import com.yuyang.wirelesstransmission.transmission.FileInfo_send;

import java.util.ArrayList;

/**
 * Created by vista on 2018/3/22.
 */

public class SyncAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<FileInfo_Sync> list;
    private LayoutInflater mLayout;

    public SyncAdapter(ArrayList<FileInfo_Sync> arrayList, Context context){
        list=arrayList;
        mLayout=LayoutInflater.from(context);
    }

    private class SyncItemHolder extends RecyclerView.ViewHolder{
        private ImageView type;
        private TextView name,path,syncType,fileType;
        private CheckBox up,down;
        public SyncItemHolder(View view){
            super(view);
            type = (ImageView) view.findViewById(R.id.sync_icon);
            name = (TextView) view.findViewById(R.id.sync_name);
            path = (TextView)view.findViewById(R.id.sync_path);
            fileType = (TextView)view.findViewById(R.id.sync_type);
            syncType = (TextView)view.findViewById(R.id.sync_conflict);
            up= (CheckBox)view.findViewById(R.id.sync_local);
            down = (CheckBox)view.findViewById(R.id.sync_remote);
        }
        public void band(final FileInfo_Sync info){
            Log.d("SYNCADP", "band: ");
            type.setImageResource(info.getFileInfo().getTypeIconID());
            name.setText(info.getFileInfo().getName());
            syncType.setText(info.getFileInfo().getType().name());
            fileType.setText(info.getFileInfo().getMimeType());
            if(info.getFileInfo().getType()==FileInfo.syncType.newfile) {
                path.setText(((FileInfo_send)info.getFileInfo()).getPath());
            }else{
                path.setText(((FileInfo_rec)info.getFileInfo()).getPath());
            }
            switch (info.getMode()){
                case skip: up.setChecked(false);down.setChecked(false);break;
                case upload:up.setChecked(true);down.setChecked(false);break;
                case download: up.setChecked(false);down.setChecked(true);break;
            }
            up.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked) {
                        down.setChecked(false);
                        info.setMode(FileInfo_Sync.syncMode.upload);
                    }else
                        info.setMode(FileInfo_Sync.syncMode.skip);
                }
            });
            down.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked) {
                        up.setChecked(false);
                        info.setMode(FileInfo_Sync.syncMode.download);
                    }else
                        info.setMode(FileInfo_Sync.syncMode.skip);
                }
            });
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SyncItemHolder(mLayout.inflate(R.layout.file_info_sync,parent,false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        FileInfo_Sync sync = list.get(position);
        SyncItemHolder itemHolder=(SyncItemHolder) holder;
        itemHolder.band(sync);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
