/** A department's clearance record for a student. */
public class Due {
    private final int id;
    private final int studentId;
    private final String department;
    private boolean cleared;

    public Due(int id, int studentId, String department, boolean cleared) {
        this.id = id; this.studentId = studentId; this.department = department; this.cleared = cleared;
    }
    public int getId() { return id; }
    public int getStudentId() { return studentId; }
    public String getDepartment() { return department; }
    public boolean isCleared() { return cleared; }
    public void setCleared(boolean cleared) { this.cleared = cleared; }
}
