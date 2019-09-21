package com.yuyang.wirelesstransmission;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.ImageButton;

public class initActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);
        viewer=(ImageButton)findViewById(R.id.view_button);
        server=(ImageButton)findViewById(R.id.server_button);
        his=(ImageButton)findViewById(R.id.his_Button);
        setting=(ImageButton)findViewById(R.id.set_eButton);

        server.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int orient=0;
                Configuration mConfiguration = initActivity.this.getResources().getConfiguration();
                int ori = mConfiguration.orientation;
                if (ori == mConfiguration.ORIENTATION_LANDSCAPE) {
                    orient=1;
                } else if (ori == mConfiguration.ORIENTATION_PORTRAIT) {
                    orient=0;
                }
                Intent intent = new Intent(initActivity.this,ServerActivity.class);
                intent.putExtra("ori",orient);
                startActivity(intent);
            }
        });
        viewer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int orient=0;
                Configuration mConfiguration = initActivity.this.getResources().getConfiguration();
                int ori = mConfiguration.orientation;
                if (ori == mConfiguration.ORIENTATION_LANDSCAPE) {
                    orient=1;
                } else if (ori == mConfiguration.ORIENTATION_PORTRAIT) {
                    orient=0;
                }
                Intent intent = new Intent(initActivity.this,ClientActivity.class);
                intent.putExtra("ori",orient);
                startActivity(intent);
            }
        });
        his.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(initActivity.this,HistoryActivity.class);
                startActivity(intent);
            }
        });
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(initActivity.this,SettingsActivity.class);
                startActivity(intent);
            }
        });
        check_premisson();

    }
    @TargetApi(Build.VERSION_CODES.M)
    private boolean check_premisson(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_STROAGE);
            return false;
        }
        else
            return true;
    }

    private ImageButton viewer,server,his,setting;
    private static final int REQUEST_STROAGE=0X12;

    private void not(){
        Intent myIntent = new Intent(this, initActivity.class);
        //Initialize PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, myIntent, 0);
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setContentTitle("Server").setSmallIcon(R.drawable.arrow_down).setContentIntent(pendingIntent)
                .setContentText("in server");
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(uri);
        notificationManager.notify(1,builder.build());
    }
}
