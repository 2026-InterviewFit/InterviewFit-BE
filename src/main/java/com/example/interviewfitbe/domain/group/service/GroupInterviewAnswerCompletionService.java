package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.GroupAnswerCompletionType;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GroupInterviewAnswerCompletionService {

    private final GroupInterviewAnswerRepository answerRepository;

    @Transactional
    public void complete(Long answerId, GroupAnswerCompletionType type, Instant completedAt) {
        if (answerId == null) {
            throw new IllegalStateException("완료할 그룹 면접 답변 ID가 없습니다.");
        }
        answerRepository.findByIdForUpdate(answerId)
                .orElseThrow(() -> new IllegalStateException(
                        "완료할 그룹 면접 답변을 찾을 수 없습니다. answerId=" + answerId
                ))
                .complete(type, completedAt);
    }
}
