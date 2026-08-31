package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.dto.GroupInterviewAnswerResponse;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewAnswerEntity;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewAnswerRepository;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewRepository;
import com.example.interviewfitbe.domain.group.repository.GroupParticipantRepository;
import com.example.interviewfitbe.domain.video_storage.service.VideoService;
import com.example.interviewfitbe.domain.video_storage.service.MinioVideoStreamingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupInterviewAnswerService {

    private final GroupInterviewAnswerRepository answerRepository;
    private final GroupInterviewRepository interviewRepository;
    private final GroupParticipantRepository participantRepository;
    private final VideoService videoService;
    private final GroupInterviewAnswerAnalysisService analysisService;
    private final MinioVideoStreamingService streamingService;

    @Transactional
    public GroupInterviewAnswerResponse uploadVideo(
            Long roomId,
            Long answerId,
            Long actorUserId,
            MultipartFile file
    ) throws IOException {
        GroupInterviewAnswerEntity answer = answerRepository
                .findByRoomIdAndIdForUpdate(roomId, answerId)
                .orElseThrow(() -> new IllegalArgumentException("해당 방의 답변을 찾을 수 없습니다."));

        if (!answer.getParticipantUserId().equals(actorUserId)) {
            throw new AccessDeniedException("답변자 본인만 답변 영상을 업로드할 수 있습니다.");
        }
        if (answer.isVideoUploaded()) {
            throw new IllegalStateException("이미 영상이 업로드된 답변입니다.");
        }
        if (answer.getCompletionType() != null
                && answer.getCompletionType().excludesAnalysis()) {
            throw new IllegalStateException("건너뛴 답변에는 영상을 업로드할 수 없습니다.");
        }

        String objectKey = videoService.uploadVideoToMinio(file);
        answer.attachVideo(
                objectKey,
                originalFileName(file),
                normalizeContentType(file.getContentType()),
                file.getSize()
        );
        answerRepository.saveAndFlush(answer);
        analysisService.createForUploadedAnswer(answer);
        return GroupInterviewAnswerResponse.from(answer);
    }

    @Transactional(readOnly = true)
    public List<GroupInterviewAnswerResponse> getAnswers(Long roomId, Long actorUserId) {
        if (!interviewRepository.existsById(roomId)) {
            throw new IllegalArgumentException("면접 방을 찾을 수 없습니다.");
        }
        if (!participantRepository.existsByRoomIdAndUserId(roomId, actorUserId)) {
            throw new AccessDeniedException("해당 면접 방의 참가자만 답변을 조회할 수 있습니다.");
        }
        return answerRepository.findAllByGroupInterviewIdOrderByIdAsc(roomId)
                .stream()
                .map(GroupInterviewAnswerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<StreamingResponseBody> streamMyVideo(
            Long roomId,
            Long answerId,
            Long actorUserId,
            String rangeHeader
    ) {
        GroupInterviewAnswerEntity answer = answerRepository.findByRoomIdAndId(roomId, answerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "해당 방의 답변을 찾을 수 없습니다."));
        if (!answer.getParticipantUserId().equals(actorUserId)) {
            throw new AccessDeniedException("답변자 본인만 답변 영상을 조회할 수 있습니다.");
        }
        if (answer.getCompletionType() != null && answer.getCompletionType().excludesAnalysis()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "재생 가능한 답변 영상이 없습니다.");
        }
        if (!answer.isVideoUploaded()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "재생 가능한 답변 영상이 없습니다.");
        }
        return streamingService.stream(answer.getVideoObjectKey(), rangeHeader);
    }

    private String originalFileName(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name == null || name.isBlank() ? "group-answer.webm" : name;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "video/webm";
        }
        int separator = contentType.indexOf(';');
        return (separator < 0 ? contentType : contentType.substring(0, separator)).trim();
    }
}
