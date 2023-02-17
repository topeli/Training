package org.example.models;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Table (name = "rectangle")
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Rectangle {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private int a;
    private int b;

}
