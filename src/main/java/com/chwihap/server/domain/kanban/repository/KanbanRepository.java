package com.chwihap.server.domain.kanban.repository;

import com.chwihap.server.domain.kanban.entity.KanbanStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KanbanRepository extends JpaRepository<KanbanStage, Long> {

    long countByUserId(Long userId);

    /**
     * 삽입 위치(position) 이후의 스테이지들을 한 칸씩 뒤로 민다.
     * (user_id, position) 유니크 제약 위반을 피하기 위해
     * position이 큰 행부터 갱신해야 하므로 ORDER BY DESC가 필요하고,
     * JPQL의 UPDATE는 ORDER BY를 지원하지 않아 네이티브 쿼리를 사용한다.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE kanban_stages
            SET position = position + 1
            WHERE user_id = :userId AND position >= :position
            ORDER BY position DESC
            """, nativeQuery = true)
    void shiftPositionsFrom(@Param("userId") Long userId, @Param("position") int position);
}
