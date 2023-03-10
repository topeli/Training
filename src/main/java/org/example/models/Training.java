package org.example.models;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import javax.persistence.*;
import java.time.LocalDateTime;
@EntityScan("org.example.models")
@Entity
@Table(name = "trainings")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Training {
    private Long groupId;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long trainingId;
    @ManyToOne(fetch = FetchType.EAGER,optional = false)
    @JoinColumn(name = "studentId", nullable = false)
    private Student student;
    @JoinColumn(name = "coachId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Coach coach;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
