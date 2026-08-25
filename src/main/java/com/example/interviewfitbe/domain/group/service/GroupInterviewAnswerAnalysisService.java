package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.dto.GroupInterviewAnswerAnalysisResponse;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewAnswerAnalysisEntity;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewAnswerAnalysisRepository;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewAnswerRepository;
import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class GroupInterviewAnswerAnalysisService {

    private final GroupInterviewAnswerRepository answerRepository;
    private final GroupInterviewAnswerAnalysisRepository analysisRepository;
    private final GroupInterviewAnswerAnalysisJobService analysisJobService;

    @Transactional
    public GroupInterviewAnswerAnalysisEntity createForUploadedAnswer(
            com.example.interviewfitbe.domain.group.entity.GroupInterviewAnswerEntity answer
    ) {
        GroupInterviewAnswerAnalysisEntity analysis = analysisRepository
                .findByAnswerId(answer.getId())
                .orElseGet(() -> analysisRepository.save(
                        GroupInterviewAnswerAnalysisEntity.builder()
                                .answer(answer)
                                .build()
                ));
        scheduleAfterCommit(answer.getId());
        return analysis;
    }

    @Transactional
    public GroupInterviewAnswerAnalysisResponse requestAnalysis(
            Long roomId,
            Long answerId,
            Long actorUserId
    ) {
        var answer = answerRepository.findByRoomIdAndIdForUpdate(roomId, answerId)
                .orElseThrow(() -> new IllegalArgumentException("해당 방의 답변을 찾을 수 없습니다."));
        validateOwner(answer.getParticipantUserId(), actorUserId);
        if (!answer.isVideoUploaded()) {
            throw new IllegalStateException("분석할 답변 영상이 없습니다.");
        }

        GroupInterviewAnswerAnalysisEntity analysis = analysisRepository
                .findByAnswerId(answerId)
                .orElseGet(() -> analysisRepository.save(
                        GroupInterviewAnswerAnalysisEntity.builder().answer(answer).build()
                ));
        if (analysis.getStatus() == AnalysisStatus.RUNNING) {
            throw new IllegalStateException("이미 분석 중인 답변입니다.");
        }
        if (analysis.getStatus() == AnalysisStatus.DONE) {
            return GroupInterviewAnswerAnalysisResponse.from(analysis);
        }

        analysis.markUploaded();
        analysisRepository.save(analysis);
        scheduleAfterCommit(answerId);
        return GroupInterviewAnswerAnalysisResponse.from(analysis);
    }

    @Transactional(readOnly = true)
    public GroupInterviewAnswerAnalysisResponse getAnalysis(
            Long roomId,
            Long answerId,
            Long actorUserId
    ) {
        GroupInterviewAnswerAnalysisEntity analysis = analysisRepository.findByAnswerId(answerId)
                .orElseThrow(() -> new IllegalArgumentException("답변 분석 정보를 찾을 수 없습니다."));
        var answer = analysis.getAnswer();
        if (!answer.getGroupInterview().getId().equals(roomId)) {
            throw new IllegalArgumentException("해당 방의 답변 분석 정보가 아닙니다.");
        }
        validateOwner(answer.getParticipantUserId(), actorUserId);
        return GroupInterviewAnswerAnalysisResponse.from(analysis);
    }

    private void scheduleAfterCommit(Long answerId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            analysisJobService.analyzeAsync(answerId);
                        }
                    }
            );
            return;
        }
        analysisJobService.analyzeAsync(answerId);
    }

    private void validateOwner(Long participantUserId, Long actorUserId) {
        if (!participantUserId.equals(actorUserId)) {
            throw new AccessDeniedException("답변자 본인만 답변 분석을 요청하거나 조회할 수 있습니다.");
        }
    }
}
