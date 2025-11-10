import javax.swing.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class HabitManager {
    private ArrayList<Habit> habits = new ArrayList<>();
    private final String FILE_NAME;
    private final String USER_FILE;
    private final String BADGE_FILE;

    private Map<String, java.util.Timer> timers = new HashMap<>();
    private Map<String, Integer> badgeCounts = new HashMap<>();

    private String username;

    public HabitManager(String username) {
        this.username = username;
        this.FILE_NAME = "habits_" + username + ".txt";
        this.USER_FILE = "user_" + username + ".txt";
        this.BADGE_FILE = "badges_" + username + ".txt";

        loadHabits();
        loadBadges();
        scheduleAllReminders();
    }

    // ================= CRUD =================
    public void addHabit(Habit h) {
        habits.add(h);
        saveHabits();
        scheduleReminder(h);
    }

    public List<Habit> getHabits() { return habits; }

    public void deleteHabit(int idx) {
        if (idx >= 0 && idx < habits.size()) {
            Habit h = habits.remove(idx);
            java.util.Timer t = timers.remove(h.getName());
            if (t != null) t.cancel();
            saveHabits();
        }
    }

    public void updateHabit(int idx, Habit h) {
        if (idx >= 0 && idx < habits.size()) {
            habits.set(idx, h);
            saveHabits();
            scheduleAllReminders();
        }
    }

    public void markHabitCompleteByIndex(int idx) {
        if (idx >= 0 && idx < habits.size()) {
            Habit h = habits.get(idx);
            markHabitComplete(h);
        }
    }

    public void markHabitComplete(Habit h) {
        h.markComplete();
        double progress = h.getProgress();

        // 🏅 Award badges only once per habit milestone
        int lastBadge = h.getHighestBadge();
        boolean awarded = false;

        if (progress >= 100.0 && lastBadge < 100) {
            addBadge("Gold");
            h.setHighestBadge(100);
            awarded = true;
            JOptionPane.showMessageDialog(null,
                "🏆 Congratulations!\nYou’ve earned the GOLD Badge!\nYou’ve completed this habit goal 100%!",
                "Badge Unlocked!", JOptionPane.INFORMATION_MESSAGE);
        } else if (progress >= 75.0 && lastBadge < 75) {
            addBadge("Silver");
            h.setHighestBadge(75);
            awarded = true;
            JOptionPane.showMessageDialog(null,
                "🎖️ Great Job!\nYou’ve earned the SILVER Badge!\nYou’ve achieved 75% of your goal!",
                "Badge Unlocked!", JOptionPane.INFORMATION_MESSAGE);
        } else if (progress >= 50.0 && lastBadge < 50) {
            addBadge("Bronze");
            h.setHighestBadge(50);
            awarded = true;
            JOptionPane.showMessageDialog(null,
                "🥉 Nice Work!\nYou’ve earned the BRONZE Badge!\nYou’re halfway through your habit goal!",
                "Badge Unlocked!", JOptionPane.INFORMATION_MESSAGE);
        } else if (progress >= 25.0 && lastBadge < 25) {
            addBadge("Starter");
            h.setHighestBadge(25);
            awarded = true;
            JOptionPane.showMessageDialog(null,
                "💪 Keep Going!\nYou’ve earned the STARTER Badge!\nYou’ve crossed 25% of your goal!",
                "Badge Unlocked!", JOptionPane.INFORMATION_MESSAGE);
        }

        // Always show a motivational quote after marking done (so quotes never disappear)
        JOptionPane.showMessageDialog(null,
                "Marked '" + h.getName() + "' done!\n\n" + MotivationQuotes.getRandomQuote(),
                "Habit Updated", JOptionPane.INFORMATION_MESSAGE);

        saveHabits();
    }

    // ================= BADGE SYSTEM =================
    public void addBadge(String badgeType) {
        badgeCounts.put(badgeType, badgeCounts.getOrDefault(badgeType, 0) + 1);
        saveBadges();
    }

    public void saveBadges() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(BADGE_FILE))) {
            for (Map.Entry<String, Integer> e : badgeCounts.entrySet()) {
                bw.write(e.getKey() + "," + e.getValue());
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving badges: " + e.getMessage());
        }
    }

    public void loadBadges() {
        badgeCounts.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(BADGE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length == 2) {
                    try { badgeCounts.put(p[0], Integer.parseInt(p[1])); } catch (Exception ignore) {}
                }
            }
        } catch (IOException ignored) {}
    }

    public Map<String, Integer> getBadgeCounts() {
        return badgeCounts;
    }

    // ================= PERSISTENCE =================
    public void saveHabits() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (Habit h : habits) {
                bw.write(h.toFileString());
                bw.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error saving habits: " + e.getMessage());
        }
    }

    public void loadHabits() {
        habits.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                Habit h = Habit.fromFileString(line);
                if (h != null) habits.add(h);
            }
        } catch (IOException ignored) {}
    }

    // ================= REMINDERS =================
    private long millisUntilNext(String hhmm) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("H:mm");
            LocalTime target = LocalTime.parse(hhmm, fmt);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime next = LocalDateTime.of(now.toLocalDate(), target);
            if (next.isBefore(now) || next.equals(now)) next = next.plusDays(1);
            return Duration.between(now, next).toMillis();
        } catch (DateTimeParseException e) {
            return -1;
        }
    }

    public void scheduleAllReminders() {
        for (java.util.Timer t : timers.values()) t.cancel();
        timers.clear();
        for (Habit h : habits) scheduleReminder(h);
    }

    public void scheduleReminder(Habit h) {
        String rt = h.getReminderTime();
        if (rt == null || rt.trim().isEmpty()) return;
        long delay = millisUntilNext(rt);
        if (delay < 0) return;

        java.util.Timer timer = new java.util.Timer(true);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                javax.swing.SwingUtilities.invokeLater(() -> showReminderDialog(h));
            }
        };
        timer.schedule(task, delay);
        timers.put(h.getName(), timer);
    }

    private void showReminderDialog(Habit h) {
        String msg = "Time for your habit:\n" + h.getName() + "\n[" + h.getFrequency() + "]";
        String[] options = {"Mark Done", "Remind in 10 min", "Skip"};
        int choice = JOptionPane.showOptionDialog(null, msg, "Habit Reminder",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);

        if (choice == 0) {
            markHabitComplete(h);
            // markHabitComplete already shows motivational quote, so no double message here
        } else if (choice == 1) {
            java.util.Timer t = new java.util.Timer(true);
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    javax.swing.SwingUtilities.invokeLater(() -> showReminderDialog(h));
                }
            }, 10L * 60 * 1000);
        }
    }

    // ================= REPORT =================
    public void exportReport() {
        String outFile = "habit_report_" + username + ".txt";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
            bw.write("HabitPal Report for " + username + "\n");
            bw.write("Generated on: " + LocalDateTime.now() + "\n\n");
            bw.write(String.format("%-20s %-8s %-10s %-8s %-10s %-10s\n",
                    "Name", "Freq", "Done", "Streak", "Progress", "Reminder"));
            bw.write("-----------------------------------------------------------------------\n");

            double totalProgress = 0;
            for (Habit h : habits) {
                totalProgress += h.getProgress();
                bw.write(String.format("%-20s %-8s %2d/%-7d %-8d %-9.1f %-10s\n",
                        h.getName(), h.getFrequency(),
                        h.getCompletedDays(), h.getTotalDays(),
                        h.getStreakCount(), h.getProgress(), h.getReminderTime()));
            }

            if (!habits.isEmpty()) {
                double avg = totalProgress / habits.size();
                bw.write("\nAverage Progress: " + String.format("%.1f%%", avg) + "\n");
            }
            bw.write("\nBadges Earned:\n");
            for (var e : badgeCounts.entrySet()) {
                bw.write(" - " + e.getKey() + ": " + e.getValue() + "\n");
            }

            JOptionPane.showMessageDialog(null, "Report exported to " + outFile);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Export error: " + e.getMessage());
        }
    }

    // ================= PROFILE =================
    public void saveUserProfile(String name, String email, String gender) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USER_FILE))) {
            bw.write(name + "," + email + "," + gender);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error saving profile: " + e.getMessage());
        }
    }

    public String[] loadUserProfile() {
        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line = br.readLine();
            if (line != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 3) return new String[]{parts[0], parts[1], parts[2]};
            }
        } catch (IOException ignored) {}
        return null;
    }
}