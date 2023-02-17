package org.example.services;

import org.example.models.Rectangle;
import org.example.repositories.RectangleRepository;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

@ComponentScan("com.example.demo.services")
@Service
public class RectangleService {
    private final RectangleRepository rectangleRepository;
    public RectangleService(RectangleRepository rectangleRepository){
        this.rectangleRepository = rectangleRepository;
    }

    public int rectangle(Rectangle rec) {
        rectangleRepository.save(rec);
        return rec.getA() * rec.getB();
    }
}
