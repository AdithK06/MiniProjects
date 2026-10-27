/** Student record shown in the no-due dashboard. */
public class Student {
    private final int id;
    private final String rollNo;
    private final String name;
    private final String department;
    private final int semester;
    private boolean cleared;

    public Student(int id, String rollNo, String name, String department, int semester) {
        this(id, rollNo, name, department, semester, false);
    }

    public Student(int id, String rollNo, String name, String department, int semester, boolean cleared) {
        this.id = id; this.rollNo = rollNo; this.name = name; this.department = department;
        this.semester = semester; this.cleared = cleared;
    }
    public int getId() { return id; }
    public String getRollNo() { return rollNo; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public int getSemester() { return semester; }
    public boolean isCleared() { return cleared; }
    public void setCleared(boolean cleared) { this.cleared = cleared; }
}
