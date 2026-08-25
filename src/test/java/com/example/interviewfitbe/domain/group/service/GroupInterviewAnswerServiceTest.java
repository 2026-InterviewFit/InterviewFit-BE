package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewAnswerEntity;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewAnswerRepository;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewRepository;
import com.example.interviewfitbe.domain.group.repository.GroupParticipantRepository;
import com.example.interviewfitbe.domain.video_storage.service.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class GroupInterviewAnswerServiceTest {

    private GroupInterviewAnswerRepository answerRepository;
    private VideoService videoService;
    private GroupInterviewAnswerService service;
    private GroupInterviewAnswerAnalysisService analysisService;
    private GroupInterviewEntity interview;

    @BeforeEach
    void setUp() {
        answerRepository = mock(GroupInterviewAnswerRepository.class);
        videoService = mock(VideoService.class);
        analysisService = mock(GroupInterviewAnswerAnalysisService.class);
        interview = mock(GroupInterviewEntity.class);
        when(interview.getId()).thenReturn(10L);
        service = new GroupInterviewAnswerService(
                answerRepository,
                mock(GroupInterviewRepository.class),
                mock(GroupParticipantRepository.class),
                videoService,
                analysisService
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
}
