package org.example.models;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import javax.persistence.*;
@EntityScan("com.example.demo.model")
@Entity
@Table(name="coaches")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Coach {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String surname;
    private Long age;
    private Long experience;
    private String passwordCoach;
    private Long chatId;
    private String classGroups;
    private String activity;

    public Coach(String name, String surname, Long age, String activity, Long e) {
        this.name = name;
        this.surname=surname;
        this.age = age;
        this.activity = activity;
        this.experience = e;
    }
}
