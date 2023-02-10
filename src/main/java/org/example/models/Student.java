package org.example.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "students1533")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Student {
    @Id // сразу не добавлять - показать, что класс будет подчеркиваться красным
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String surname;
    private Long age;
    private String classGroup;
}
