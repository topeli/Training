package org.example.models;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

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
    public Training(String group, Coach coach, LocalTime startTime, LocalTime endTime, LocalDate date) {
        this.coach = coach;
        this.endTime = endTime;
        this.startTime = startTime;
        this.classGroup = group;
        this.date = date;
    }

    @Override
    public String toString() {
        return "Training{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", date=" + date +
                '}';
    }
}
