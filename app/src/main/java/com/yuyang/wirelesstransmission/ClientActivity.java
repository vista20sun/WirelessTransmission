package com.yuyang.wirelesstransmission;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v4.provider.DocumentFile;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yuyang.wirelesstransmission.adapter.FileInfoAdapter;
import com.yuyang.wirelesstransmission.adapter.SyncAdapter;
import com.yuyang.wirelesstransmission.adapter.TransAdapter;
import com.yuyang.wirelesstransmission.transmission.ClientConnection;
import com.yuyang.wirelesstransmission.transmission.Client_t;
import com.yuyang.wirelesstransmission.transmission.FileAccess;
import com.yuyang.wirelesstransmission.transmission.FileInfo;
import com.yuyang.wirelesstransmission.transmission.FileInfo_rec;
import com.yuyang.wirelesstransmission.transmission.FileInfo_send;
import com.yuyang.wirelesstransmission.transmission.Server_t;
import com.yuyang.wirelesstransmission.transmission.TransInfo;
import com.yuyang.wirelesstransmission.transmission.TransManage;
import com.yuyang.wirelesstransmission.transmission.tranSignal;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientActivity
        extends AppCompatActivity
        implements TransAdapter.OnTransItemCancelListener,FileInfoAdapter.onItemRequestListener,Client_t.onSignalDeliverListener,FileInfoAdapter.onItemClickListener,ClientConnection.ConnectionCompact {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Explorer");
        setContentView(R.layout.activity_client);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        int ori=getIntent().getIntExtra("ori",0);
        if(ori==0){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }else{
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        pager=(ViewPager)findViewById(R.id.client_pages);
        LayoutInflater inflater=getLayoutInflater();
        page_file=inflater.inflate(R.layout.file_page,null);
        page_trans=inflater.inflate(R.layout.trans_page,null);
        page_his=inflater.inflate(R.layout.his_page,null);
        view_file=(RecyclerView)page_file.findViewById(R.id.file_view);
        view_trans=(RecyclerView)page_trans.findViewById(R.id.trans_view);
        view_his=(RecyclerView)page_his.findViewById(R.id.his_view);
        file_dir=(TextView)page_file.findViewById(R.id.file_dir);
        trans_stop=(TextView)page_trans.findViewById(R.id.trans_stop_txt);
        editText=null;
        qrIP=null;
        qrPort=null;

        file_upload=page_file.findViewById(R.id.file_upload);
        file_trans=page_file.findViewById(R.id.file_proc);

        trans_pause=page_trans.findViewById(R.id.trans_stop);
        trans_cancel=page_trans.findViewById(R.id.trans_cancel);
        trans_cancel.setEnabled(false);

        his_clean=page_his.findViewById(R.id.his_clean);
        his_trans=page_his.findViewById(R.id.his_trans);

        his_clean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish_list.clear();
                historyAdapter.notifyDataSetChanged();
            }
        });
        his_trans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pager.setCurrentItem(1);
            }
        });

        dir_bar=(ProgressBar) page_file.findViewById(R.id.dir_bar);
        trans_bar1=(CircleProgress) page_file.findViewById(R.id.file_bar);
        trans_bar2=(CircleProgress) page_trans.findViewById(R.id.trans_bar);
        trans_bar3=(CircleProgress) page_his.findViewById(R.id.his_bar);
        trans_bar1.setStrokeWidth(15);
        trans_bar2.setStrokeWidth(15);
        trans_bar3.setStrokeWidth(15);

        pagetitle=new String[]{"文件浏览","传输列表","历史记录"};
        ViewList=new View[]{page_file,page_trans,page_his};

        file_trans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pager.setCurrentItem(1);
            }
        });
        trans_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            connection.switchPause();
            }
        });
        trans_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connection.cancelAllTask();
                trans_cancel.setEnabled(false);
            }
        });
        file_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileAccess.getFileActivity(ClientActivity.this);
            }
        });

        connection = new ClientConnection(this);


        view_file.setLayoutManager(new LinearLayoutManager(this));
        view_trans.setLayoutManager(new LinearLayoutManager(this));
        view_his.setLayoutManager(new LinearLayoutManager(this));
        download=null;
        //client=null;
        handler=new Handler();

        file_list=new ArrayList<>();
        finish_list=new ArrayList<>();
        fileInfoAdapter=new FileInfoAdapter(file_list,this);
        historyAdapter=new FileInfoAdapter(finish_list,this,true);
        manage=new TransManage();
        connection.bindTransManage(manage);
        transAdapter=new TransAdapter(manage,this);
        view_file.setAdapter(fileInfoAdapter);
        view_his.setAdapter(historyAdapter);
        view_trans.setAdapter(transAdapter);
        path_stack=new Stack<>();
        path_stack.push("/");

        request_pos=null;
        notificationProc=-1;
        notBuilder=null;
        notManager=null;
        working =false;

        pager.setAdapter(getPageAdapter());
        helper=DataHelper.getHelper(this);
        settings=SettingStores.getSetingStores(this);
        total_bar_update();
        start_helper();
    }


    private PagerAdapter getPageAdapter(){
        return new PagerAdapter() {
            @Override
            public int getCount() {
                return ViewList.length;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view==ViewList[Integer.parseInt(object.toString())];
            }
            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView(ViewList[position]);
            }
            @Override
            public Object instantiateItem(ViewGroup container, int position){
                container.addView(ViewList[position]);
                return position;
            }
            @Override
            public CharSequence getPageTitle(int position){
                return pagetitle[position];
            }
        };
    }
    private void total_bar_update(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(manage.isEmptyTask()){
                    trans_bar1.setMax(100);
                    trans_bar1.setProgress(0);
                    trans_bar2.setMax(100);
                    trans_bar2.setProgress(0);
                    trans_bar3.setMax(100);
                    trans_bar3.setProgress(0);
                }else{
                    trans_bar1.setMax(manage.getTask(0).getSize());
                    trans_bar1.setProgress(manage.getTask(0).getFinished());
                    trans_bar2.setMax(manage.getTask(0).getSize());
                    trans_bar2.setProgress(manage.getTask(0).getFinished());
                    trans_bar3.setMax(manage.getTask(0).getSize());
                    trans_bar3.setProgress(manage.getTask(0).getFinished());
                }
            }
        });
    }
    private void repeat_file_dialog(final FileInfo_rec rec, final DocumentFile df){
        AlertDialog.Builder builder= new AlertDialog.Builder(ClientActivity.this);
        builder.setTitle("文件已存在");
        builder.setMessage(String.format("文件\"%s\"已经存在, 是否覆盖?",rec.getName()));
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("覆盖", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                connection.send(tranSignal.Get_File,rec.getPath());
                remove_repeat(df);
                connection.add_task(rec);
                connection.start_task();
                dialog.dismiss();
            }
        });
        builder.show();
    }
    private void remove_repeat(DocumentFile df){
        for (int i = 0;i<finish_list.size();i++){
            if(finish_list.get(i).getDocumentFile().getUri().equals(df.getUri())) {
                finish_list.remove(i);
                final int x = i;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        historyAdapter.notifyItemRemoved(x);
                    }
                });
                return ;
            }
        }
    }
    private void repeat_task_dialog(final FileInfo_rec rec,final int idx){
        AlertDialog.Builder builder= new AlertDialog.Builder(ClientActivity.this);
        builder.setTitle("任务冲突");
        builder.setMessage(String.format("文件\"%s\"已经在传输列队中?",rec.getName()));
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("查看", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                pager.setCurrentItem(1);
                view_trans.smoothScrollToPosition(idx);
                //total_bar_update();
                dialog.dismiss();
            }
        });
        builder.show();
    }




    public static boolean isIP(String str){
        String ip = "((?:(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d))))";
        Pattern pattern = Pattern.compile(ip);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }
    public static boolean isPort_accept(int port){
        return port>1024&&port<60000;
    }

    private void start_helper(){
        AlertDialog.Builder helper= new AlertDialog.Builder(ClientActivity.this);
        final View view = LayoutInflater.from(ClientActivity.this).inflate(R.layout.layout_catch_helper,null);
        helper.setTitle("建立连接");
        helper.setView(view);
        helper.setCancelable(false);
        final EditText path=(EditText)view.findViewById(R.id.catch_to_dir);
        final EditText port=(EditText)view.findViewById(R.id.catch_port);
        final EditText addr=(EditText)view.findViewById(R.id.catch_ip);
        final TextView sele=(TextView)view.findViewById(R.id.catch_sele_dir);
        final ProgressBar link=(ProgressBar)view.findViewById(R.id.link__process);
        final ImageButton qr = (ImageButton) view.findViewById(R.id.qr_scan);
        qr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new IntentIntegrator(ClientActivity.this)
                        .setOrientationLocked(false)
                        .setCaptureActivity(QRScanActivity.class)
                        .initiateScan();
            }
        });
        addr.requestFocus();
        final EditText group[]={path,port,addr};
        editText=path;
        qrPort=port;
        qrIP=addr;
        download=FileAccess.getDir_Doc(this);
        if(download!=null)
            path.setText(FileAccess.getPathUri(this,download.getUri()));
        port.setText(settings.getPortSignalDefault());
        sele.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileAccess.getDirActivity(ClientActivity.this);
            }
        });
        helper.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ClientActivity.this.finish();
                editText=null;
                qrIP=null;
                qrPort=null;
            }
        });
        helper.setPositiveButton("应用", null);
        final AlertDialog dialog= helper.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(EditText E:group){
                    if(E.getText().length()==0){
                        E.setError("输入错误");
                        E.requestFocus();
                        return;
                    }
                }
                if(download==null||download.isFile()){
                    path.setError("无法使用该路径");
                    path.requestFocus();
                    return;
                }

                String address=addr.getText().toString();
                if(!isIP(address)){
                    addr.setError("无效地址");
                    addr.requestFocus();
                    return;
                }
                if(false&&getIP().equals("0.0.0.0")){
                    Toast.makeText(ClientActivity.this, "请检查连接", Toast.LENGTH_SHORT).show();
                    return;
                }
                int port_;
                try{
                    port_ =Integer.parseInt(port.getText().toString());
                    if(!isPort_accept(port_))
                        throw new Exception("无效端口");
                }catch (NumberFormatException e){
                    port.setError("端口号只允许数字");
                    port.requestFocus();
                    return;
                }catch (Exception e){
                    port.setError("无效端口");
                    port.requestFocus();
                    return;
                }
                //client=Client_t.getClient_t(address,port_,settings.getTime_Out(),ClientActivity.this);
                //connection.setClient(client);
                connection.buildClient(address,port_,settings.getTime_Out(),ClientActivity.this);
                connection.setDownload(download);
                link.setIndeterminate(true);
                new Thread(){
                    public void run(){
                        final int e=connection.openClient();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(e==0) {
                                    editText = null;
                                    qrIP=null;
                                    qrPort=null;
                                    dialog.dismiss();
                                }else{
                                    link.setIndeterminate(false);
                                    if(e==-4)
                                        Toast.makeText(ClientActivity.this, "链接被拒绝，请重启服务端", Toast.LENGTH_SHORT).show();
                                    else
                                        Toast.makeText(ClientActivity.this, "连接超时，请重试"+e, Toast.LENGTH_SHORT).show();
                                    connection.closeClient();
                                }
                            }
                        });
                    }
                }.start();
            }
        });

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        switch (keyCode){
            case KeyEvent.KEYCODE_BACK:
                if(pager.getCurrentItem()==0){
                    if(path_stack.size()==1) {
                        Toast.makeText(this, "当前已在根目录", Toast.LENGTH_SHORT).show();
                    }else{
                        if(!connection.isDir_sw()) {
                            path_stack.pop();
                            connection.switch_dir(path_stack.peek());
                        }
                        else {
                            if(request_pos!=null)
                                return true;
                            path_stack.pop();
                            request_pos=new FileInfo_rec("par",-1,path_stack.peek());
                            connection.send(tranSignal.Target_Found);
                            Log.d("target_found","calcel");
                        }
                    }
                }else{
                    pager.setCurrentItem(0);
                }
                return true;
            default: return super.onKeyDown(keyCode,event);
        }
    }
    @Override
    public void OnSignalDeliver() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                connection.signalHandel();
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if(requestCode==FileAccess.READ_REQUEST_CODE){
            if(resultCode == RESULT_OK){
                Uri uri = resultData.getData();
                if(editText!=null) {
                    download=DocumentFile.fromTreeUri(this,uri);
                    editText.setText(FileAccess.getPathUri(this,download.getUri()));

                }
            }
        }else if(requestCode==FileAccess.FILE_REQUEST_CODE){
            if(resultCode == RESULT_OK){
                Uri uri = resultData.getData();
                DocumentFile documentFile=DocumentFile.fromSingleUri(this,uri);
                FileInfo_send send=new FileInfo_send(documentFile);
                connection.add_task(send);
                connection.send(tranSignal.Send_File,String.format("%s:::%d:::%s",send.getName(),send.getSize(),path_stack.peek()));
                connection.start_task();
            }
        }else if(requestCode==IntentIntegrator.REQUEST_CODE){
            IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode,resultCode,resultData);
            if(intentResult!=null){
                if(intentResult.getContents() == null) {
                    Toast.makeText(this,"无效二维码",Toast.LENGTH_LONG).show();
                }else{
                    String ScanResult = intentResult.getContents();
                    String datas[] = ScanResult.split(":::");
                    if(qrPort==null||qrIP==null)
                        return;
                    qrIP.setText(datas[1]);
                    qrPort.setText(datas[2]);
                    Toast.makeText(this, "服务器所在网络:"+datas[0], Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    ////



   /////

    @Override
    public InputStream onRequireInStream(DocumentFile src) throws FileNotFoundException {
        return ClientActivity.this.getContentResolver().openInputStream(src.getUri());
    }

    @Override
    public OutputStream onRequireOutStream(DocumentFile tar) throws FileNotFoundException {
        return ClientActivity.this.getContentResolver().openOutputStream(tar.getUri());
    }

    @Override
    public void onTaskStart() {
        setOnWorking(true);

    }

    @Override
    public void onProgressUpdate() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                transAdapter.notifyItemChanged(0);
            }
        });
        notificationUpdate();
        total_bar_update();
    }

    @Override
    public void onTaskFinished() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                transAdapter.notifyItemRemoved(0);
            }
        });
        total_bar_update();
    }

    @Override
    public void onAllTaskFinished() {
        total_bar_update();
        cancelNotification();
        TransFinishService.notify_new(ClientActivity.this,"文件传输已完成");
        setOnWorking(false);
    }

    @Override
    public void onMakeNotification() {
        makeNotification();
    }

    @Override
    public void onNotificationUpdate() {
        notificationUpdate();
    }

    @Override
    public void onAddFinishedTask(final FileInfo finishInfo) {
        finish_list.add((FileInfo_rec) finishInfo);
        handler.post(new Runnable() {
            @Override
            public void run() {
                historyAdapter.notifyItemInserted(finish_list.size());
            }
        });
        helper.insert_history(null,(FileInfo_rec)finishInfo);
    }

    @Override
    public void onNotificationIndeterminate(boolean state) {
        notificationIndeterminate(state);
    }

    @Override
    public void onNotificationInit() {
        notificationUpdate();
        notificationProc=-1;
    }

    @Override
    public void onTaskStop() {
        trans_stop.setText("继续");
    }

    @Override
    public void onTaskContinue() {
        trans_stop.setText("暂停");
    }

    @Override
    public void onTaskCancelALL() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                transAdapter.notifyDataSetChanged();
            }
        });
        total_bar_update();
    }

    @Override
    public void onTaskAdd(final int pos) {
        total_bar_update();
        notificationUpdateRealTime();
        handler.post(new Runnable() {
            @Override
            public void run() {
                transAdapter.notifyItemInserted(pos);
            }
        });
    }

    @Override
    public void onTaskCancelAt(int index) {
        transAdapter.notifyItemRemoved(index);
    }

    @Override
    public void onPauseSwitch(boolean state) {
        trans_stop.setText(state?"继续":"暂停");
    }

    @Override
    public void onAddFileInfo(FileInfo_rec file) {
        file_list.add(file);
        fileInfoAdapter.notifyItemInserted(file_list.size());
        //connection.send(tranSignal.INFO_D);
    }

    @Override
    public void OnLinkLost() {
        connection.setLinklost(true);
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ClientActivity.this, "连接丢失，请检查网络设置", Toast.LENGTH_SHORT).show();
            }
        });
        connection.closeClient();
        finish();
    }

    @Override
    public void onSwitchDir() {
        file_list.clear();
        fileInfoAdapter.notifyDataSetChanged();
        file_dir.setText(path_stack.peek());
    }

    @Override
    public void onTaskRepeat(final FileInfo_rec rec,final int idx) {
        repeat_task_dialog(rec,idx);
    }

    @Override
    public void onFileRepeat(final FileInfo_rec rec, final DocumentFile df) {
        repeat_file_dialog(rec,df);
    }

    @Override
    public void onRecordPath(String path) {
        path_stack.push(path);
    }

    @Override
    public void onSwitchDirStart() {
        dir_bar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDirSwitched() {
        dir_bar.setVisibility(View.INVISIBLE);
        if(request_pos!=null){
            connection.requestItem(request_pos);
            request_pos=null;
        }
    }
    @Override
    public void onSyncStart(final boolean success){
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(success)
                    crateSyncDialog();
                else
                    Toast.makeText(ClientActivity.this, "请等待当前任务完成", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void onSyncFinish(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                finishSync();
            }
        });
    }

    @Override
    public void onSyncAdd(final ClientConnection.FileInfo_Sync sync){
        if(syncAdapter ==null) {
            Log.d("SYNCTEST-ADP", "syncAdapter missing");
            return;
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                connection.getSyncList().add(sync);
                syncAdapter.notifyItemInserted(connection.getSyncList().size());
            }
        });
    }

    @Override
    public void onSyncProcessMaxSet(final int p){
        if(syncBar==null)
            return;
        handler.post(new Runnable() {
            @Override
            public void run() {
                syncBar.setIndeterminate(false);
                syncBar.setMax(p);
                syncBar.setProgress(0);
                syncBar.setSecondaryProgress(0);
            }
        });

    }

    @Override
    public void onSyncProcessUpdata(int p){
        if(syncBar==null)
            return;
        syncBar.setProgress(p);
    }

    @Override
    public void onSyncApplying(final boolean apply) {
        if(apply == true && !working)
            return;
        handler.post(new Runnable() {
            @Override
            public void run() {
                trans_cancel.setEnabled(!apply);
            }
        });
    }

    public void onSecondarySyncProcessUpdate(int p){
        if(syncBar==null)
            return;
        syncBar.setSecondaryProgress(p);
    }

    @Override
    public void OnTransItemCanceled(int pos) {
        connection.cancelItem(pos);
    }

    @Override
    public void OnItemRequest(int pos) {
        if(request_pos!=null||connection.isCanceling()||connection.isSync_apply())
            return;
        FileInfo_rec info=file_list.get(pos);
        if(connection.isDir_sw()){
            request_pos=info;
            connection.send(tranSignal.Target_Found);
            Log.d("target_found","push");
            return;
        }
        connection.requestItem(info);
    }
    private Intent getIntentOut(FileInfo info) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Log.d("uricheck",info.getDocumentFile().getUri().toString());
        File file=new File(FileAccess.getPathUri(ClientActivity.this,info.getDocumentFile().getUri()));
        if(file==null||!file.exists()){
            Toast.makeText(this,"文件不存在", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "无法找到合适的应用打开该文件", Toast.LENGTH_SHORT);
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.c_his:
                Intent intent = new Intent(ClientActivity.this,HistoryActivity.class);
                startActivity(intent);
                return true;
            case R.id.c_close:
                showCloseDialog();
                return true;
            case R.id.c_sync:
                connection.syncStart();
                return true;
            case R.id.c_ref:
                if(!connection.isDir_sw()) {
                    connection.switch_dir(path_stack.peek());
                }
                else {
                    if(request_pos!=null)
                        return true;
                    request_pos=new FileInfo_rec("par",-1,path_stack.peek());
                    connection.send(tranSignal.Target_Found);
                    Log.d("target_found","calcel");
                }
                return true;

            default:
                return false;
        }
    }

    private void showCloseDialog(){
        AlertDialog.Builder dialog =new AlertDialog.Builder(this);
        dialog.setTitle("关闭连接?");
        dialog.setMessage("是否关闭连接?");
        dialog.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                connection.closeClient();
                ClientActivity.this.finish();
            }
        });
        dialog.setNegativeButton("否",null);
        dialog.create().show();
    }

    private void bindSyncDialog(View dialogView){
        syncBar = (ProgressBar) dialogView.findViewById(R.id.sync_bar);
        seleRemote = (Button) dialogView.findViewById(R.id.sync_select_remote);
        seleLocal = (Button) dialogView.findViewById(R.id.sync_select_local);
        seleClear = (Button) dialogView.findViewById(R.id.sync_select_clear);
        //syncTitle = dialogView.findViewById(R.id.sync_title);
        syncView = (RecyclerView) dialogView.findViewById(R.id.syncRecyclerView);
        syncView.setLayoutManager(new LinearLayoutManager(this));
        syncAdapter = new SyncAdapter(connection.getSyncList(),ClientActivity.this);
        syncBar.setIndeterminate(true);
        seleRemote.setEnabled(false);
        seleLocal.setEnabled(false);
        seleClear.setEnabled(false);
        syncView.setAdapter(syncAdapter);
    }
    private void unbindSyncDialog(){
        syncBar = null;
        seleRemote = null;
        seleLocal = null;
        seleClear=null;
        //syncTitle = null;
        syncView = null;
        syncAdapter = null;
        syncDia=null;
    }

    private void finishSync(){
        if(syncDia==null)
            return;
        seleLocal.setEnabled(true);
        seleRemote.setEnabled(true);
        seleClear.setEnabled(true);
        seleLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(ClientConnection.FileInfo_Sync x:connection.getSyncList()){
                    x.setMode(ClientConnection.FileInfo_Sync.syncMode.upload);
                }
                syncAdapter.notifyDataSetChanged();
            }
        });
        seleRemote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(ClientConnection.FileInfo_Sync x:connection.getSyncList()){
                    x.setMode(ClientConnection.FileInfo_Sync.syncMode.download);
                }
                syncAdapter.notifyDataSetChanged();
            }
        });
        seleClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(ClientConnection.FileInfo_Sync x:connection.getSyncList()){
                    x.setMode(ClientConnection.FileInfo_Sync.syncMode.skip);
                }
                syncAdapter.notifyDataSetChanged();
            }
        });
        syncDia.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        syncDia.getButton(AlertDialog.BUTTON_NEGATIVE).setText("取消");
        syncDia.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
    }
    private void crateSyncDialog(){
        final AlertDialog.Builder syncDialog = new AlertDialog.Builder(ClientActivity.this);
        final View dialogView = LayoutInflater.from(ClientActivity.this).inflate(R.layout.sync_dialog_view,null);
        syncDialog.setTitle("同步目录");
        syncDialog.setCancelable(false);
        syncDialog.setView(dialogView);
        bindSyncDialog(dialogView);
        syncDialog.setPositiveButton("应用",null);
        syncDialog.setNegativeButton("停止",null);
        syncDia=syncDialog.show();
        Window window = syncDia.getWindow();
        window.getDecorView().setPadding(0,0,0,0);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width=WindowManager.LayoutParams.MATCH_PARENT;
        lp.height=WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(lp);
        syncDia.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        syncDia.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connection.applySyncList();
                //connection.clearSyncList();
                syncDia.dismiss();
                unbindSyncDialog();
            }
        });
        syncDia.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(syncDia.getButton(AlertDialog.BUTTON_NEGATIVE).getText().toString().equals("停止")){
                    connection.syncStop();
                    syncDia.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                }else{
                    connection.clearSyncList();
                    syncDia.dismiss();
                    unbindSyncDialog();
                }
            }
        });

    }


    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.client_menu, menu);
        return true;
    }
    @Override
    public void OnClickListener(int pos) {
        openBySys(getIntentOut(finish_list.get(pos)));
    }

    private String getIP(){
        WifiManager m=(WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!m.isWifiEnabled()) {
            m.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = m.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        return Server_t.intToIp(ipAddress);
    }
    @Override
    public void OnLongClickListerer(int pos) {
        delete_file_dialog(pos);
    }
    private void delete_file_dialog(final int pos){
        AlertDialog.Builder builder= new AlertDialog.Builder(ClientActivity.this);
        final FileInfo_rec rec = finish_list.get(pos);
        builder.setTitle("删除记录");
        builder.setMessage(String.format("删除记录\"%s\"?",rec.getName()));
        builder.setNeutralButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("删除记录", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                helper.delete(null,rec.getPath());
                finish_list.remove(pos);
                historyAdapter.notifyItemRemoved(pos);

            }
        });
        builder.setNegativeButton("删除文件", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                helper.delete(null,rec.getPath());
                finish_list.get(pos).getDocumentFile().delete();
                finish_list.remove(pos);
                historyAdapter.notifyItemRemoved(pos);

            }
        });
        builder.show();
    }

    private void makeNotification(){
        if(notBuilder!=null)
            return;
        notBuilder=new NotificationCompat.Builder(ClientActivity.this);
        Intent myIntent = new Intent(this, ClientActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, myIntent, 0);
        notBuilder.setOngoing(true);
        notBuilder.setContentIntent(pendingIntent);
        notBuilder.setSmallIcon(R.drawable.icon_main);
        notBuilder.setContentTitle("Transmission");
        notBuilder.setContentText(String.format("剩余任务:%d",manage.getTaskSize()));
        notBuilder.setProgress(0,0,true);
        notManager=((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE));
        notManager.notify(notificationNum,notBuilder.build());
    }
    private void notificationUpdate(){
        Log.d("NotificationServe","1");
        if(notBuilder==null||manage.isEmptyTask())
            return;
        TransInfo info=manage.getTask(0);
        notBuilder.setContentTitle(String.format("正在%s - %s",info.isSend()?"上传":"下载",info.getName()));
        notBuilder.setContentText(String.format("剩余任务:%d",manage.getTaskSize()));
        if(info.getSize()==0){
            notBuilder.setProgress(0,0 ,true);
            notManager.notify(notificationNum, notBuilder.build());
            return;
        }
        int pro=(int)((100*info.getFinished()/info.getSize()));
        if(pro==100||pro>notificationProc+3) {
            notificationProc = pro;
            notBuilder.setProgress(100,notificationProc , false);
            notManager.notify(notificationNum, notBuilder.build());
        }
    }
    private void notificationUpdateRealTime(){
        if(notBuilder==null||manage.isEmptyTask())
            return;
        TransInfo info=manage.getTask(0);
        notBuilder.setContentTitle(String.format("正在%s-%s",info.isSend()?"分享":"接收",info.getName()));
        notBuilder.setContentText(String.format("剩余任务:%d",manage.getTaskSize()));
        notManager.notify(notificationNum, notBuilder.build());
    }
    private void notificationIndeterminate(boolean indeterminate){// task_wait||cancel_pause||pause_all
        if(indeterminate) {
            notBuilder.setProgress(0, 0, true);
            notManager.notify(notificationNum, notBuilder.build());
        }
    }

    private void cancelNotification(){
        if(notManager==null)
            return;
        notManager.cancel(notificationNum);
        notBuilder=null;
        notManager=null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        finish();
    }
    @Override
    public void finish(){
        connection.closeClient();
        cancelNotification();
        super.finish();
    }
    @Override
    public void onDestroy(){
        connection.closeClient();
        super.onDestroy();
    }

    private void setOnWorking(final boolean on){
        working = on;
        handler.post(new Runnable() {
            @Override
            public void run() {
                pager.setKeepScreenOn(on);
                trans_cancel.setEnabled(on&&!connection.isSync_apply());
            }
        });
    }






    private ViewPager pager;
    private View page_file,page_trans,page_his,ViewList[];
    private RecyclerView view_file,view_trans,view_his;
    private TextView file_dir,trans_stop;
    private EditText editText,qrIP,qrPort;
    private View file_upload,file_trans,trans_pause,trans_cancel,his_clean,his_trans;
    private ProgressBar dir_bar;
    private CircleProgress trans_bar1,trans_bar2,trans_bar3;
    private Handler handler;
    private Stack<String> path_stack;
    private String pagetitle[];
    private DocumentFile download;
    private AlertDialog syncDia;
    private ProgressBar syncBar;
    private Button seleRemote;
    private Button seleLocal;
    private Button seleClear;
    private RecyclerView syncView;
    private SyncAdapter syncAdapter;
    private ArrayList<FileInfo_rec> file_list;
    private ArrayList<FileInfo_rec> finish_list;
    private FileInfo_rec request_pos;
    private FileInfoAdapter fileInfoAdapter,historyAdapter;
    private TransAdapter transAdapter;
    private TransManage manage;
    private DataHelper helper;
    private SettingStores settings;
    private NotificationCompat.Builder notBuilder;
    private NotificationManager notManager;
    private final static int notificationNum=0x514;
    private int notificationProc;
    private ClientConnection connection;
    private boolean working;
}
