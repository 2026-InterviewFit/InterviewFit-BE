package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAnswerAnalysisJobService {

    private final InterviewAnswerRepository interviewAnswerRepository;
    private final TransactionTemplate transactionTemplate;

    /*
     * 기존 구조:
     * answer.videoObjectKey 기준으로 답변별 영상 분석
     *
     * 수정 구조:
     * 전체 면접 영상은 videos 테이블에 1개 저장
     * answer는 startTime/endTime만 저장
     *
     * 따라서 video 조회 흐름이 완성되기 전까지는
     * 여기서 바로 eye/pose/speech 분석을 호출하면 안 됨.
     */
    @Async("analysisExecutor")
    public void analyzeAnswerAsync(Long answerId) {
        log.info("답변 분석 Job 호출됨. 현재 전체 영상 기반 구조로 전환 중이므로 자동 분석은 보류: answerId={}", answerId);

        transactionTemplate.executeWithoutResult(status -> {
            InterviewAnswerEntity answer = interviewAnswerRepository.findById(answerId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "해당 답변을 찾을 수 없습니다. answerId=" + answerId
                    ));

            answer.markUploaded();
        });
    }
}