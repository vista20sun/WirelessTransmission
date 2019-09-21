package com.yuyang.wirelesstransmission.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.yuyang.wirelesstransmission.transmission.FileAccess;
import com.yuyang.wirelesstransmission.R;
import com.yuyang.wirelesstransmission.transmission.FileInfo_rec;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by vista on 2017/9/30.
 */

public class FileInfoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ArrayList<FileInfo_rec> data;
    private LayoutInflater mLayout;
    private onItemRequestListener listener;
    private onItemClickListener clickListener;
    private Context context;
    private boolean his;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FileInfoHolder(mLayout.inflate(R.layout.file_info_card,parent,false));
        //创建显示元素，载入对应Layout
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        FileInfo_rec info=data.get(position);//从容器的对应位置取得要显示的对象
        FileInfoHolder mholder=(FileInfoHolder) holder;
        mholder.band(info);//将对象显示在分配的显示元素上
    }
    public FileInfoAdapter(ArrayList<FileInfo_rec> list, Activity c,boolean history){
        data=list;//获取需显示对象的容器
        context=c;
        mLayout=LayoutInflater.from(c);
        listener=(FileInfoAdapter.onItemRequestListener) c;
        clickListener=(onItemClickListener) c;
        his=history;
    }
    @Override
    public int getItemCount() {
        return data.size();//确定需显示对象容器的大小
    }



    public interface onItemRequestListener{
        void OnItemRequest(int pos);
    }
    public interface onItemClickListener{
        void OnClickListener(int pos);
        void OnLongClickListerer(int pos);
    }


    public FileInfoAdapter(ArrayList<FileInfo_rec> list, Activity c){
        this(list,c,false);
    }


    private class FileInfoHolder extends RecyclerView.ViewHolder{
        private ImageView type_icon;
        private TextView name,size,type,path,checked;
        public FileInfoHolder(View view){
            super(view);
            type_icon=(ImageView)view.findViewById(R.id.file_icon);
            name=(TextView)view.findViewById(R.id.file_name);
            size=(TextView)view.findViewById(R.id.file_size);
            path=(TextView)view.findViewById(R.id.file_path);
            type=(TextView)view.findViewById(R.id.file_type);
            checked=(TextView) view.findViewById(R.id.file_verified);
            //将UI元素与变量绑定
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!his)
                        listener.OnItemRequest(getLayoutPosition());
                    else
                        clickListener.OnClickListener(getLayoutPosition());
                }
            });
            //设置处理用户操作的触发器
        }
        public void band(FileInfo_rec info){
            //将信息绑定到对应UI元素上
            name.setText(info.getName());
            String types[]=info.getMime().split("/");
            type.setText(types[0].equals("application")?types[1]:types[0]);
            long size_t=info.getSize();
            size.setText(size_t>=0?FileAccess.getFormatSize(size_t):"directory");

            if(info.isDone()) {
                SimpleDateFormat sdf= new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                path.setText(sdf.format(new Date(info.getDocumentFile().lastModified())));
            }
            else
                path.setText(info.getPath());
            type_icon.setImageResource(info.getTypeIconID());
        }
    }

            /*
        *
            if(!his)
                checked.setVisibility(View.GONE);
            else {
                switch (info.getVerify()) {
                    case verified:
                        checked.setText("校验通过");
                        checked.setTextColor(ContextCompat.getColor(context,R.color.verified));
                        break;
                    case unverified:
                        checked.setText("校验失败");
                        checked.setTextColor(ContextCompat.getColor(context,R.color.unverified));
                        break;
                    default:
                        checked.setText("未校验");
                        checked.setTextColor(ContextCompat.getColor(context,R.color.unchecked));
                }
            }*/
}
