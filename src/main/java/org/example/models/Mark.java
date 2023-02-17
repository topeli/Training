package org.example.models;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;//aaaaaaaaaa
import org.springframework.boot.autoconfigure.domain.EntityScan;

import javax.persistence.*;
@EntityScan("com.example.demo.model")
@Entity
@Table(name="marks")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Mark {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long student_id;

    private Long coach_id;
    private int mark;
}
