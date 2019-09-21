package com.yuyang.wirelesstransmission;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

/**
 * Created by vista on 2017/12/2.
 */

public class TransFinishService extends IntentService {
    public static final String Tag="TransFinishService";
    public static final String KEY_ID="direction";
    public static final String ACTION_ORDER="com.cs442.yluo41.wirelesstransmission.action.finished";
    public TransFinishService() {
        super(Tag);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String info = intent.getStringExtra(KEY_ID);
        if(info == null )return;
        not(info);
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    private void not(String info){
        Intent myIntent = new Intent(this,HistoryActivity.class);
        //Initialize PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, myIntent, 0);
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle("WirelessTransmission")
                .setSmallIcon(R.drawable.icon_main)
                .setContentIntent(pendingIntent)
                .setContentText(info);
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(uri);
        notificationManager.notify(1,builder.build());
    }
    public static void notify_new(Context context,String info){
            Intent intent = new Intent(ACTION_ORDER);
            intent.putExtra(KEY_ID,info);
            context.sendBroadcast(intent);
    }
}
