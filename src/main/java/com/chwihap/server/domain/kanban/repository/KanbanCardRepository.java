 package com.chwihap.server.domain.kanban.repository;

  import com.chwihap.server.domain.kanban.entity.KanbanCard;
  import com.chwihap.server.domain.kanban.entity.KanbanStage;                                                              
  import org.springframework.data.jpa.repository.JpaRepository;
  import org.springframework.data.jpa.repository.Modifying;                                                                
  import org.springframework.data.jpa.repository.Query;
  import org.springframework.data.repository.query.Param;

  public interface KanbanCardRepository extends JpaRepository<KanbanCard, Long> {

      boolean existsByJobPosting_Id(Long jobPostingId);

      long countByStage(KanbanStage stage);

      @Query("""                                                                                                           
              SELECT COALESCE(MAX(c.position), 0)                                                                          
              FROM KanbanCard c                                                                                            
              WHERE c.stage = :stage                                                                                       
              """)                                                                                                         
      int findMaxPositionByStage(@Param("stage") KanbanStage stage);

      @Modifying(clearAutomatically = true)                                                                                
      @Query("""                                                                                                           
              UPDATE KanbanCard c                                                                                          
              SET c.stage = :moveToStage,                                                                                  
                  c.position = c.position + :positionOffset                                                                
              WHERE c.stage = :deleteStage                                                                                 
              """)                                                                                                         
      int moveCardsToStage(
              @Param("deleteStage") KanbanStage deleteStage,
              @Param("moveToStage") KanbanStage moveToStage,
              @Param("positionOffset") int positionOffset
      );
  }
