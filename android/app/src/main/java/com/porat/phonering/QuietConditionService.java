package com.porat.phonering;
import android.net.Uri;
import android.service.notification.ConditionProviderService;
public final class QuietConditionService extends ConditionProviderService {
    @Override public void onConnected(){new QuietController(this).reconcile();}
    @Override public void onSubscribe(Uri id){if(QuietController.CONDITION.equals(id))new QuietController(this).reconcile();}
    @Override public void onUnsubscribe(Uri id){}
}
