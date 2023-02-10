package org.example.services;

import org.example.models.Rectangle;
import org.example.repositories.RectangleRepository;

@ComponentScan("com.example.demo.services")
@Service
public class RectangleService {
    private final RectangleRepository rectangleRepository;
    public RectangleService(RectangleRepository rectangleRepository){
        this.rectangleRepository = rectangleRepository;
    }

    public int Rectangle(Rectangle rec) {
        return rec.a * rec.b;
    }
}
