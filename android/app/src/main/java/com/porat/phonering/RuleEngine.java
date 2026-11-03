package com.porat.phonering;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

/** Pure Java scheduling, independent of Android and testable with javac. */
public final class RuleEngine {
    public static final class Rule {
        final int start, end;
        final Set<Integer> days;
        final boolean enabled;
        public Rule(int start, int end, Set<Integer> days, boolean enabled) {
            if (start < 0 || start >= 1440 || end < 0 || end >= 1440 || start == end
                    || days.isEmpty() || days.size() > 7 || days.stream().anyMatch(d -> d < 0 || d > 6))
                throw new IllegalArgumentException("Invalid rule");
            this.start = start; this.end = end; this.days = Set.copyOf(days); this.enabled = enabled;
        }
        boolean active(int day, int minute) {
            if (!enabled) return false;
            return start < end ? days.contains(day) && minute >= start && minute < end
                : (days.contains(day) && minute >= start) || (days.contains((day + 6) % 7) && minute < end);
        }
    }
    public static boolean quiet(List<Rule> rules, boolean enabled, long pauseUntil, long now, ZoneId zone) {
        if (!enabled || now < pauseUntil) return false;
        ZonedDateTime date = Instant.ofEpochMilli(now).atZone(zone);
        int day = date.getDayOfWeek().getValue() % 7, minute = date.getHour() * 60 + date.getMinute();
        return rules.stream().anyMatch(r -> r.active(day, minute));
    }
    public static long next(List<Rule> rules, boolean enabled, long pauseUntil, long now, ZoneId zone) {
        if (!enabled) return 0;
        boolean current = quiet(rules, enabled, pauseUntil, now, zone);
        long candidate = pauseUntil > now && quiet(rules, enabled, pauseUntil, pauseUntil, zone) != current ? pauseUntil : 0;
        for (long t = (now / 60000 + 1) * 60000; t <= now + 8L * 86400000; t += 60000) {
            if (quiet(rules, enabled, pauseUntil, t, zone) != current) return candidate == 0 ? t : Math.min(t, candidate);
        }
        return candidate;
    }
}
