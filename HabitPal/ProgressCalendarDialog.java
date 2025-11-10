import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.time.temporal.ChronoUnit;

public class ProgressCalendarDialog extends JDialog {
    private Habit habit;

    public ProgressCalendarDialog(JFrame parent, Habit habit) {
        super(parent, "Weekly Progress – " + habit.getName(), true);
        this.habit = habit;

        setLayout(new BorderLayout(10, 10));

        // Add a title label for the month/week display
        JLabel header = new JLabel(
                "Current Week Progress (" + LocalDate.now().getMonth() + ")",
                SwingConstants.CENTER
        );
        header.setFont(new Font("SansSerif", Font.BOLD, 15));
        header.setForeground(new Color(0, 102, 204));
        add(header, BorderLayout.NORTH);

        // Create grid for days + marks
        JPanel grid = new JPanel(new GridLayout(2, 7, 5, 5));
        JLabel[] dayLabels = new JLabel[7];
        JLabel[] markLabels = new JLabel[7];

        // Days of week (Sunday → Saturday)
        DayOfWeek[] days = {
                DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
        };

        // Add day names on top row
        for (int i = 0; i < 7; i++) {
            dayLabels[i] = new JLabel(days[i].toString().substring(0, 3), SwingConstants.CENTER);
            dayLabels[i].setFont(new Font("SansSerif", Font.BOLD, 13));
            grid.add(dayLabels[i]);
        }

        // Fixed streak/mark logic
        LocalDate today = LocalDate.now();
        LocalDate last = habit.getLastCompletedDate();

        // Calculate the Sunday of the current week
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() % 7);

        for (int i = 0; i < 7; i++) {
            LocalDate day = startOfWeek.plusDays(i);
            String mark = "✘";

            // Mark for completed streak days including today
            if (habit.isDoneOn(day)) {
                    mark = "✔";
            }

            markLabels[i] = new JLabel(mark, SwingConstants.CENTER);
            markLabels[i].setFont(new Font("SansSerif", Font.BOLD, 18));
            markLabels[i].setForeground(mark.equals("✔") ? new Color(0, 150, 0) : Color.RED);
            grid.add(markLabels[i]);
        }

        // ✅ Info section below the grid
        JPanel info = new JPanel(new GridLayout(3, 1));
        info.add(new JLabel("Streak: " + habit.getStreakCount() + " days"));
        info.add(new JLabel("Last done: " +
                (habit.getLastCompletedDate() != null ? habit.getLastCompletedDate() : "N/A")));
        info.add(new JLabel("Progress: " + String.format("%.1f%%", habit.getProgress())));

        add(grid, BorderLayout.CENTER);
        add(info, BorderLayout.SOUTH);

        setSize(500, 260);
        setLocationRelativeTo(parent);
    }
}