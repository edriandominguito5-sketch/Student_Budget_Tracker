package org.yourcompany.yourproject;

public class Student {
    private String name;
    private String studentId;

    public Student() {
        this("", "");
    }

    public Student(String name, String studentId) {
        this.name = name == null ? "" : name.trim();
        this.studentId = studentId == null ? "" : studentId.trim();
    }

    public String getName() {
        return name;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId == null ? "" : studentId.trim();
    }
}
