import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Student service using MySQL when configured, with an in-memory demo fallback. */
public class NoDueService {
    private final List<Student> students = new ArrayList<>();
    private int nextId = 1;
    private final Map<Integer, String[]> demoCredentials = new HashMap<>();
    private final boolean databaseMode = Database.isConfigured();

    public NoDueService() {
        if (databaseMode) loadFromDatabase();
        else {
            addStudent("CSE001", "A", "CSE", 3);
            addStudent("ECE002", "Sample Student", "ECE", 2);
        }
    }

    public synchronized List<Student> searchStudents(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (databaseMode) {
            List<Student> result = new ArrayList<>();
            try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(
                    "SELECT id, roll_no, name, department, semester, cleared FROM students " +
                    "WHERE CAST(id AS CHAR) LIKE ? OR LOWER(roll_no) LIKE ? OR LOWER(name) LIKE ? OR LOWER(department) LIKE ? ORDER BY name")) {
                String term = "%" + q + "%";
                ps.setString(1, term); ps.setString(2, term); ps.setString(3, term); ps.setString(4, term);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(readStudent(rs)); }
                return result;
            } catch (SQLException e) { throw databaseError(e); }
        }
        List<Student> result = new ArrayList<>();
        for (Student s : students) if (q.isEmpty() || String.valueOf(s.getId()).contains(q)
                || s.getRollNo().toLowerCase(Locale.ROOT).contains(q)
                || s.getName().toLowerCase(Locale.ROOT).contains(q) || s.getDepartment().toLowerCase(Locale.ROOT).contains(q)) result.add(s);
        return result;
    }

    public synchronized Student addStudent(String rollNo, String name, String department, int semester) {
        String roll = required(rollNo, "Roll number").toUpperCase(Locale.ROOT);
        String studentName = required(name, "Student name");
        String dept = required(department, "Department").toUpperCase(Locale.ROOT);
        if (semester < 1 || semester > 12) throw new IllegalArgumentException("Semester must be between 1 and 12.");
        if (databaseMode) {
            try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO students (roll_no, name, department, semester, cleared) VALUES (?, ?, ?, ?, FALSE)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, roll); ps.setString(2, studentName); ps.setString(3, dept); ps.setInt(4, semester); ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) return new Student(keys.getInt(1), roll, studentName, dept, semester); }
                throw new SQLException("Insert succeeded but MySQL did not return the student id.");
            } catch (SQLException e) { if (e.getErrorCode() == 1062) throw new IllegalArgumentException("That roll number already exists."); throw databaseError(e); }
        }
        for (Student s : students) if (s.getRollNo().equalsIgnoreCase(roll)) throw new IllegalArgumentException("That roll number already exists.");
        Student student = new Student(nextId++, roll, studentName, dept, semester); students.add(student); return student;
    }

    /** Registers a new account, or attaches credentials to a student previously added by an admin. */
    public synchronized Student registerStudent(String rollNo, String name, String department, int semester, char[] password) {
        validatePassword(password);
        String roll = required(rollNo, "Roll number").toUpperCase(Locale.ROOT);
        String studentName = required(name, "Student name");
        String dept = required(department, "Department").toUpperCase(Locale.ROOT);
        if (semester < 1 || semester > 12) throw new IllegalArgumentException("Semester must be between 1 and 12.");
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        String saltText = Base64.getEncoder().encodeToString(salt);
        String hashText = hashPassword(password, salt);
        if (databaseMode) {
            try (Connection c = Database.getConnection()) {
                c.setAutoCommit(false);
                try {
                    Student existing = null; boolean hasPassword = false;
                    try (PreparedStatement ps = c.prepareStatement("SELECT id, roll_no, name, department, semester, cleared, password_hash FROM students WHERE roll_no = ? FOR UPDATE")) {
                        ps.setString(1, roll);
                        try (ResultSet rs = ps.executeQuery()) { if (rs.next()) { existing = readStudent(rs); hasPassword = rs.getString("password_hash") != null; } }
                    }
                    Student registered;
                    if (existing == null) {
                        try (PreparedStatement ps = c.prepareStatement("INSERT INTO students (roll_no, name, department, semester, cleared, password_hash, password_salt) VALUES (?, ?, ?, ?, FALSE, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                            ps.setString(1, roll); ps.setString(2, studentName); ps.setString(3, dept); ps.setInt(4, semester); ps.setString(5, hashText); ps.setString(6, saltText); ps.executeUpdate();
                            try (ResultSet keys = ps.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("MySQL did not return the student id."); registered = new Student(keys.getInt(1), roll, studentName, dept, semester); }
                        }
                    } else {
                        if (hasPassword) throw new IllegalArgumentException("An account already exists for that roll number.");
                        if (!existing.getName().equalsIgnoreCase(studentName) || !existing.getDepartment().equalsIgnoreCase(dept) || existing.getSemester() != semester)
                            throw new IllegalArgumentException("The details must match the student record created by the administrator.");
                        try (PreparedStatement ps = c.prepareStatement("UPDATE students SET password_hash = ?, password_salt = ? WHERE id = ?")) {
                            ps.setString(1, hashText); ps.setString(2, saltText); ps.setInt(3, existing.getId()); ps.executeUpdate();
                        }
                        registered = existing;
                    }
                    c.commit(); return registered;
                } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            } catch (IllegalArgumentException e) { throw e; }
            catch (SQLException e) { if (e.getErrorCode() == 1062) throw new IllegalArgumentException("That roll number already exists."); throw databaseError(e); }
        }
        Student existing = null;
        for (Student candidate : students) if (candidate.getRollNo().equalsIgnoreCase(roll)) { existing = candidate; break; }
        if (existing != null) {
            if (demoCredentials.containsKey(existing.getId())) throw new IllegalArgumentException("An account already exists for that roll number.");
            if (!existing.getName().equalsIgnoreCase(studentName) || !existing.getDepartment().equalsIgnoreCase(dept) || existing.getSemester() != semester)
                throw new IllegalArgumentException("The details must match the student record created by the administrator.");
        } else existing = addStudent(roll, studentName, dept, semester);
        demoCredentials.put(existing.getId(), new String[]{saltText, hashText}); return existing;
    }

    public synchronized boolean toggleClearance(String rollNo) {
        Student s = findStudent(rollNo); if (s == null) return false;
        if (databaseMode) return setCleared(rollNo, !s.isCleared());
        s.setCleared(!s.isCleared()); return true;
    }
    public synchronized Student findStudent(String rollNo) {
        if (rollNo == null) return null;
        if (databaseMode) {
            try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(
                    "SELECT id, roll_no, name, department, semester, cleared FROM students WHERE roll_no = ?")) {
                ps.setString(1, rollNo.trim()); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? readStudent(rs) : null; }
            } catch (SQLException e) { throw databaseError(e); }
        }
        for (Student s : students) if (s.getRollNo().equalsIgnoreCase(rollNo)) return s;
        return null;
    }
    /** Authenticate using generated student ID or roll number and the registered password. */
    public synchronized Student authenticate(String rollNo, String password) {
        if (rollNo == null || password == null) return null;
        Student student;
        String saltText = null, hashText = null;
        if (databaseMode) {
            try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(
                    "SELECT id, roll_no, name, department, semester, cleared, password_hash, password_salt FROM students WHERE roll_no = ? OR CAST(id AS CHAR) = ? LIMIT 1")) {
                ps.setString(1, rollNo.trim()); ps.setString(2, rollNo.trim());
                try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) return null; student = readStudent(rs); hashText = rs.getString("password_hash"); saltText = rs.getString("password_salt"); }
            } catch (SQLException e) { throw databaseError(e); }
        } else {
            student = null;
            for (Student candidate : students) if (candidate.getRollNo().equalsIgnoreCase(rollNo.trim()) || String.valueOf(candidate.getId()).equals(rollNo.trim())) { student = candidate; break; }
            if (student == null) return null;
            String[] credentials = demoCredentials.get(student.getId());
            if (credentials == null) return null;
            saltText = credentials[0]; hashText = credentials[1];
        }
        if (hashText == null || saltText == null) return null;
        byte[] salt = Base64.getDecoder().decode(saltText);
        return MessageDigest.isEqual(Base64.getDecoder().decode(hashText), Base64.getDecoder().decode(hashPassword(password.toCharArray(), salt))) ? student : null;
    }
    public synchronized boolean payDue(String rollNo) { return setCleared(rollNo, true); }
    private boolean setCleared(String rollNo, boolean cleared) {
        Student student = findStudent(rollNo); if (student == null) return false;
        if (databaseMode) {
            try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE students SET cleared = ? WHERE roll_no = ?")) {
                ps.setBoolean(1, cleared); ps.setString(2, rollNo.trim()); return ps.executeUpdate() > 0;
            } catch (SQLException e) { throw databaseError(e); }
        }
        student.setCleared(cleared); return true;
    }
    public synchronized boolean removeStudent(String rollNo) {
        if (databaseMode) {
            try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM students WHERE roll_no = ?")) {
                ps.setString(1, rollNo); return ps.executeUpdate() > 0;
            } catch (SQLException e) { throw databaseError(e); }
        }
        Student s = findStudent(rollNo); return s != null && students.remove(s);
    }
    private void loadFromDatabase() {
        try (Connection c = Database.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, roll_no, name, department, semester, cleared FROM students ORDER BY name")) {
            while (rs.next()) students.add(readStudent(rs));
        } catch (SQLException e) { throw databaseError(e); }
    }
    private Student readStudent(ResultSet rs) throws SQLException {
        return new Student(rs.getInt("id"), rs.getString("roll_no"), rs.getString("name"),
                rs.getString("department"), rs.getInt("semester"), rs.getBoolean("cleared"));
    }
    private IllegalStateException databaseError(SQLException e) {
        return new IllegalStateException("Database operation failed: " + e.getMessage() +
                ". Check that MySQL is running, sql/no_due.sql was imported, and the NODUE_DB_* settings are correct.", e);
    }
    private String required(String value, String label) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + " is required.");
        return value.trim();
    }
    private void validatePassword(char[] password) {
        if (password == null || password.length < 8) throw new IllegalArgumentException("Password must be at least 8 characters.");
    }
    private String hashPassword(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, 120000, 256);
        try { return Base64.getEncoder().encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded()); }
        catch (Exception e) { throw new IllegalStateException("Could not secure the password.", e); }
        finally { spec.clearPassword(); }
    }
}
