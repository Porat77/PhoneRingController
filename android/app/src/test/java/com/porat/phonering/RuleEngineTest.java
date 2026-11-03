package com.porat.phonering;
import org.junit.Test;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;
public class RuleEngineTest {
    @Test public void overnightWrap(){
        RuleEngine.Rule r=new RuleEngine.Rule(1380,420,Set.of(6),true);
        assertTrue(r.active(0,419));assertFalse(r.active(0,420));assertFalse(r.active(6,419));
    }
    @Test public void springAndFall(){
        ZoneId z=ZoneId.of("America/New_York");
        var rules=List.of(new RuleEngine.Rule(120,180,Set.of(0),true));
        long now=Instant.parse("2026-03-08T06:59:00Z").toEpochMilli();
        assertEquals(Instant.parse("2026-03-15T06:00:00Z").toEpochMilli(),RuleEngine.next(rules,true,0,now,z));
    }
}
