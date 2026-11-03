package com.porat.phonering;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.service.notification.Condition;
import android.service.notification.ZenPolicy;
import org.json.*;
import java.time.ZoneId;
import java.util.*;

final class QuietController {
    static final String TICK = "com.porat.phonering.TICK";
    static final Uri CONDITION = Uri.parse("condition://com.porat.phonering/schedule");
    private final Context context;
    private final android.content.SharedPreferences prefs;
    private final NotificationManager notifications;
    private final AlarmManager alarms;
    QuietController(Context c) {
        context = c.getApplicationContext();
        prefs = context.getSharedPreferences("phone-ring", Context.MODE_PRIVATE);
        notifications = context.getSystemService(NotificationManager.class);
        alarms = context.getSystemService(AlarmManager.class);
    }
    JSONObject load() throws JSONException {
        return validate(new JSONObject(prefs.getString("state", "{\"version\":1,\"enabled\":false,\"pauseUntil\":0,\"rules\":[]}")));
    }
    JSONObject validate(JSONObject s) throws JSONException {
        if (s.getInt("version") != 1 || !(s.get("enabled") instanceof Boolean)
                || !(s.get("pauseUntil") instanceof Number) || s.getDouble("pauseUntil") != s.getLong("pauseUntil")
                || s.getLong("pauseUntil") < 0 || s.getLong("pauseUntil") > 9007199254740991L)
            throw new JSONException("Invalid state");
        JSONArray rules = s.getJSONArray("rules");
        if (rules.length() > 100) throw new JSONException("Too many rules");
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < rules.length(); i++) {
            JSONObject r = rules.getJSONObject(i);
            String id = r.getString("id"), name = r.getString("name");
            if (id.isEmpty() || id.length() > 80 || !ids.add(id) || name.trim().isEmpty()
                    || name.length() > 40 || !(r.get("enabled") instanceof Boolean)) throw new JSONException("Invalid rule");
            int start = minute(r.getString("start")), end = minute(r.getString("end"));
            Set<Integer> days = new HashSet<>();
            JSONArray a = r.getJSONArray("days");
            for (int j = 0; j < a.length(); j++) {
                if (!(a.get(j) instanceof Number) || a.getDouble(j) != a.getInt(j) || !days.add(a.getInt(j)))
                    throw new JSONException("Invalid days");
            }
            new RuleEngine.Rule(start, end, days, r.getBoolean("enabled"));
        }
        return s;
    }
    private int minute(String s) throws JSONException {
        if (!s.matches("([01][0-9]|2[0-3]):[0-5][0-9]")) throw new JSONException("Invalid time");
        return Integer.parseInt(s.substring(0,2))*60 + Integer.parseInt(s.substring(3));
    }
    private List<RuleEngine.Rule> rules(JSONObject s) throws JSONException {
        List<RuleEngine.Rule> list = new ArrayList<>();
        JSONArray a = s.getJSONArray("rules");
        for (int i=0;i<a.length();i++) {
            JSONObject r = a.getJSONObject(i); Set<Integer> days = new HashSet<>();
            JSONArray d=r.getJSONArray("days");for(int j=0;j<d.length();j++)days.add(d.getInt(j));
            list.add(new RuleEngine.Rule(minute(r.getString("start")),minute(r.getString("end")),days,r.getBoolean("enabled")));
        }
        return list;
    }
    synchronized void save(JSONObject s) throws JSONException {
        validate(s);
        if (!prefs.edit().putString("state",s.toString()).commit()) throw new IllegalStateException("Could not save settings");
        reconcile();
    }
    boolean access() { return notifications.isNotificationPolicyAccessGranted(); }
    boolean exact() { return Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms(); }
    private AutomaticZenRule rule() {
        String id = prefs.getString("ruleId", null);
        return id == null || !access() ? null : notifications.getAutomaticZenRule(id);
    }
    void connect() {
        if (!access()) throw new IllegalStateException("Notification policy access is required");
        if (rule() == null) {
            ZenPolicy.Builder builder = new ZenPolicy.Builder().allowAlarms(true).allowMedia(true).allowSystem(true)
                .allowCalls(ZenPolicy.PEOPLE_TYPE_NONE).allowMessages(ZenPolicy.PEOPLE_TYPE_NONE)
                .allowRepeatCallers(false).allowEvents(false).allowReminders(false);
            if(Build.VERSION.SDK_INT>=30)builder.allowConversations(ZenPolicy.CONVERSATION_SENDERS_NONE);
            ZenPolicy policy=builder.build();
            AutomaticZenRule r = new AutomaticZenRule("זמן לשקט",
                new ComponentName(context, QuietConditionService.class),
                new ComponentName(context, MainActivity.class), CONDITION, policy,
                NotificationManager.INTERRUPTION_FILTER_PRIORITY, true);
            String id = notifications.addAutomaticZenRule(r);
            if (id == null || !prefs.edit().putString("ruleId", id).commit())
                throw new IllegalStateException("Could not create quiet rule");
        }
        reconcile();
    }
    synchronized void reconcile() {
        PendingIntent tick = PendingIntent.getBroadcast(context, 0,
            new Intent(context, ScheduleReceiver.class).setAction(TICK),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarms.cancel(tick);
        try {
            JSONObject s=load();long now=System.currentTimeMillis();
            List<RuleEngine.Rule> list=rules(s);
            boolean quiet=RuleEngine.quiet(list,s.getBoolean("enabled"),s.getLong("pauseUntil"),now,ZoneId.systemDefault());
            AutomaticZenRule own=rule();
            if (own != null) {
                notifications.setAutomaticZenRuleState(prefs.getString("ruleId", ""),
                    new Condition(CONDITION, "Scheduled quiet hours", quiet ? Condition.STATE_TRUE : Condition.STATE_FALSE));
            }
            long next=RuleEngine.next(list,s.getBoolean("enabled"),s.getLong("pauseUntil"),now,ZoneId.systemDefault());
            // Daily safety reconciliation also covers clock/time-zone changes missed by OEMs.
            next=next==0 ? now+86400000 : Math.min(next,now+86400000);
            if(s.getBoolean("enabled") && access() && own!=null) {
                if(exact()) {
                    try { alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,tick); }
                    catch (SecurityException e) { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,tick); }
                } else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,tick);
            }
            prefs.edit().remove("error").apply();
        } catch (Exception e) {
            prefs.edit().putString("error","Android could not apply the schedule. Check permissions and reopen the app.").apply();
        }
    }
    JSONObject result() throws JSONException {
        JSONObject system=new JSONObject();system.put("access",access());system.put("exact",exact());
        AutomaticZenRule own=rule();system.put("ruleExists",own!=null);system.put("ruleEnabled",own!=null&&own.isEnabled());
        int filter=notifications.getCurrentInterruptionFilter();
        system.put("dnd",filter!=NotificationManager.INTERRUPTION_FILTER_ALL && filter!=NotificationManager.INTERRUPTION_FILTER_UNKNOWN);
        system.put("error",prefs.getString("error",""));
        return new JSONObject().put("state",load()).put("system",system);
    }
}
