package com.yuyang.wirelesstransmission.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yuyang.wirelesstransmission.transmission.FileAccess;
import com.yuyang.wirelesstransmission.R;
import com.yuyang.wirelesstransmission.transmission.TransManage;
import com.yuyang.wirelesstransmission.transmission.TransInfo;
/**
 * Created by vista on 2017/9/30.
 */

public class TransAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private TransManage manage;
    private LayoutInflater mLayout;
    private OnTransItemCancelListener listener;
    public interface OnTransItemCancelListener {
        void OnTransItemCanceled(int pos);
    }

    public TransAdapter(TransManage m, Activity c){
        manage=m;
        mLayout=LayoutInflater.from(c);
        listener=(OnTransItemCancelListener) c;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TransItemHolder(mLayout.inflate(R.layout.trans_info_card,parent,false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        TransInfo info=manage.getTask(position);
        TransItemHolder mholder=(TransItemHolder) holder;
        mholder.band(info,position);
    }

    @Override
    public int getItemCount() {
        return manage.getTaskSize();
    }

    private class TransItemHolder extends RecyclerView.ViewHolder{
        private ImageView type,cancel,tar;
        private ProgressBar bar;
        private TextView name,size;
        public TransItemHolder(View view){
            super(view);
            type=(ImageView)view.findViewById(R.id.trans_icon);
            cancel=(ImageView)view.findViewById(R.id.task_cancel);
            bar=(ProgressBar)view.findViewById(R.id.task_bar);
            name=(TextView)view.findViewById(R.id.trans_name);
            size=(TextView)view.findViewById(R.id.trans_size);
            tar=(ImageView)view.findViewById(R.id.trans_tar);
            bar.setMax(100);
        }
        public void band(TransInfo info,final int pos){
            boolean working=(pos==0);
            tar.setImageResource(info.getInfo().isSend()?R.drawable.arrow_up:R.drawable.arrow_down);
            type.setImageResource(info.getInfo().getTypeIconID());
            bar.setIndeterminate(!working);
            if(working) {
                long s=info.getInfo().getSize();
                if(s!=0) {
                    bar.setProgress((int) (info.getFinished() * 100 / s));
                }else{
                    bar.setIndeterminate(true);
                }
                size.setText(String.format("%s/%s", FileAccess.getFormatSize(info.getFinished()), FileAccess.getFormatSize(info.getInfo().getSize())));
            }else{
                size.setText("等待中");
            }
            name.setText(info.getInfo().getName());
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.OnTransItemCanceled(getAdapterPosition());
                }
            });

        }
    }

}
