package org.example.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Table(name="triangles")
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor

public class Triangle {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private int a;
    private int b;


}
