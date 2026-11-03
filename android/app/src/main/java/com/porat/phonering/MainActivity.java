package com.porat.phonering;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.*;
import androidx.webkit.*;
import org.json.*;
import java.io.ByteArrayInputStream;
import java.util.Collections;

public final class MainActivity extends Activity {
    private static final String ORIGIN="https://appassets.androidplatform.net";
    private WebView web;
    private QuietController controller;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        controller=new QuietController(this);web=new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        WebViewAssetLoader loader=new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
                Uri u=request.getUrl();
                if(isLocal(u)) {
                    WebResourceResponse resource=loader.shouldInterceptRequest(u);
                    if(resource!=null)return resource;
                }
                return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request) {
                return !isLocal(request.getUrl());
            }
        });
        if(!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)){
            android.widget.TextView text=new android.widget.TextView(this);
            text.setText("יש לעדכן את Android System WebView כדי להשתמש באפליקציה.");
            setContentView(text);return;
        }
        WebViewCompat.addWebMessageListener(web,"PhoneRing",Collections.singleton(ORIGIN),
            (view,message,sourceOrigin,isMainFrame,reply)->{
                if(!isMainFrame||!ORIGIN.equals(sourceOrigin.toString()))return;
                JSONObject response=new JSONObject();
                try {
                    String data=message.getData();
                    if(data==null||data.length()>100000)throw new JSONException("Invalid request");
                    JSONObject req=new JSONObject(data);response.put("id",req.getString("id"));
                    switch(req.getString("action")){
                        case "load":controller.reconcile();break;
                        case "save":controller.save(req.getJSONObject("payload"));break;
                        case "connect":
                            if(!controller.access())startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                            else {controller.connect();startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));}
                            break;
                        case "exact":
                            if(android.os.Build.VERSION.SDK_INT>=31)startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));
                            break;
                        default:throw new JSONException("Unknown action");
                    }
                    response.put("result",controller.result());
                }catch(Exception e){try{response.put("error","לא ניתן להשלים את הפעולה. בדקו הרשאות ונסו שוב.");}catch(JSONException ignored){}}
                reply.postMessage(response.toString());
            });
        setContentView(web);
        // Keep the WebView below Android 15 edge-to-edge status/navigation bars.
        web.setOnApplyWindowInsetsListener((v,insets)->{
            if(android.os.Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars());
                v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            } else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        web.loadUrl(ORIGIN+"/assets/index.html");
    }
    private static boolean isLocal(Uri u){
        return "https".equals(u.getScheme())&&"appassets.androidplatform.net".equals(u.getHost())
            && u.getPort()==-1 && u.getPath()!=null && u.getPath().startsWith("/assets/");
    }
    @Override protected void onResume(){super.onResume();if(controller!=null)controller.reconcile();}
    @Override protected void onDestroy(){if(web!=null)web.destroy();super.onDestroy();}
}
