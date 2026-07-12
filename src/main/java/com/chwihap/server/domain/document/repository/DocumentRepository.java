package com.chwihap.server.domain.document.repository;

import com.chwihap.server.domain.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUser_IdAndJobPosting_IdOrderByCreatedAtAsc(Long userId, Long jobPostingId);
}
