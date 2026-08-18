package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.eye_blinking.service.EyeBlinkingAnalysisService;
import com.example.interviewfitbe.domain.eye_gaze.service.EyeGazeAnalysisService;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewAnswerAnalysisEntity;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewAnswerEntity;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import com.example.interviewfitbe.domain.group.entity.GroupQuestionEntity;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewAnswerAnalysisRepository;
import com.example.interviewfitbe.domain.group.repository.GroupQuestionRepository;
import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.repository.JobRoleRepository;
import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResponse;
import com.example.interviewfitbe.domain.pose.service.PoseAnalysisService;
import com.example.interviewfitbe.domain.speech_coaching.service.SpeechAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GroupInterviewAnswerAnalysisJobServiceTest {

    private GroupInterviewAnswerAnalysisRepository analysisRepository;
    private EyeBlinkingAnalysisService blinkingService;
    private EyeGazeAnalysisService gazeService;
    private PoseAnalysisService poseService;
    private SpeechAnalysisService speechService;
    private GroupInterviewAnswerAnalysisJobService service;
    private GroupInterviewAnswerAnalysisEntity analysis;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        analysisRepository = mock(GroupInterviewAnswerAnalysisRepository.class);
        blinkingService = mock(EyeBlinkingAnalysisService.class);
        gazeService = mock(EyeGazeAnalysisService.class);
        poseService = mock(PoseAnalysisService.class);
        speechService = mock(SpeechAnalysisService.class);
        GroupQuestionRepository questionRepository = mock(GroupQuestionRepository.class);
        JobRoleRepository jobRoleRepository = mock(JobRoleRepository.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        TransactionStatus transactionStatus = mock(TransactionStatus.class);

        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback<Object>) invocation.getArgument(0))
                        .doInTransaction(transactionStatus));
        doAnswer(invocation -> {
            ((Consumer<TransactionStatus>) invocation.getArgument(0)).accept(transactionStatus);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        GroupInterviewEntity interview = mock(GroupInterviewEntity.class);
        when(interview.getId()).thenReturn(10L);
        when(interview.getJobRoleId()).thenReturn(2L);
        GroupInterviewAnswerEntity answer = GroupInterviewAnswerEntity.builder()
                .id(123L)
                .groupInterview(interview)
                .participantUserId(5L)
                .questionId(30L)
                .videoObjectKey("videos/answer.webm")
                .build();
        analysis = GroupInterviewAnswerAnalysisEntity.builder()
                .id(77L)
                .answer(answer)
                .build();

        when(analysisRepository.findByAnswerId(123L)).thenReturn(Optional.of(analysis));
        when(analysisRepository.findByAnswerIdForUpdate(123L)).thenReturn(Optional.of(analysis));
        GroupQuestionEntity question = mock(GroupQuestionEntity.class);
        when(question.getQuestionText()).thenReturn("지원 동기를 말씀해 주세요.");
        when(questionRepository.findById(30L)).thenReturn(Optional.of(question));
        when(jobRoleRepository.findById(2L)).thenReturn(Optional.of(
                JobRoleEntity.builder().category("IT/개발").name("백엔드").build()
        ));

        service = new GroupInterviewAnswerAnalysisJobService(
                analysisRepository,
                questionRepository,
                jobRoleRepository,
                blinkingService,
                gazeService,
                poseService,
                speechService,
                new ObjectMapper(),
                transactionTemplate
        );
    }

    @Test
    void analyzesOneVideoWithAllExistingFastApiClientsAndStoresOneResult() {
        when(speechService.analyzeSpeech(
                eq("minio"), eq("videos/answer.webm"), eq("IT/개발"),
                eq(10L), eq(123L), eq(30L), anyString()
        )).thenReturn(Map.of("result", Map.of(
                "text_script", "안녕하세요",
                "filler_word_analysis", Map.of("total_count", 1),
                "repeated_expressions", List.of(),
                "speech_pace_analysis", Map.of("characters_per_second", 4.2),
                "silence_analysis", Map.of("total_silence_seconds", 1.0),
                "ai_evaluation", Map.of("feedback", "좋습니다")
        )));
        when(blinkingService.analyzeBlinking("videos/answer.webm"))
                .thenReturn(Map.of("result", Map.of("blink_count", 10)));
        when(gazeService.analyzeGaze("videos/answer.webm"))
                .thenReturn(Map.of("result", Map.of("center_ratio", 0.8)));
        when(poseService.analyzePosture("videos/answer.webm"))
                .thenReturn(List.of(PoseAnalysisResponse.builder().timestamp(1.0).build()));

        service.analyzeAsync(123L);

        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(analysis.getSpeechText()).isEqualTo("안녕하세요");
        assertThat(analysis.getSpeechResultJson()).contains("filler_word_analysis");
        assertThat(analysis.getAiEvaluationJson()).contains("좋습니다");
        assertThat(analysis.getEyeBlinkingResultJson()).contains("blink_count");
        assertThat(analysis.getEyeGazeResultJson()).contains("center_ratio");
        assertThat(analysis.getPoseResultJson()).contains("timestamp");
    }

    @Test
    void analysisFailureIsStoredWithoutThrowingToUploadFlow() {
        when(speechService.analyzeSpeech(anyString(), anyString(), any(), anyLong(),
                anyLong(), anyLong(), anyString()))
                .thenThrow(new IllegalStateException("FastAPI unavailable"));

        service.analyzeAsync(123L);

        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysis.getErrorMessage()).contains("FastAPI unavailable");
        verifyNoInteractions(blinkingService, gazeService, poseService);
    }
}
