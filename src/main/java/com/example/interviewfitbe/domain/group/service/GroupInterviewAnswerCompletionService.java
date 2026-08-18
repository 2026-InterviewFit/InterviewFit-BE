package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.GroupAnswerCompletionType;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GroupInterviewAnswerCompletionService {

    private final GroupInterviewAnswerRepository answerRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Long answerId, GroupAnswerCompletionType type, Instant completedAt) {
        if (answerId == null) {
            return;
        }
        answerRepository.findById(answerId)
                .ifPresent(answer -> answer.complete(type, completedAt));
    }
}
