import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

public class HabitPalGUI extends JFrame {
    private HabitManager manager;

    public HabitPalGUI(String username) {
        manager = new HabitManager(username);
        setTitle("HabitPal — Smart Habit Tracker");
        setSize(640, 420);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        String[] profile = manager.loadUserProfile();
        if (profile == null) {
            ProfileDialog pd = new ProfileDialog(this, manager);
            pd.setVisible(true);
        }

        initUI();
    }

    private void initUI() {
        JPanel top = new JPanel(new BorderLayout());
        JLabel title = new JLabel("HabitPal — Build Good Habits", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        top.add(title, BorderLayout.CENTER);

        JPanel mainButtons = new JPanel(new GridLayout(2, 3, 10, 10));
        JButton addBtn = new JButton("Add Habit");
        JButton viewBtn = new JButton("View Habits");
        JButton badgesBtn = new JButton("View Badges");
        JButton exportBtn = new JButton("Export Report");
        JButton refreshBtn = new JButton("Refresh Reminders");
        JButton profileBtn = new JButton("Profile");
        JButton exitBtn = new JButton("Save & Exit");

        mainButtons.add(addBtn);
        mainButtons.add(viewBtn);
        mainButtons.add(badgesBtn);
        mainButtons.add(exportBtn);
        mainButtons.add(refreshBtn);
        mainButtons.add(profileBtn);
        
        // === Exit button panel (centered single button) ===
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(exitBtn);
        
        // === Combine both panels ===
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(mainButtons, BorderLayout.CENTER);
        centerPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        // === Add title and center panels to frame ===
        add(top, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        // Button actions
        addBtn.addActionListener(e -> {
            AddHabitDialog ad = new AddHabitDialog(this, manager);
            ad.setVisible(true);
        });

        viewBtn.addActionListener(e -> {
            ViewHabitsDialog vd = new ViewHabitsDialog(this, manager);
            vd.setVisible(true);
        });

        badgesBtn.addActionListener(e -> {
        StringBuilder sb = new StringBuilder("🏅 Your Earned Badges:\n\n");
        Map<String, Integer> badges = manager.getBadgeCounts();
        if (badges.isEmpty()) {
            sb.append("No badges earned yet.\nKeep going!");
        } else {
            for (var entry : badges.entrySet()) {
                sb.append(entry.getKey()).append(" – ").append(entry.getValue()).append("\n");
            }
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Badges", JOptionPane.INFORMATION_MESSAGE);});

        exportBtn.addActionListener(e -> manager.exportReport());

        refreshBtn.addActionListener(e -> {
            manager.scheduleAllReminders();
            JOptionPane.showMessageDialog(this, "Reminders refreshed successfully!");
        });

        profileBtn.addActionListener(e -> {
            ProfileDialog pd = new ProfileDialog(this, manager);
            pd.setVisible(true);
        });

        exitBtn.addActionListener(e -> {
            manager.saveHabits();
            JOptionPane.showMessageDialog(this, "Data saved successfully. Exiting HabitPal.");
            System.exit(0);
        });
    }

    // --------------------- Profile Dialog ---------------------
    static class ProfileDialog extends JDialog {
        JTextField nameF, emailF;
        JComboBox<String> genderC;

        public ProfileDialog(JFrame parent, HabitManager manager) {
            super(parent, "User Profile", true);
            setLayout(new GridLayout(4, 2, 8, 8));

            add(new JLabel("Name:"));
            nameF = new JTextField();
            add(nameF);
            add(new JLabel("Email:"));
            emailF = new JTextField();
            add(emailF);
            add(new JLabel("Gender:"));
            genderC = new JComboBox<>(new String[]{"Male", "Female", "Other"});
            add(genderC);
            JButton save = new JButton("Save");
            add(new JLabel());
            add(save);

            setSize(350, 180);
            setLocationRelativeTo(parent);

            String[] prof = manager.loadUserProfile();
            if (prof != null) {
                nameF.setText(prof[0]);
                emailF.setText(prof[1]);
                genderC.setSelectedItem(prof[2]);
            }

            save.addActionListener(e -> {
                String n = nameF.getText().trim();
                String em = emailF.getText().trim();
                String g = (String) genderC.getSelectedItem();
                if (n.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Name is required.");
                    return;
                }
                manager.saveUserProfile(n, em, g);
                JOptionPane.showMessageDialog(this, "Profile saved successfully.");
                dispose();
            });
        }
    }

    // --------------------- Add Habit Dialog ---------------------
    static class AddHabitDialog extends JDialog {
        public AddHabitDialog(JFrame parent, HabitManager manager) {
            super(parent, "Add Habit", true);
            setLayout(new GridLayout(6, 2, 6, 6));

            add(new JLabel("Habit name:"));
            JTextField nameF = new JTextField();
            add(nameF);

            add(new JLabel("Frequency:"));
            JComboBox<String> freqC = new JComboBox<>(new String[]{"Daily", "Weekly"});
            add(freqC);

            add(new JLabel("Total days to track:"));
            JSpinner daysS = new JSpinner(new SpinnerNumberModel(7, 1, 365, 1));
            add(daysS);

            add(new JLabel("Reminder time (H:mm):"));
            JTextField timeF = new JTextField();
            add(timeF);

            JButton addBtn = new JButton("Add");
            add(new JLabel());
            add(addBtn);

            setSize(480, 260);
            setLocationRelativeTo(parent);

            addBtn.addActionListener(e -> {
                String nm = nameF.getText().trim();
                String freq = (String) freqC.getSelectedItem();
                int days = (Integer) daysS.getValue();
                String rt = timeF.getText().trim();

                if (nm.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter a habit name.");
                    return;
                }

                Habit h = new Habit(nm, freq, days, rt);
                manager.addHabit(h);
                JOptionPane.showMessageDialog(this, "Habit added successfully!");
                dispose();
            });
        }
    }

    // --------------------- View Habits Dialog ---------------------
    static class ViewHabitsDialog extends JDialog {
        DefaultTableModel model;
        JTable table;
        HabitManager manager;

        public ViewHabitsDialog(JFrame parent, HabitManager manager) {
            super(parent, "View Habits", true);
            this.manager = manager;

            String[] cols = {"#", "Name", "Freq", "Progress", "Streak", "Reminder"};
            model = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int row, int col) { return false; }
            };

            table = new JTable(model);
            refreshTable();

            JScrollPane sp = new JScrollPane(table);
            add(sp, BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            JButton markBtn = new JButton("Mark Done");
            JButton delBtn = new JButton("Delete");
            JButton editBtn = new JButton("Edit");
            JButton calBtn = new JButton("View Progress Calendar");
            JButton closeBtn = new JButton("Close");

            bottom.add(markBtn);
            bottom.add(delBtn);
            bottom.add(editBtn);
            bottom.add(calBtn);
            bottom.add(closeBtn);
            add(bottom, BorderLayout.SOUTH);

            setSize(600, 350);
            setLocationRelativeTo(parent);

            markBtn.addActionListener(e -> {
                int r = table.getSelectedRow();
                if (r == -1) { JOptionPane.showMessageDialog(this, "Select a row first."); return; }
                manager.markHabitCompleteByIndex(r);
                refreshTable();
            });

            delBtn.addActionListener(e -> {
                int r = table.getSelectedRow();
                if (r == -1) { JOptionPane.showMessageDialog(this, "Select a row first."); return; }
                manager.deleteHabit(r);
                refreshTable();
            });

            editBtn.addActionListener(e -> {
                int r = table.getSelectedRow();
                if (r == -1) { JOptionPane.showMessageDialog(this, "Select a row first."); return; }
                Habit h = manager.getHabits().get(r);
                EditHabitDialog ed = new EditHabitDialog(parent, manager, h, r);
                ed.setVisible(true);
                refreshTable();
            });

            calBtn.addActionListener(e -> {
                int r = table.getSelectedRow();
                if (r == -1) { JOptionPane.showMessageDialog(this, "Select a habit to view calendar."); return; }
                Habit h = manager.getHabits().get(r);
                ProgressCalendarDialog cd = new ProgressCalendarDialog(parent, h);
                cd.setVisible(true);
            });

            closeBtn.addActionListener(e -> dispose());
        }

        private void refreshTable() {
            model.setRowCount(0);
            java.util.List<Habit> list = manager.getHabits();
            for (int i = 0; i < list.size(); i++) {
                Habit h = list.get(i);
                model.addRow(new Object[]{
                        i + 1,
                        h.getName(),
                        h.getFrequency(),
                        String.format("%.1f%% (%d/%d)", h.getProgress(), h.getCompletedDays(), h.getTotalDays()),
                        h.getStreakCount(),
                        h.getReminderTime()
                });
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String username = JOptionPane.showInputDialog(null,
                    "Enter your profile name (for saving habits):", "Login", JOptionPane.PLAIN_MESSAGE);
            if (username == null || username.trim().isEmpty()) username = "default";
            HabitPalGUI gui = new HabitPalGUI(username);
            gui.setVisible(true);
        });
    }
}