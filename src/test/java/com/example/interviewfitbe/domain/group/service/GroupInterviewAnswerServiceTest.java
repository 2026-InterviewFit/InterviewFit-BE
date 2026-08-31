package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewAnswerEntity;
import com.example.interviewfitbe.domain.group.entity.GroupAnswerCompletionType;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewAnswerRepository;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewRepository;
import com.example.interviewfitbe.domain.group.repository.GroupParticipantRepository;
import com.example.interviewfitbe.domain.video_storage.service.VideoService;
import com.example.interviewfitbe.domain.video_storage.service.MinioVideoStreamingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class GroupInterviewAnswerServiceTest {

    private GroupInterviewAnswerRepository answerRepository;
    private VideoService videoService;
    private GroupInterviewAnswerService service;
    private GroupInterviewAnswerAnalysisService analysisService;
    private MinioVideoStreamingService streamingService;
    private GroupInterviewEntity interview;

    @BeforeEach
    void setUp() {
        answerRepository = mock(GroupInterviewAnswerRepository.class);
        videoService = mock(VideoService.class);
        analysisService = mock(GroupInterviewAnswerAnalysisService.class);
        streamingService = mock(MinioVideoStreamingService.class);
        interview = mock(GroupInterviewEntity.class);
        when(interview.getId()).thenReturn(10L);
        service = new GroupInterviewAnswerService(
                answerRepository,
                mock(GroupInterviewRepository.class),
                mock(GroupParticipantRepository.class),
                videoService,
                analysisService,
                streamingService
        );
    }

    @Test
    void participantUploadsVideoUsingExistingMinioService() throws Exception {
        GroupInterviewAnswerEntity answer = GroupInterviewAnswerEntity.builder()
                .id(123L)
                .groupInterview(interview)
                .participantUserId(5L)
                .round(1)
                .questionIndex(1)
                .build();
        MockMultipartFile file = new MockMultipartFile(
                "file", "answer.webm", "video/webm;codecs=vp8", new byte[]{1, 2, 3}
        );
        when(answerRepository.findByRoomIdAndIdForUpdate(10L, 123L))
                .thenReturn(Optional.of(answer));
        when(videoService.uploadVideoToMinio(file)).thenReturn("videos/group-answer.webm");

        var response = service.uploadVideo(10L, 123L, 5L, file);

        assertThat(response.isVideoUploaded()).isTrue();
        assertThat(response.getVideoObjectKey()).isEqualTo("videos/group-answer.webm");
        assertThat(answer.getVideoContentType()).isEqualTo("video/webm");
        verify(answerRepository).saveAndFlush(answer);
        verify(analysisService).createForUploadedAnswer(answer);
    }

    @Test
    void duplicateVideoUploadReturnsConflictBeforeMinioUpload() {
        GroupInterviewAnswerEntity answer = GroupInterviewAnswerEntity.builder()
                .id(123L)
                .groupInterview(interview)
                .participantUserId(5L)
                .videoObjectKey("videos/already.webm")
                .build();
        when(answerRepository.findByRoomIdAndIdForUpdate(10L, 123L))
                .thenReturn(Optional.of(answer));
        MockMultipartFile file = new MockMultipartFile(
                "file", "answer.webm", "video/webm", new byte[]{1}
        );

        assertThatThrownBy(() -> service.uploadVideo(10L, 123L, 5L, file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 영상이 업로드");
        verifyNoInteractions(videoService);
    }

    @Test
    void skippedAnswerIsExcludedFromVideoUploadAndAnalysis() {
        GroupInterviewAnswerEntity answer = GroupInterviewAnswerEntity.builder()
                .id(123L)
                .groupInterview(interview)
                .participantUserId(5L)
                .build();
        answer.complete(GroupAnswerCompletionType.RECORDING_FAILED, java.time.Instant.now());
        when(answerRepository.findByRoomIdAndIdForUpdate(10L, 123L))
                .thenReturn(Optional.of(answer));
        MockMultipartFile file = new MockMultipartFile(
                "file", "answer.webm", "video/webm", new byte[]{1}
        );

        assertThatThrownBy(() -> service.uploadVideo(10L, 123L, 5L, file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("건너뛴 답변");

        verifyNoInteractions(videoService, analysisService);
    }

    @Test
    void ownerCanStreamCompletedVideo() {
        GroupInterviewAnswerEntity answer = answer(5L, "videos/answer.webm");
        answer.complete(GroupAnswerCompletionType.COMPLETED, java.time.Instant.now());
        when(answerRepository.findByRoomIdAndId(10L, 123L)).thenReturn(Optional.of(answer));
        ResponseEntity<StreamingResponseBody> expected = ResponseEntity.ok(output -> {});
        when(streamingService.stream("videos/answer.webm", "bytes=0-99")).thenReturn(expected);

        assertThat(service.streamMyVideo(10L, 123L, 5L, "bytes=0-99")).isSameAs(expected);
    }

    @Test
    void ownerCanStreamTimedOutVideo() {
        GroupInterviewAnswerEntity answer = answer(5L, "videos/answer.webm");
        answer.complete(GroupAnswerCompletionType.TIMED_OUT, java.time.Instant.now());
        when(answerRepository.findByRoomIdAndId(10L, 123L)).thenReturn(Optional.of(answer));
        ResponseEntity<StreamingResponseBody> expected = ResponseEntity.ok(output -> {});
        when(streamingService.stream("videos/answer.webm", null)).thenReturn(expected);

        assertThat(service.streamMyVideo(10L, 123L, 5L, null)).isSameAs(expected);
    }

    @Test
    void anotherParticipantCannotStreamVideo() {
        when(answerRepository.findByRoomIdAndId(10L, 123L))
                .thenReturn(Optional.of(answer(6L, "videos/answer.webm")));

        assertThatThrownBy(() -> service.streamMyVideo(10L, 123L, 5L, null))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(streamingService);
    }

    @Test
    void missingAnswerReturnsNotFound() {
        when(answerRepository.findByRoomIdAndId(10L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.streamMyVideo(10L, 999L, 5L, null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(streamingService);
    }

    @Test
    void answerFromAnotherRoomReturnsNotFound() {
        when(answerRepository.findByRoomIdAndId(99L, 123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.streamMyVideo(99L, 123L, 5L, null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(streamingService);
    }

    @Test
    void answerWithoutVideoReturnsNotFound() {
        when(answerRepository.findByRoomIdAndId(10L, 123L))
                .thenReturn(Optional.of(answer(5L, null)));

        assertThatThrownBy(() -> service.streamMyVideo(10L, 123L, 5L, null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(streamingService);
    }

    @Test
    void skippedAndRecordingFailedAnswersCannotBeStreamed() {
        for (GroupAnswerCompletionType type : new GroupAnswerCompletionType[]{
                GroupAnswerCompletionType.SKIPPED, GroupAnswerCompletionType.RECORDING_FAILED}) {
            GroupInterviewAnswerEntity answer = answer(5L, "videos/answer.webm");
            answer.complete(type, java.time.Instant.now());
            when(answerRepository.findByRoomIdAndId(10L, 123L)).thenReturn(Optional.of(answer));

            assertThatThrownBy(() -> service.streamMyVideo(10L, 123L, 5L, null))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }
        verifyNoInteractions(streamingService);
    }

    private GroupInterviewAnswerEntity answer(Long participantUserId, String objectKey) {
        return GroupInterviewAnswerEntity.builder()
                .id(123L)
                .groupInterview(interview)
                .participantUserId(participantUserId)
                .round(1)
                .questionIndex(0)
                .videoObjectKey(objectKey)
                .build();
    }
}
