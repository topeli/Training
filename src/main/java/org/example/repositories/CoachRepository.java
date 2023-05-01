package org.example.repositories;
import org.example.models.Coach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CoachRepository extends JpaRepository<Coach, Long>{
    @Query(value = "select * from coaches where name = :name",nativeQuery = true)
    List<Coach> findByName(String name);

    @Query(value = "select * from coaches where chat_id = :chatId", nativeQuery = true)
    List<Coach> coachByChatId(Long chatId);
}
