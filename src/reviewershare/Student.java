package reviewershare;

/** A registered user (a student, or the admin). Passwords are never kept in memory - only in the database (hashed). */
public class Student {
    public String id, name, email, course, section, role;

    public Student(String id, String name, String email, String course, String section, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.course = course;
        this.section = section;
        this.role = role;
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }
}