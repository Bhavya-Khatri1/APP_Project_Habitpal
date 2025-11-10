import javax.swing.*;
import java.awt.*;

public class EditHabitDialog extends JDialog {
    private HabitManager manager;
    private Habit habit;
    private int index;

    public EditHabitDialog(JFrame parent, HabitManager manager, Habit habit, int index) {
        super(parent, "Edit Habit", true);
        this.manager = manager;
        this.habit = habit;
        this.index = index;

        setLayout(new GridLayout(6, 2, 8, 8));

        add(new JLabel("Habit name:"));
        JTextField nameF = new JTextField(habit.getName());
        add(nameF);

        add(new JLabel("Frequency:"));
        JComboBox<String> freqC = new JComboBox<>(new String[]{"Daily", "Weekly"});
        freqC.setSelectedItem(habit.getFrequency());
        add(freqC);

        add(new JLabel("Total days to track:"));
        JSpinner daysS = new JSpinner(new SpinnerNumberModel(habit.getTotalDays(), 1, 365, 1));
        add(daysS);

        add(new JLabel("Reminder time (H:mm):"));
        JTextField timeF = new JTextField(habit.getReminderTime());
        add(timeF);

        JButton saveBtn = new JButton("Save Changes");
        JButton cancelBtn = new JButton("Cancel");
        add(saveBtn);
        add(cancelBtn);

        setSize(400, 260);
        setLocationRelativeTo(parent);

        // Action handlers
        saveBtn.addActionListener(e -> {
            String nm = nameF.getText().trim();
            String freq = (String) freqC.getSelectedItem();
            int total = (Integer) daysS.getValue();
            String time = timeF.getText().trim();

            if (nm.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Habit name cannot be empty.");
                return;
            }

            habit.setName(nm);
            habit.setFrequency(freq);
            habit.setTotalDays(total);
            habit.setReminderTime(time);

            manager.updateHabit(index, habit);
            JOptionPane.showMessageDialog(this, "Habit updated successfully!");
            dispose();
        });

        cancelBtn.addActionListener(e -> dispose());
    }
}