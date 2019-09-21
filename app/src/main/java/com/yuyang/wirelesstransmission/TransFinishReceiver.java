package com.yuyang.wirelesstransmission;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by vista on 2017/12/2.
 */

public class TransFinishReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setClass(context,TransFinishService.class);
        context.startService(intent);
    }
}
