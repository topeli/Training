package org.example.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "students1533")
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Student {
    @Id // сразу не добавлять - показать, что класс будет подчеркиваться красным
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String surname;
    private int age;
    private String classGroup;

    public Student(String name, String surname, int age, String classGroup) {
        this.name = name;
        this.surname = surname;
        this.age = age;
        this.classGroup = classGroup;
    }
    //private LocalDateTime created;
}
