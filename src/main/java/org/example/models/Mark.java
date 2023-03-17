package org.example.models;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import javax.persistence.*;
@EntityScan("org.example.models")
@Entity
@Table(name="marks")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Mark {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER,optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Student student;
    @ManyToOne(fetch = FetchType.EAGER,optional = false)
    @JoinColumn(name = "coach_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Coach coach;
    private int mark;

    public Mark(int mark) {
        this.mark = mark;
    }
}
