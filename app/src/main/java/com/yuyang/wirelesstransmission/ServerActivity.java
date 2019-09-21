package com.yuyang.wirelesstransmission;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.yuyang.wirelesstransmission.adapter.TransAdapter;
import com.yuyang.wirelesstransmission.transmission.FileAccess;
import com.yuyang.wirelesstransmission.transmission.FileInfo;
import com.yuyang.wirelesstransmission.transmission.FileInfo_rec;
import com.yuyang.wirelesstransmission.transmission.ServerConnection;
import com.yuyang.wirelesstransmission.transmission.Server_t;
import com.yuyang.wirelesstransmission.transmission.TransInfo;
import com.yuyang.wirelesstransmission.transmission.TransManage;
import com.yuyang.wirelesstransmission.transmission.tranSignal;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

public class ServerActivity extends AppCompatActivity implements Server_t.onSignalDeliverListener, TransAdapter.OnTransItemCancelListener,ServerConnection.ConnectionCompact {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Server");
        setContentView(R.layout.activity_sever);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        int ori=getIntent().getIntExtra("ori",0);
        if(ori==0){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }else{
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        pager=(ViewPager)findViewById(R.id.server_pages);
        LayoutInflater inflater=getLayoutInflater();
        server_info=inflater.inflate(R.layout.server_page,null);
        server_trans=inflater.inflate(R.layout.trans_page,null);
        trans_view=(RecyclerView)server_trans.findViewById(R.id.trans_view);
        server_trans.findViewById(R.id.trans_opt).setVisibility(View.GONE);

        helper = DataHelper.getHelper(this);
        settings = SettingStores.getSetingStores(this);

        scroll_log=(ScrollView)server_info.findViewById(R.id.server_scroll);
        server_log=(TextView) server_info.findViewById(R.id.server_log);
        server_proc=(TextView) server_info.findViewById(R.id.server_proc);
        server_bar=(CircleProgress)server_info.findViewById(R.id.server_bar);
        trans_icon=(ImageView) server_info.findViewById(R.id.fileicon_curr);

        connection = new ServerConnection(this);

        pause=(Button)findViewById(R.id.server_pause);
        cancel=(Button)findViewById(R.id.server_cancel);
        pagetitle=new String[]{"信息总览","传输列表"};
        ViewList=new View[]{server_info,server_trans};
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connection.cancel_all_task();
                cancel.setEnabled(false);
            }
        });
        cancel.setEnabled(false);
        pause.setEnabled(false);
        server_proc.setText("当前无任务");

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connection.setPause_all(!connection.isPause_all());
                if(connection.isPause_all()){
                    connection.send(tranSignal.Stop);
                    pause.setText("继续");
                }else{
                    connection.send(tranSignal.Continue);
                    pause.setText("暂停");
                }
                log_append(connection.isPause_all()?"暂停传输":"继续传输");
            }
        });


        manage=new TransManage();
        connection.setManage(manage);
        connection.buildServer(settings.getDefaultSignalPort(),settings.getDefaultFilePort(),settings.getTime_Out(),this);


        trans_view.setLayoutManager(new LinearLayoutManager(this));
        transAdapter=new TransAdapter(manage,this);
        trans_view.setAdapter(transAdapter);
        pager.setAdapter(getPageAdapter());
        handler=new Handler();

        notificationProc=-1;
        notBuilder=null;
        notManager=null;
        sync_applying= false;
        working =false;

        proc_update();
        start_helper();
    }


    private void start_helper(){
        AlertDialog.Builder helper= new AlertDialog.Builder(ServerActivity.this);
        final View view = LayoutInflater.from(ServerActivity.this).inflate(R.layout.layout_server_helper,null);
        helper.setTitle("建立服务");
        helper.setView(view);
        helper.setCancelable(false);
        final EditText path=(EditText) view.findViewById(R.id.share_dir);
        final TextView sele=(TextView) view.findViewById(R.id.server_sele_dir);
        final Switch ver = (Switch) view.findViewById(R.id.verification);
        editText=path;
        path.setEnabled(false);
        shareDir = FileAccess.getDir_Doc(this);
        connection.setSharedir(shareDir);
        if(shareDir !=null)
            path.setText(FileAccess.getPathUri(this, shareDir.getUri()));
        sele.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileAccess.getDirActivity(ServerActivity.this);
            }
        });
        helper.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ServerActivity.this.finish();
                editText=null;
            }
        });
        helper.setPositiveButton("应用", null);
        final AlertDialog dialog= helper.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(shareDir ==null) {
                    path.setError("无效输入");
                    return;
                }
                if(shareDir.isFile()){
                    path.setError("无效路径");
                    path.requestFocus();
                    return;
                }
                if(getIP().equals("0.0.0.0")){
                    Toast.makeText(ServerActivity.this, "请检查连接", Toast.LENGTH_SHORT).show();
                    return;
                }
                verficition=ver.isChecked();
                log_append(String.format("共享目录:\"%s\"",path.getText().toString()));
                connection.openServer(getSSID(),getIP(),verficition);
                dialog.dismiss();

            }
        });

    }

    private void proc_update(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(manage.isEmptyTask()){
                    server_bar.setMax(100);
                    server_bar.setProgress(0);
                }else{
                    server_bar.setMax(manage.getTask(0).getSize()==0?100:manage.getTask(0).getSize());
                    server_bar.setProgress(manage.getTask(0).getSize()==0?100:manage.getTask(0).getFinished());
                }

            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if(requestCode==FileAccess.READ_REQUEST_CODE){
            if(resultCode == RESULT_OK){
                Uri uri = resultData.getData();
                if(editText!=null) {
                    shareDir =DocumentFile.fromTreeUri(this,uri);
                    editText.setText(FileAccess.getPathUri(this, shareDir.getUri()));
                    connection.setSharedir(shareDir);
                }
            }
        }
    }

    public void finish(){
        connection.closeServer();
        cancelNotification();
        super.finish();
        //FileAccess.releaseDir_Doc(Sthis);
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



    private void log_append(final CharSequence str){
        handler.post(new Runnable() {
            @Override
            public void run() {
                server_log.append(String.format("%s\n",str));
                scroll_log.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
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
    private String getSSID(){
        WifiManager m=(WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!m.isWifiEnabled()) {
            m.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = m.getConnectionInfo();
        return wifiInfo.getSSID();
    }

    private void makeNotification(){
        if(notBuilder!=null)
            return;
        notBuilder=new NotificationCompat.Builder(ServerActivity.this);
        Intent myIntent = new Intent(this, ServerActivity.class);
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
    private void notiificationUpdate(TransInfo info){
        if(notBuilder==null||manage.isEmptyTask())
            return;
        notBuilder.setContentTitle(String.format("%s-%s",info.isSend()?"分享":"接收",info.getName()));
        notBuilder.setContentText(String.format("剩余任务:%d",manage.getTaskSize()));
        if(info.getSize()==0){
            notBuilder.setProgress(0,0 ,true);
            notManager.notify(notificationNum, notBuilder.build());
            return;
        }
        int pro=(int)((100*info.getFinished()/info.getSize()));
        if(pro==100||pro>notificationProc+5) {
            notificationProc = pro;
            notBuilder.setProgress(100,notificationProc , false);
            notManager.notify(notificationNum, notBuilder.build());
        }
    }

    private void notiificationUpdateRealTime(){
        if(notBuilder==null||manage.isEmptyTask())
            return;
        TransInfo info=manage.getTask(0);
        notBuilder.setContentTitle(String.format("正在%s-%s",info.isSend()?"分享":"接收",info.getName()));
        notBuilder.setContentText(String.format("剩余任务:%d",manage.getTaskSize()));
        notManager.notify(notificationNum, notBuilder.build());
    }
    private void notificationIndeterminate(boolean indeterminate){
        if(indeterminate) {//
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

    private void showCloseDialog(){
        AlertDialog.Builder dialog =new AlertDialog.Builder(this);
        dialog.setTitle("关闭服务?");
        dialog.setMessage("确认关闭服务?");
        dialog.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                connection.closeServer();
                connection.setClosed(true);
                ServerActivity.this.finish();
            }
        });
        dialog.setNegativeButton("否",null);
        dialog.create().show();
    }

    private void swipIcon(final FileInfo info){
        final AlphaAnimation in=new AlphaAnimation(0, 1);
        in.setDuration(150);
        in.setFillAfter(false);
        AlphaAnimation out=new AlphaAnimation(1, 0);
        out.setDuration(150);
        out.setFillAfter(false);
        out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if(info!=null)
                    trans_icon.setImageResource(info.getTypeIconID());
                else
                    trans_icon.setImageDrawable(null);
                trans_icon.startAnimation(in);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        trans_icon.startAnimation(out);
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

    @Override
    public InputStream onRequireInStream(DocumentFile src) throws FileNotFoundException {
        return ServerActivity.this.getContentResolver().openInputStream(src.getUri());
    }

    @Override
    public OutputStream onRequireOutStream(DocumentFile tar) throws FileNotFoundException {
        return ServerActivity.this.getContentResolver().openOutputStream(tar.getUri());
    }

    @Override
    public void log(final String info) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                log_append(info);
            }
        });
    }
    private void setOnWorking(final boolean on){
        working = on;
        handler.post(new Runnable() {
            @Override
            public void run() {
                pager.setKeepScreenOn(on);
                cancel.setEnabled(on&&!sync_applying);
            }
        });
    }

    @Override
    public void OnLinkLost() {
        connection.closeServer();
        manage.clearTask();
        handler.post(new Runnable() {
            @Override
            public void run() {
                transAdapter.notifyDataSetChanged();
                cancel.setEnabled(false);
                pause.setEnabled(false);
                trans_icon.setImageDrawable(null);
                server_bar.setProgress(0);
            }
        });
        if(!connection.isClosed()) {
            connection.initFlags();
            connection.openServer(getSSID(),getIP(),verficition);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ServerActivity.this, "连接丢失，请检查网络", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onProcessUpdate() {
        proc_update();
    }

    @Override
    public void onTaskAdded(int pos) {
        notiificationUpdateRealTime();
        transAdapter.notifyItemInserted(pos);
    }

    @Override
    public void onAllTaskFinished() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                swipIcon(null);
                server_proc.setText("当前无任务");
            }
        });
        setOnWorking(false);
    }

    @Override
    public void onTaskRemoved(final int idx) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                transAdapter.notifyItemRemoved(idx);
            }
        });
    }

    @Override
    public void onTaskPaused() {
        pause.setText("继续");
    }

    @Override
    public void onTaskContinued() {
        pause.setText("暂停");
    }

    @Override
    public void onTransStart() {
        makeNotification();
        setOnWorking(true);
    }

    @Override
    public void onRunningTaskChanged(final TransInfo info) {
        notiificationUpdate(info);
        handler.post(new Runnable() {
            @Override
            public void run() {
                swipIcon(info.getInfo());
                server_proc.setText(info.getName());
            }
        });
        notificationProc=-1;

    }

    @Override
    public void onTransFinish() {
        cancelNotification();
        TransFinishService.notify_new(ServerActivity.this,"分享任务完成");
    }

    @Override
    public void onRunningTaskProcessUpdate() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                transAdapter.notifyItemChanged(0);
            }
        });
    }

    @Override
    public void onNotificationUpdate(TransInfo info) {
        notiificationUpdate(info);
    }

    @Override
    public void onTaskFinish(FileInfo_rec info) {
        helper.insert_history(null,info);
    }

    @Override
    public void onPause(boolean state) {
        notificationIndeterminate(state);
    }

    @Override
    public void onCancelAllTask() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                transAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onStartServerFail() {
        finish();
    }

    @Override
    public void onStartServerSuccess() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                //cancel.setEnabled(true);
                pause.setEnabled(true);
            }
        });
    }

    @Override
    public void onSyncStart(final int size){
        handler.post(new Runnable() {
            @Override
            public void run() {
                trans_icon.setImageResource(R.drawable.icon_sync_colored);
                server_bar.setMax(size);
                server_bar.setProgress(0);
            }
        });
    }
    @Override
    public void onSyncFinish(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                trans_icon.setImageDrawable(null);
                server_bar.setMax(100);
                server_bar.setProgress(0);
            }
        });
    }
    @Override
    public void onSyncUpdata(final int proc){
        handler.post(new Runnable() {
            @Override
            public void run() {
                server_bar.setProgress(proc);
            }
        });
    }

    @Override
    public void onSyncApplying() {
        sync_applying = !sync_applying;
        if(sync_applying||!working)
            return;
        handler.post(new Runnable() {
            @Override
            public void run() {
                cancel.setEnabled(true);
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        switch (keyCode){
            case KeyEvent.KEYCODE_BACK: showCloseDialog();return true;
            default: return super.onKeyDown(keyCode,event);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.service_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.s_his:
                Intent intent = new Intent(ServerActivity.this,HistoryActivity.class);
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }



    @Override
    public void OnTransItemCanceled(int pos) {
        if(sync_applying)
            return;
        connection.cancelItem(pos);
    }
    @Override
    public void onDestroy(){
        connection.closeServer();
        super.onDestroy();
        Log.d("","dest");
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig){
        finish();
    }
    @Override
    public void onSocketSet(final String ip,final String ssid,final int port){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showQRCode(String.format("%s:::%s:::%d",ssid,ip,port));
            }
        },100);
    }
    @Override
    public void onSocketAccept(){
        trans_icon.setBackground(null);
    }

    public void showQRCode(String str){
        BitMatrix result = null;
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            result = multiFormatWriter.encode(str, BarcodeFormat.QR_CODE, trans_icon.getWidth(), trans_icon.getHeight());
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            if(qrcode!=null&&!qrcode.isRecycled()) {
                qrcode.recycle();
                qrcode=null;
            }
            qrcode = barcodeEncoder.createBitmap(result);
        } catch (WriterException e){
            e.printStackTrace();
        } catch (IllegalArgumentException iae){ // ?
            return;
        }
        trans_icon.setImageBitmap(qrcode);
    }

    private ViewPager pager;
    private View server_info,server_trans, ViewList[];
    private String pagetitle[];
    private ScrollView scroll_log;
    private TextView server_proc,server_log;
    private EditText editText;
    private RecyclerView trans_view;
    private CircleProgress server_bar;
    private Button pause,cancel;
    private TransManage manage;
    private TransAdapter transAdapter;
    private DocumentFile shareDir;
    private Handler handler;
    private boolean verficition,sync_applying,working;

    private DataHelper helper;
    private ImageView trans_icon;
    private SettingStores settings;
    private NotificationCompat.Builder notBuilder;
    private NotificationManager notManager;
    private final static int notificationNum=0x114;
    private int notificationProc;
    private ServerConnection connection;

    private Bitmap qrcode;
}
