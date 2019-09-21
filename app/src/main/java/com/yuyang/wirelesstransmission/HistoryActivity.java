package com.yuyang.wirelesstransmission;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.yuyang.wirelesstransmission.adapter.FileInfoAdapter;
import com.yuyang.wirelesstransmission.transmission.FileInfo_rec;

import java.io.File;
import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity implements FileInfoAdapter.onItemRequestListener ,FileInfoAdapter.onItemClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //初始化用户界面
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);//载入用户界面布局
        setTitle("历史记录");//修改该界面的标题
        view_his=(RecyclerView)findViewById(R.id.history_view);//对相应的元素进行变量绑
        // ......
        view_his.setLayoutManager(new LinearLayoutManager(this));
        helper = DataHelper.getHelper(this);
        historys=helper.getHistory();
        historyAdapter=new FileInfoAdapter(historys,this,true);
        view_his.setAdapter(historyAdapter);
    }
    /*
    *
    * */

    private Intent getIntentOut(FileInfo_rec info) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        File file=new File(info.getPath());
        Log.d("pathdebug",info.getPath());
        if(file==null||!file.exists()){
            Toast.makeText(this, "file does not exist", Toast.LENGTH_SHORT).show();
            return null;
        }
        Uri uri=Uri.fromFile(file);
        intent.setDataAndType(uri, info.getMimeType());
        return intent;

    }
    private void openBySys(Intent intent) {
        try {
            startActivity(intent);
        }
        catch (Exception e) {
            Toast.makeText(this, "Cannot Open with System", Toast.LENGTH_SHORT);
        }
    }

    private RecyclerView view_his;
    private ArrayList<FileInfo_rec> historys;
    private FileInfoAdapter historyAdapter;
    private DataHelper helper;

    private void delete_file_dialog(final int pos){
        AlertDialog.Builder builder= new AlertDialog.Builder(HistoryActivity.this);
        final FileInfo_rec rec = historys.get(pos);
        builder.setTitle("Delete record");
        builder.setMessage(String.format("Delete record\"%s\"?",rec.getName()));
        builder.setNeutralButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                helper.delete(null,rec.getPath());
                historys.remove(pos);
                historyAdapter.notifyItemRemoved(pos);

            }
        });
        builder.show();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.his_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.h_cle:
                showClearDialog();
                return true;
            default:
                return false;
        }
    }
    private void showClearDialog(){
        AlertDialog.Builder dialog =new AlertDialog.Builder(this);
        dialog.setTitle("Clear Histories?");
        dialog.setMessage("Do you wan to clear all records?");
        dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                helper.clear(null);
                historys.clear();
                historyAdapter.notifyDataSetChanged();
            }
        });
        dialog.setNegativeButton("No",null);
        dialog.create().show();
    }

    @Override
    public void OnItemRequest(int pos) {
        return;
    }

    @Override
    public void OnClickListener(int pos) {
        openBySys(getIntentOut(historys.get(pos)));
    }

    @Override
    public void OnLongClickListerer(int pos) {
        delete_file_dialog(pos);
    }
}
