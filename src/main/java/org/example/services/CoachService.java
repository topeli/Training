package org.example.services;
import org.example.models.Coach;
import org.example.repositories.CoachRepository;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.stereotype.Service;
@ComponentScan("org.example.services")
@Service
public class CoachService {
    private final CoachRepository coachRepository;


    public CoachService(CoachRepository coachRepository) {
        this.coachRepository = coachRepository;
    }

    public void addCoach(Coach coach){
        coachRepository.save(coach);
    }
}
