package com.porat.phonering;
import java.time.*;
import java.util.*;
public final class RuleEngineCheck {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        RuleEngine.Rule r=new RuleEngine.Rule(1380,420,Set.of(6),true);
        check(r.active(6,1380));check(r.active(0,419));check(!r.active(0,420));check(!r.active(6,419));
        ZoneId zone=ZoneId.of("America/New_York");
        var spring=List.of(new RuleEngine.Rule(120,180,Set.of(0),true));
        long now=Instant.parse("2026-03-08T06:59:00Z").toEpochMilli();
        check(RuleEngine.next(spring,true,0,now,zone)==Instant.parse("2026-03-15T06:00:00Z").toEpochMilli());
        var fall=List.of(new RuleEngine.Rule(90,105,Set.of(0),true));
        check(RuleEngine.next(fall,true,0,Instant.parse("2026-11-01T05:50:00Z").toEpochMilli(),zone)==Instant.parse("2026-11-01T06:30:00Z").toEpochMilli());
        var all=List.of(new RuleEngine.Rule(0,720,Set.of(0,1,2,3,4,5,6),true),new RuleEngine.Rule(720,0,Set.of(0,1,2,3,4,5,6),true));
        check(RuleEngine.next(all,true,0,now,zone)==0);
        check(!RuleEngine.quiet(all,false,0,now,zone));
        check(RuleEngine.next(all,true,now+12345,now,zone)==now+12345);
        System.out.println("PASS Java: midnight, week wrap, disabled, DST, continuous quiet, exact pause expiry");
    }
}
