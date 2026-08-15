package com.autopro.backend.repository;

import com.autopro.backend.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p WHERE p.id = :userId")
    List<ChatRoom> findByParticipantId(@Param("userId") Long userId);

    @Query("""
            SELECT cr FROM ChatRoom cr
            JOIN cr.participants p1
            JOIN cr.participants p2
            WHERE p1.id = :userId1 AND p2.id = :userId2
              AND cr.isGroup = false
            """)
    Optional<ChatRoom> findPrivateChatRoom(@Param("userId1") Long userId1,
                                           @Param("userId2") Long userId2);
}
