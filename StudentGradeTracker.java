import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;

/**
 * Student Grade Tracker (Swing GUI)
 *
 * At startup the user enters the number of subjects and their names.
 * Each student then gets one mark per subject.
 *
 * Features:
 *  - Add / remove students and enter marks for every subject
 *  - Per-student total, average, highest, lowest mark and Grade
 *  - Class-wide average, highest and lowest mark
 *  - Highest mark in each subject (with the student who scored it)
 *  - Summary report of all students
 *
 * Data is stored in ArrayLists.
 */
public class StudentGradeTracker extends JFrame {

    // ---------- Student model ----------
    static class Student {
        private final String name;
        private final ArrayList<Double> marks = new ArrayList<>(); // one per subject, same order as subjects

        Student(String name) {
            this.name = name;
        }

        String getName() {
            return name;
        }

        ArrayList<Double> getMarks() {
            return marks;
        }

        double getTotal() {
            double sum = 0;
            for (double m : marks) sum += m;
            return sum;
        }

        double getAverage() {
            return marks.isEmpty() ? 0 : getTotal() / marks.size();
        }

        double getHighest() {
            double max = marks.get(0);
            for (double m : marks) if (m > max) max = m;
            return max;
        }

        double getLowest() {
            double min = marks.get(0);
            for (double m : marks) if (m < min) min = m;
            return min;
        }

        String getGrade() {
            double avg = getAverage();
            if (avg >= 90) return "A";
            if (avg >= 80) return "B";
            if (avg >= 70) return "C";
            if (avg >= 60) return "D";
            return "F";
        }
    }

    // ---------- Application state ----------
    private final ArrayList<String> subjects;
    private final ArrayList<Student> students = new ArrayList<>();

    // ---------- GUI components ----------
    private final JTextField nameField = new JTextField(12);
    private final ArrayList<JTextField> markFields = new ArrayList<>();
    private final DefaultTableModel studentModel;
    private final JTable studentTable;
    private final DefaultTableModel topperModel;
    private final JLabel statsLabel = new JLabel();

    // ---------- Entry point ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // fall back to the default look and feel
            }
            ArrayList<String> subjects = askForSubjects();
            if (subjects == null) {
                System.exit(0);
            }
            new StudentGradeTracker(subjects).setVisible(true);
        });
    }

    // ---------- Startup dialogs: number of subjects, then their names ----------
    private static ArrayList<String> askForSubjects() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(3, 1, 15, 1));
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Number of subjects (1-15):"));
        panel.add(spinner);
        int result = JOptionPane.showConfirmDialog(null, panel, "Student Grade Tracker - Setup",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return null;

        int count = (Integer) spinner.getValue();
        ArrayList<String> names = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            while (true) {
                String name = JOptionPane.showInputDialog(null, "Enter name of subject " + i + " of " + count + ":",
                        "Student Grade Tracker - Setup", JOptionPane.QUESTION_MESSAGE);
                if (name == null) return null; // cancelled
                name = name.trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Subject name cannot be empty.");
                } else if (containsIgnoreCase(names, name)) {
                    JOptionPane.showMessageDialog(null, "That subject was already entered.");
                } else {
                    names.add(name);
                    break;
                }
            }
        }
        return names;
    }

    // ---------- Build the window ----------
    private StudentGradeTracker(ArrayList<String> subjects) {
        super("Student Grade Tracker");
        this.subjects = subjects;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- Input form (north) ---
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new TitledBorder("Add a student (marks out of 100)"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 5, 3, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        int col = 0;
        c.gridx = col; c.gridy = 0;
        form.add(new JLabel("Student name"), c);
        c.gridy = 1;
        form.add(nameField, c);
        col++;

        for (String subject : subjects) {
            JTextField f = new JTextField(5);
            markFields.add(f);
            c.gridx = col; c.gridy = 0;
            form.add(new JLabel(subject), c);
            c.gridy = 1;
            form.add(f, c);
            f.addActionListener(e -> addStudent()); // Enter key adds the student
            col++;
        }
        nameField.addActionListener(e -> markFields.get(0).requestFocusInWindow());

        JButton addButton = new JButton("Add Student");
        addButton.addActionListener(e -> addStudent());
        c.gridx = col; c.gridy = 1; c.weightx = 0;
        form.add(addButton, c);
        add(form, BorderLayout.NORTH);

        // --- Students table (center) ---
        ArrayList<String> columns = new ArrayList<>();
        columns.add("Name");
        columns.addAll(subjects);
        columns.add("Total");
        columns.add("Average");
        columns.add("Highest");
        columns.add("Lowest");
        columns.add("Grade");

        studentModel = new DefaultTableModel(columns.toArray(), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        studentTable = new JTable(studentModel);
        studentTable.setRowHeight(24);
        studentTable.setFillsViewportHeight(true);
        studentTable.setAutoCreateRowSorter(true);
        centerAllButFirstColumn(studentTable);
        styleHeader(studentTable);

        JPanel center = new JPanel(new BorderLayout(5, 5));
        center.setBorder(new TitledBorder("Student summary report"));
        center.add(new JScrollPane(studentTable), BorderLayout.CENTER);

        JButton removeButton = new JButton("Remove Selected");
        removeButton.addActionListener(e -> removeSelected());
        JButton clearButton = new JButton("Clear All");
        clearButton.addActionListener(e -> clearAll());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(removeButton);
        buttons.add(clearButton);
        center.add(buttons, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        // --- Class stats + highest mark per subject (south) ---
        topperModel = new DefaultTableModel(new Object[]{"Subject", "Highest Mark", "Scored By"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable topperTable = new JTable(topperModel);
        topperTable.setRowHeight(22);
        centerAllButFirstColumn(topperTable);
        styleHeader(topperTable);
        JScrollPane topperScroll = new JScrollPane(topperTable);
        topperScroll.setPreferredSize(new Dimension(100, Math.min(190, 30 + 22 * subjects.size())));

        JPanel south = new JPanel(new BorderLayout(5, 5));
        south.setBorder(new TitledBorder("Highest marks by subject & class statistics"));
        south.add(topperScroll, BorderLayout.CENTER);
        statsLabel.setBorder(new EmptyBorder(4, 4, 4, 4));
        south.add(statsLabel, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        refresh();
        setSize(Math.max(760, 150 + 90 * subjects.size() + 360), 640);
        setLocationRelativeTo(null);
    }

    // ---------- Actions ----------
    private void addStudent() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError("Please enter the student's name.");
            nameField.requestFocusInWindow();
            return;
        }
        if (findStudent(name) != null) {
            showError("A student named \"" + name + "\" already exists.");
            return;
        }

        Student s = new Student(name);
        for (int i = 0; i < subjects.size(); i++) {
            JTextField f = markFields.get(i);
            String text = f.getText().trim();
            double mark;
            try {
                mark = Double.parseDouble(text);
            } catch (NumberFormatException ex) {
                showError("Please enter a valid number for " + subjects.get(i) + ".");
                f.requestFocusInWindow();
                return;
            }
            if (mark < 0 || mark > 100) {
                showError("Marks for " + subjects.get(i) + " must be between 0 and 100.");
                f.requestFocusInWindow();
                return;
            }
            s.getMarks().add(mark);
        }

        students.add(s);
        nameField.setText("");
        for (JTextField f : markFields) f.setText("");
        nameField.requestFocusInWindow();
        refresh();
    }

    private void removeSelected() {
        int viewRow = studentTable.getSelectedRow();
        if (viewRow < 0) {
            showError("Select a student in the table first.");
            return;
        }
        int modelRow = studentTable.convertRowIndexToModel(viewRow);
        Student s = students.get(modelRow);
        int ok = JOptionPane.showConfirmDialog(this, "Remove " + s.getName() + "?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            students.remove(modelRow);
            refresh();
        }
    }

    private void clearAll() {
        if (students.isEmpty()) return;
        int ok = JOptionPane.showConfirmDialog(this, "Remove all students?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            students.clear();
            refresh();
        }
    }

    // ---------- Refresh tables and statistics ----------
    private void refresh() {
        // students table
        studentModel.setRowCount(0);
        double classSum = 0;
        int classCount = 0;
        double classHigh = Double.NEGATIVE_INFINITY;
        double classLow = Double.POSITIVE_INFINITY;
        String highName = "";
        String lowName = "";

        for (Student s : students) {
            ArrayList<Object> row = new ArrayList<>();
            row.add(s.getName());
            for (double m : s.getMarks()) {
                row.add(fmt(m));
                classSum += m;
                classCount++;
            }
            row.add(fmt(s.getTotal()));
            row.add(fmt(s.getAverage()));
            row.add(fmt(s.getHighest()));
            row.add(fmt(s.getLowest()));
            row.add(s.getGrade());
            studentModel.addRow(row.toArray());

            if (s.getHighest() > classHigh) {
                classHigh = s.getHighest();
                highName = s.getName();
            }
            if (s.getLowest() < classLow) {
                classLow = s.getLowest();
                lowName = s.getName();
            }
        }

        // highest mark in each subject (ties list every student)
        topperModel.setRowCount(0);
        for (int sub = 0; sub < subjects.size(); sub++) {
            if (students.isEmpty()) {
                topperModel.addRow(new Object[]{subjects.get(sub), "-", "-"});
                continue;
            }
            double max = Double.NEGATIVE_INFINITY;
            for (Student s : students) {
                if (s.getMarks().get(sub) > max) max = s.getMarks().get(sub);
            }
            StringBuilder names = new StringBuilder();
            for (Student s : students) {
                if (s.getMarks().get(sub) == max) {
                    if (names.length() > 0) names.append(", ");
                    names.append(s.getName());
                }
            }
            topperModel.addRow(new Object[]{subjects.get(sub), fmt(max), names.toString()});
        }

        // class statistics
        if (classCount == 0) {
            statsLabel.setText("No students added yet.");
        } else {
            statsLabel.setText(String.format(
                    "Students: %d    |    Subjects: %d    |    Class average: %.2f    |    "
                            + "Highest mark: %.2f (%s)    |    Lowest mark: %.2f (%s)",
                    students.size(), subjects.size(), classSum / classCount,
                    classHigh, highName, classLow, lowName));
        }
    }

    // ---------- Helpers ----------
    private Student findStudent(String name) {
        for (Student s : students) {
            if (s.getName().equalsIgnoreCase(name)) return s;
        }
        return null;
    }

    private static boolean containsIgnoreCase(ArrayList<String> list, String value) {
        for (String item : list) {
            if (item.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    private static String fmt(double value) {
        return String.format("%.2f", value);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input error", JOptionPane.WARNING_MESSAGE);
    }

    private static void centerAllButFirstColumn(JTable table) {
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 1; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(center);
        }
    }

    private static void styleHeader(JTable table) {
        JTableHeader header = table.getTableHeader();
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        header.setReorderingAllowed(false);
    }
}