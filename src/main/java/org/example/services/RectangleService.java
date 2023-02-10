package org.example.services;

import org.example.models.Rectangle;
import org.example.models.Student;
import org.example.repositories.RectangleRepository;

@ComponentScan("com.example.demo.services")
@Service
public class RectangleService {
    private final RectangleRepository rectangleRepository;
    public RectangleService(RectangleRepository rectangleRepository){
        this.rectangleRepository = rectangleRepository;
    }

    public int rectangle(Rectangle rec) {
        return rec.getA() * rec.getB();
    }
    public void addRectangle(Rectangle rec) {
        RectangleRepository.save(rec);
    }
}
