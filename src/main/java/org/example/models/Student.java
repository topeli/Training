package org.example.models;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "students1533")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String surname;
    private int age;
    private String classGroup;

    private String password;

    private Long chatId;



    public Student(String name, String surname, int age, String classGroup) {
        this.name = name;
        this.surname = surname;
        this.age = age;
        this.classGroup = classGroup;
    }
    //private LocalDateTime created;
}
