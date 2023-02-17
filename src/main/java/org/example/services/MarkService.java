package org.example.services;
import org.example.models.Mark;
import org.example.repositories.MarkRepository;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
@ComponentScan("org.example.services")
@Service
public class MarkService {
    private final MarkRepository markRepository;



    public MarkService(MarkRepository markRepository) {
        this.markRepository = markRepository;
    }

    public void addMark(Mark mark){
        markRepository.save(mark);
    }
}
