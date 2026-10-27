import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Swing entry point for the student clearance application. */
public class Main {
    private static final Color NAVY = new Color(25, 43, 72);
    private final NoDueService service = new NoDueService();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Roll No.", "Student Name", "Department", "Semester", "Status"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField search = new JTextField(20);
    private final JLabel countLabel = new JLabel("0 students");
    private JFrame frame;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { new Main().showWindow(); }
            catch (IllegalStateException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "NoDue startup failed", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void showWindow() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) { }
        frame = new JFrame("NoDue | Student Clearance");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(850, 560));
        frame.setSize(1050, 680);
        frame.setLocationRelativeTo(null);
        showHome();
        frame.setVisible(true);
    }

    private void showHome() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(new Color(245, 247, 251));
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(38, 46, 38, 46));

        JLabel title = new JLabel("NoDue");
        title.setFont(new Font("SansSerif", Font.BOLD, 34));
        title.setForeground(NAVY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = new JLabel("Student clearance portal");
        subtitle.setForeground(new Color(105, 116, 134));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel storage = new JLabel(storageDescription());
        storage.setForeground(Database.isConfigured() ? new Color(28, 125, 79) : new Color(165, 93, 20));
        storage.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton login = button("Student login", NAVY, Color.WHITE);
        login.setAlignmentX(Component.CENTER_ALIGNMENT);
        login.addActionListener(e -> showLogin());
        JButton register = button("New student registration", new Color(225, 232, 243), NAVY);
        register.setAlignmentX(Component.CENTER_ALIGNMENT);
        register.addActionListener(e -> showRegistration());
        JButton admin = button("Administrator dashboard", new Color(225, 232, 243), NAVY);
        admin.setAlignmentX(Component.CENTER_ALIGNMENT);
        admin.addActionListener(e -> showAdmin());

        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(8));
        card.add(storage);
        card.add(Box.createVerticalStrut(28));
        card.add(login);
        card.add(Box.createVerticalStrut(12));
        card.add(register);
        card.add(Box.createVerticalStrut(12));
        card.add(admin);
        root.add(card);
        frame.setContentPane(root);
        frame.revalidate();
    }

    private void showLogin() {
        JTextField userId = new JTextField(18);
        JPasswordField password = new JPasswordField(18);
        Object[] fields = {"Student ID or roll number", userId, "Password", password};
        int choice = JOptionPane.showConfirmDialog(frame, fields, "Student login",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;
        Student student;
        try {
            student = service.authenticate(userId.getText(), new String(password.getPassword()));
        } catch (IllegalStateException ex) {
            showDatabaseError(ex);
            return;
        }
        if (student == null) {
            JOptionPane.showMessageDialog(frame, "Login failed. Check your ID or roll number and password.",
                    "Unable to sign in", JOptionPane.ERROR_MESSAGE);
            return;
        }
        showStudentDashboard(student.getRollNo());
    }

    private void showRegistration() {
        JTextField roll = new JTextField(18);
        JTextField name = new JTextField(18);
        JTextField department = new JTextField(18);
        JSpinner semester = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
        JPasswordField password = new JPasswordField(18);
        JPasswordField confirm = new JPasswordField(18);
        Object[] fields = {"Roll number", roll, "Full name", name, "Department", department,
                "Semester", semester, "Create password (at least 8 characters)", password,
                "Confirm password", confirm};
        int choice = JOptionPane.showConfirmDialog(frame, fields, "New student registration",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        char[] pass = password.getPassword();
        char[] repeated = confirm.getPassword();
        if (!Arrays.equals(pass, repeated)) {
            Arrays.fill(pass, '\0');
            Arrays.fill(repeated, '\0');
            JOptionPane.showMessageDialog(frame, "The passwords do not match.",
                    "Registration failed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Student student = service.registerStudent(roll.getText(), name.getText(), department.getText(),
                    (Integer) semester.getValue(), pass);
            JOptionPane.showMessageDialog(frame,
                    "Registration complete. Your student ID is " + student.getId()
                            + ". Use this ID or your roll number with your chosen password to log in.",
                    "Registration successful", JOptionPane.INFORMATION_MESSAGE);
            showStudentDashboard(student.getRollNo());
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(), "Registration failed", JOptionPane.WARNING_MESSAGE);
        } catch (IllegalStateException ex) {
            showDatabaseError(ex);
        } finally {
            Arrays.fill(pass, '\0');
            Arrays.fill(repeated, '\0');
        }
    }

    private void showStudentDashboard(String rollNo) {
        Student student;
        try { student = service.findStudent(rollNo); }
        catch (IllegalStateException ex) { showDatabaseError(ex); return; }
        if (student == null) { showHome(); return; }

        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBackground(new Color(245, 247, 251));
        root.setBorder(new EmptyBorder(26, 30, 24, 30));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Welcome, " + student.getName());
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(NAVY);
        JLabel subtitle = new JLabel("Your student clearance dashboard");
        subtitle.setForeground(new Color(105, 116, 134));
        titles.add(title);
        titles.add(Box.createVerticalStrut(6));
        titles.add(subtitle);
        JButton logout = button("Log out", new Color(225, 232, 243), NAVY);
        logout.addActionListener(e -> showHome());
        header.add(titles, BorderLayout.WEST);
        header.add(logout, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JPanel details = new JPanel(new GridLayout(0, 2, 14, 14));
        details.setBackground(Color.WHITE);
        details.setBorder(new EmptyBorder(24, 26, 24, 26));
        addDetail(details, "Student ID", String.valueOf(student.getId()));
        addDetail(details, "Roll number", student.getRollNo());
        addDetail(details, "Name", student.getName());
        addDetail(details, "Department", student.getDepartment());
        addDetail(details, "Semester", String.valueOf(student.getSemester()));
        addDetail(details, "Clearance status", student.isCleared() ? "Cleared" : "Due pending");
        JLabel info = new JLabel(student.isCleared()
                ? "No outstanding due. Your certificate is ready."
                : "An outstanding due is recorded for your account.");
        info.setForeground(student.isCleared() ? new Color(28, 125, 79) : new Color(165, 93, 20));
        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(details, BorderLayout.NORTH);
        body.add(info, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        if (!student.isCleared()) {
            JButton pay = button("Pay due", NAVY, Color.WHITE);
            pay.addActionListener(e -> payStudentDue(rollNo));
            actions.add(pay);
        } else {
            JButton certificate = button("Download certificate", NAVY, Color.WHITE);
            certificate.addActionListener(e -> downloadCertificate(rollNo));
            actions.add(certificate);
        }
        body.add(actions, BorderLayout.SOUTH);
        root.add(body, BorderLayout.CENTER);
        frame.setContentPane(root);
        frame.revalidate();
        frame.repaint();
    }

    private void addDetail(JPanel panel, String label, String value) {
        JLabel key = new JLabel(label);
        key.setForeground(new Color(105, 116, 134));
        JLabel text = new JLabel(value);
        text.setFont(new Font("SansSerif", Font.BOLD, 16));
        text.setForeground(NAVY);
        panel.add(key);
        panel.add(text);
    }

    private void payStudentDue(String rollNo) {
        int answer = JOptionPane.showConfirmDialog(frame,
                "This demo payment marks the outstanding due as paid. No real money will be charged.",
                "Confirm demo payment", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            if (service.payDue(rollNo)) {
                JOptionPane.showMessageDialog(frame, "Demo payment recorded. Your clearance is complete.",
                        "Payment complete", JOptionPane.INFORMATION_MESSAGE);
                showStudentDashboard(rollNo);
            }
        } catch (IllegalStateException ex) { showDatabaseError(ex); }
    }

    private void downloadCertificate(String rollNo) {
        Student student;
        try { student = service.findStudent(rollNo); }
        catch (IllegalStateException ex) { showDatabaseError(ex); return; }
        if (student == null || !student.isCleared()) {
            notice("Clear all outstanding dues before downloading a certificate.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("NoDue-Certificate-" + student.getRollNo() + ".pdf"));
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        File selected = chooser.getSelectedFile();
        File output = selected.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")
                ? selected : new File(selected.getAbsolutePath() + ".pdf");
        try {
            writeCertificatePdf(output, student);
            JOptionPane.showMessageDialog(frame, "Certificate saved to " + output.getAbsolutePath(),
                    "Certificate downloaded", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Could not save certificate: " + ex.getMessage(),
                    "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void writeCertificatePdf(File file, Student student) throws IOException {
        String[] lines = {"CERTIFICATE OF NO DUE", "", "This is to certify that " + student.getName(),
                "Student ID: " + student.getId(), "Roll No: " + student.getRollNo(),
                "Department: " + student.getDepartment(), "Semester: " + student.getSemester(), "",
                "has cleared all recorded dues.", "", "Issued by NoDue Student Clearance Portal."};
        StringBuilder stream = new StringBuilder("BT /F1 18 Tf 72 740 Td ");
        for (int i = 0; i < lines.length; i++) {
            String safe = lines[i].replaceAll("[^\\x20-\\x7E]", "?");
            String escaped = safe.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
            if (i > 0) stream.append("0 -30 Td ");
            stream.append("(").append(escaped).append(") Tj ");
        }
        stream.append("ET");
        String content = stream.toString();
        String[] objects = {
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + content.getBytes("ISO-8859-1").length + " >>\nstream\n" + content + "\nendstream"
        };
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        int[] offsets = new int[objects.length + 1];
        for (int i = 0; i < objects.length; i++) {
            offsets[i + 1] = pdf.toString().getBytes("ISO-8859-1").length;
            pdf.append(i + 1).append(" 0 obj\n").append(objects[i]).append("\nendobj\n");
        }
        int xref = pdf.toString().getBytes("ISO-8859-1").length;
        pdf.append("xref\n0 ").append(objects.length + 1).append("\n0000000000 65535 f \n");
        for (int i = 1; i < offsets.length; i++)
            pdf.append(String.format(Locale.ROOT, "%010d 00000 n \n", offsets[i]));
        pdf.append("trailer\n<< /Size ").append(objects.length + 1)
                .append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(pdf.toString().getBytes("ISO-8859-1"));
        }
    }

    private void showAdmin() {
        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBackground(new Color(245, 247, 251));
        root.setBorder(new EmptyBorder(26, 30, 24, 30));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Student records");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(NAVY);
        JLabel subtitle = new JLabel("Manage student details and clearance status  •  " + storageDescription());
        subtitle.setForeground(new Color(105, 116, 134));
        titles.add(title);
        titles.add(Box.createVerticalStrut(6));
        titles.add(subtitle);
        JButton home = button("Home", new Color(225, 232, 243), NAVY);
        home.addActionListener(e -> showHome());
        JButton add = button("+  Add student", NAVY, Color.WHITE);
        add.addActionListener(e -> addStudent());
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        headerActions.add(home);
        headerActions.add(add);
        header.add(titles, BorderLayout.WEST);
        header.add(headerActions, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);
        search.setFont(new Font("SansSerif", Font.PLAIN, 14));
        search.setToolTipText("Search by ID, roll number, name or department");
        search.addActionListener(e -> refresh());
        JPanel searchBox = new JPanel(new BorderLayout(8, 0));
        searchBox.setOpaque(false);
        searchBox.add(search, BorderLayout.CENTER);
        JButton find = button("Search", new Color(225, 232, 243), NAVY);
        find.addActionListener(e -> refresh());
        JButton clear = button("Clear search", new Color(225, 232, 243), NAVY);
        clear.addActionListener(e -> { search.setText(""); refresh(); });
        JPanel searchActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchActions.setOpaque(false);
        searchActions.add(find);
        searchActions.add(clear);
        searchBox.add(searchActions, BorderLayout.EAST);
        toolbar.add(searchBox, BorderLayout.WEST);
        JButton status = button("Toggle clearance", new Color(225, 232, 243), NAVY);
        status.addActionListener(e -> toggleSelected());
        toolbar.add(status, BorderLayout.EAST);
        center.add(toolbar, BorderLayout.NORTH);

        table.setRowHeight(42);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(237, 241, 248));
        table.setSelectionBackground(new Color(220, 231, 248));
        table.setSelectionForeground(NAVY);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(235, 238, 243));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(226, 231, 239)));
        center.add(scroll, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        countLabel.setForeground(new Color(105, 116, 134));
        footer.add(countLabel, BorderLayout.WEST);
        JButton delete = button("Remove selected", new Color(255, 235, 235), new Color(161, 43, 43));
        delete.addActionListener(e -> removeSelected());
        footer.add(delete, BorderLayout.EAST);
        center.add(footer, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);
        frame.setContentPane(root);
        refresh();
        frame.revalidate();
        frame.repaint();
    }

    private void addStudent() {
        JTextField roll = new JTextField();
        JTextField name = new JTextField();
        JTextField department = new JTextField();
        JSpinner semester = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
        Object[] fields = {"Roll number", roll, "Student name", name,
                "Department", department, "Semester", semester};
        int choice = JOptionPane.showConfirmDialog(frame, fields, "Add student",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;
        try {
            Student added = service.addStudent(roll.getText(), name.getText(), department.getText(),
                    (Integer) semester.getValue());
            search.setText("");
            refresh();
            for (int row = 0; row < model.getRowCount(); row++) {
                if (added.getId() == ((Number) model.getValueAt(row, 0)).intValue()) {
                    table.setRowSelectionInterval(row, row);
                    break;
                }
            }
            JOptionPane.showMessageDialog(frame, "Student " + added.getRollNo()
                    + " was added to the student list.", "Student added", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(), "Check student details", JOptionPane.WARNING_MESSAGE);
        } catch (IllegalStateException ex) { showDatabaseError(ex); }
    }

    private void refresh() {
        try {
            List<Student> students = service.searchStudents(search.getText().trim());
            model.setRowCount(0);
            for (Student student : students) {
                model.addRow(new Object[]{student.getId(), student.getRollNo(), student.getName(),
                        student.getDepartment(), student.getSemester(), student.isCleared() ? "Cleared" : "Pending"});
            }
            countLabel.setText(students.size() + (students.size() == 1 ? " student" : " students"));
        } catch (IllegalStateException ex) { showDatabaseError(ex); }
    }

    private String selectedRoll() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return (String) model.getValueAt(table.convertRowIndexToModel(row), 1);
    }

    private void toggleSelected() {
        String roll = selectedRoll();
        if (roll == null) { notice("Select a student first."); return; }
        try { service.toggleClearance(roll); refresh(); }
        catch (IllegalStateException ex) { showDatabaseError(ex); }
    }

    private void removeSelected() {
        String roll = selectedRoll();
        if (roll == null) { notice("Select a student first."); return; }
        int answer = JOptionPane.showConfirmDialog(frame, "Remove student " + roll + "?",
                "Confirm removal", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            try { service.removeStudent(roll); refresh(); }
            catch (IllegalStateException ex) { showDatabaseError(ex); }
        }
    }

    private String storageDescription() {
        return Database.isConfigured() ? "MySQL database: nodue" : "DEMO MODE: changes are temporary";
    }

    private JButton button(String text, Color background, Color foreground) {
        JButton result = new JButton(text);
        result.setFont(new Font("SansSerif", Font.BOLD, 13));
        result.setBackground(background);
        result.setForeground(foreground);
        result.setFocusPainted(false);
        result.setBorder(new EmptyBorder(10, 15, 10, 15));
        result.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return result;
    }

    private void showDatabaseError(IllegalStateException ex) {
        JOptionPane.showMessageDialog(frame, ex.getMessage(), "Database operation failed", JOptionPane.ERROR_MESSAGE);
    }

    private void notice(String message) {
        JOptionPane.showMessageDialog(frame, message, "NoDue", JOptionPane.INFORMATION_MESSAGE);
    }
}
