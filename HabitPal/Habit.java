import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Habit model with persistent list of completed dates.
 *
 * File format (CSV):
 * name,frequency,totalDays,completedDays,reminderTime,streakCount,highestBadge,lastCompletedDate,completedDates
 *
 * completedDates is a semicolon-separated list of ISO dates (yyyy-MM-dd).
 * Older files without the last fields will still load.
 */
public class Habit implements Serializable {
    private String name;
    private String frequency;          // Daily / Weekly
    private int totalDays;
    private int completedDays;
    private String reminderTime;       // "HH:mm" or empty

    // tracking fields
    private int streakCount;
    private LocalDate lastCompletedDate;

    // NEW: store all completed dates (so calendar can show exactly which days were done)
    // Use LinkedHashSet to keep insertion order (not strictly necessary)
    private Set<LocalDate> completedDates = new LinkedHashSet<>();

    // NEW: highest awarded badge for this habit (0, 25, 50, 75, 100)
    private int highestBadge = 0;

    public Habit(String name, String frequency, int totalDays, String reminderTime) {
        this.name = name;
        this.frequency = frequency;
        this.totalDays = totalDays;
        this.completedDays = 0;
        this.reminderTime = (reminderTime == null) ? "" : reminderTime;
        this.streakCount = 0;
        this.lastCompletedDate = null;
        this.completedDates = new LinkedHashSet<>();
        this.highestBadge = 0;
    }

    // getters
    public String getName() { return name; }
    public String getFrequency() { return frequency; }
    public int getTotalDays() { return totalDays; }
    public int getCompletedDays() { return completedDays; }
    public String getReminderTime() { return reminderTime; }
    public int getStreakCount() { return streakCount; }
    public LocalDate getLastCompletedDate() { return lastCompletedDate; }
    public int getHighestBadge() { return highestBadge; }

    // expose a read-only copy of completedDates
    public Set<LocalDate> getCompletedDates() {
        return Collections.unmodifiableSet(completedDates);
    }

    // convenience: check if done on a specific day
    public boolean isDoneOn(LocalDate day) {
        return completedDates.contains(day);
    }

    // setters
    public void setName(String n) { this.name = n; }
    public void setFrequency(String f) { this.frequency = f; }
    public void setTotalDays(int t) { this.totalDays = t; }
    public void setReminderTime(String rt) { this.reminderTime = rt; }
    public void setHighestBadge(int val) { this.highestBadge = val; }

    /**
     * Called when the user marks this habit complete.
     * Adds today's date to completedDates, updates completedDays,
     * recomputes streakCount and lastCompletedDate.
     */
    public void markComplete() {
        LocalDate today = LocalDate.now();

        // if today's date was not already recorded, add it and increment completedDays
        if (!completedDates.contains(today)) {
            completedDates.add(today);
            completedDays = completedDates.size();
        }

        // update lastCompletedDate to the most recent completed date
        // (completedDates is not necessarily ordered by date, so compute max)
        if (!completedDates.isEmpty()) {
            lastCompletedDate = completedDates.stream().max(LocalDate::compareTo).orElse(today);
        } else {
            lastCompletedDate = null;
        }

        // recompute streak: count consecutive days ending at lastCompletedDate
        recomputeStreak();
    }

    /** Recomputes streakCount from completedDates and lastCompletedDate */
    private void recomputeStreak() {
        streakCount = 0;
        if (lastCompletedDate == null) return;

        LocalDate cursor = lastCompletedDate;
        while (completedDates.contains(cursor)) {
            streakCount++;
            cursor = cursor.minusDays(1);
        }
    }

    /** Calculates completion percentage */
    public double getProgress() {
        if (totalDays == 0) return 0;
        return (completedDays * 100.0) / totalDays;
    }

    @Override
    public String toString() {
        return name + " (" + frequency + ") - " + completedDays + "/" + totalDays +
                " done (" + String.format("%.1f", getProgress()) + "%)  [" + reminderTime + "]";
    }

    /** Converts to a line for saving */
    public String toFileString() {
        // completedDates serialized as semicolon-separated ISO dates
        String dates = completedDates.stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining(";"));
        // New format: name,freq,total,completed,reminder,streak,highestBadge,lastCompletedDate,dates
        return name + "," + frequency + "," + totalDays + "," + completedDays + "," +
               reminderTime + "," + streakCount + "," + highestBadge + "," +
               (lastCompletedDate != null ? lastCompletedDate.toString() : "") + "," +
               dates;
    }

    /** Reconstructs a Habit from a line; tolerant of older formats */
    public static Habit fromFileString(String line) {
        String[] p = line.split(",", -1);
        try {
            String name = p.length > 0 ? p[0] : "";
            String freq = p.length > 1 ? p[1] : "Daily";
            int total = p.length > 2 && !p[2].isEmpty() ? Integer.parseInt(p[2]) : 0;
            String reminder = p.length > 4 ? p[4] : "";
            Habit h = new Habit(name, freq, total, reminder);

            if (p.length > 3 && !p[3].isEmpty()) {
                try { h.completedDays = Integer.parseInt(p[3]); } catch (Exception ex) { h.completedDays = 0; }
            }

            // backward-compatible parsing:
            // if new format includes highestBadge at position 6:
            // p[5] -> streakCount, p[6] -> highestBadge, p[7] -> lastCompletedDate, p[8] -> dates
            if (p.length > 5 && !p[5].isEmpty()) {
                try { h.streakCount = Integer.parseInt(p[5]); } catch (Exception ex) { h.streakCount = 0; }
            }

            if (p.length > 6 && !p[6].isEmpty()) {
                try { h.highestBadge = Integer.parseInt(p[6]); } catch (Exception ex) { h.highestBadge = 0; }
            }

            if (p.length > 7 && !p[7].isEmpty()) {
                try { h.lastCompletedDate = LocalDate.parse(p[7]); } catch (Exception ex) { h.lastCompletedDate = null; }
            }

            // parse completedDates if present (last field)
            if (p.length > 8 && !p[8].isEmpty()) {
                String dates = p[8];
                String[] tokens = dates.split(";");
                for (String t : tokens) {
                    try {
                        LocalDate d = LocalDate.parse(t);
                        h.completedDates.add(d);
                    } catch (Exception ignore) {}
                }
                // ensure completedDays matches set size
                h.completedDays = h.completedDates.size();
                // recompute lastCompletedDate and streak in case they were inconsistent
                if (!h.completedDates.isEmpty()) {
                    h.lastCompletedDate = h.completedDates.stream().max(LocalDate::compareTo).orElse(h.lastCompletedDate);
                    h.recomputeStreak();
                }
            } else {
                // older formats: try to ensure consistency
                if (h.lastCompletedDate != null && h.completedDays == 0) {
                    // leave as-is; user may not have completedDates list
                }
            }

            return h;
        } catch (Exception e) {
            return null;
        }
    }
}