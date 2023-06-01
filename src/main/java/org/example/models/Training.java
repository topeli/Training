package org.example.models;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

@Entity
@Table(name = "trainings")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Training {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long trainingId;

    private String classGroup;

    @ManyToOne(fetch = FetchType.EAGER,optional = false)
    @JoinColumn(name = "coach_id", nullable = false)
    private Coach coach;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm", timezone = "Europe/Moscow")
    private LocalTime startTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm", timezone = "Europe/Moscow")
    private LocalTime endTime;
    private LocalDate date;
    private String activity;
    public Training(String group, Coach coach, LocalTime startTime, LocalTime endTime, LocalDate date, String activity) {
        this.coach = coach;
        this.endTime = endTime;
        this.startTime = startTime;
        this.classGroup = group;
        this.date = date;
        this.activity = activity;
    }
}
