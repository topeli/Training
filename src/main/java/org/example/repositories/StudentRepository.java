package org.example.repositories;

import org.example.models.Rectangle;
import org.example.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RectangleRepository extends JpaRepository<Rectangle, Rectangle> {
}
