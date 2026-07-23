package com.example.interviewfitbe.domain.pt_interview.repository;

import com.example.interviewfitbe.domain.pt_interview.entity.PtQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PtQuestionRepository
        extends JpaRepository<PtQuestionEntity, Long> {

    List<PtQuestionEntity>
    findAllByPtInterviewIdOrderByQuestionNumberAsc(
            Long ptInterviewId
    );

    void deleteAllByPtInterviewId(
            Long ptInterviewId
    );
}
