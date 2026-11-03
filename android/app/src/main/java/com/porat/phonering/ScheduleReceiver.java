package com.porat.phonering;
import android.content.*;
public final class ScheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        new QuietController(context).reconcile();
    }
}
