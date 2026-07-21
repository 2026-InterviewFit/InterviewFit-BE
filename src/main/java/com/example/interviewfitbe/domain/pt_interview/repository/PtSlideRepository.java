package com.example.interviewfitbe.domain.pt_interview.repository;

import com.example.interviewfitbe.domain.pt_interview.entity.PtSlideEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PtSlideRepository
        extends JpaRepository<PtSlideEntity, Long> {

    List<PtSlideEntity> findAllByDocumentIdOrderBySlideNumberAsc(
            Long documentId
    );

    void deleteAllByDocumentId(Long documentId);
}
