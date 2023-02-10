package org.example.repositories;

import org.example.models.Rectangle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RectangleRepository extends JpaRepository<Rectangle, Long> {

}
