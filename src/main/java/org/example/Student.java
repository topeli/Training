package org.example;

import java.util.Objects;

public class Student {
    private long id;
    private String name;
    private int mark;

    public Student(long id, String name, int mark) {
        this.id = id;
        this.name = name;
        this.mark = mark;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMark() {
        return mark;
    }

    public void setMark(int mark) {
        this.mark = mark;
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", mark=" + mark +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        Student student = (Student) o;
        return this.mark == student.mark && this.name.compareTo(student.name)==0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, mark);
    }
}
