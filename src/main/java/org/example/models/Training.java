package org.example.models;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "trainings")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Training {
    @Id
    private Long group_id;
    private Long coach_id;
    private LocalDateTime start_time;
    private LocalDateTime end_time;

}
