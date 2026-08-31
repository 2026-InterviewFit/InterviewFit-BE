package com.example.interviewfitbe.domain.followup.controller;

import com.example.interviewfitbe.domain.followup.dto.FollowUpAnswerCreateRequest;
import com.example.interviewfitbe.domain.followup.dto.FollowUpAnswerCreateResponse;
import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateRequest;
import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateResponse;
import com.example.interviewfitbe.domain.followup.dto.FollowUpTranscriptionResponse;
import com.example.interviewfitbe.domain.followup.service.FollowUpAnswerService;
import com.example.interviewfitbe.domain.followup.service.FollowUpService;
import com.example.interviewfitbe.domain.followup.service.FollowUpTranscriptionService;
import com.example.interviewfitbe.global.jwt.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/followup")
public class FollowUpController {

    private final FollowUpService followUpService;
    private final FollowUpAnswerService followUpAnswerService;
    private final FollowUpTranscriptionService followUpTranscriptionService;


    /**
     * 꼬리질문 생성.
     */
    @PostMapping("/create")
    public FollowUpCreateResponse createFollowUpQuestion(
            @RequestBody FollowUpCreateRequest request
    ) {
        Long userId = AuthUtils.getCurrentUserId();
        request.setUserId(userId);

        return followUpService.createAndSave(request);
    }


    /**
     * 꼬리질문 답변 저장.
     *
     * 프론트에서 꼬리질문 답변 완료 시 호출한다.
     *
     * 요청:
     *   {
     *     "interviewId": 123,
     *     "followUpQuestionId": 45,
     *     "answerText": "답변 텍스트 (STT 결과)",
     *     "startTime": 62.5,
     *     "endTime": 145.3
     *   }
     *
     * 응답:
     *   FollowUpAnswerCreateResponse (followUpAnswerId 등)
     */
    @PostMapping("/answers")
    public FollowUpAnswerCreateResponse createFollowUpAnswer(
            @RequestBody FollowUpAnswerCreateRequest request
    ) {
        Long userId = AuthUtils.getCurrentUserId();

        return followUpAnswerService.create(userId, request);
    }


    /**
     * 꼬리질문용 답변 STT.
     */
    @PostMapping(
            value = "/transcribe",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public FollowUpTranscriptionResponse transcribeAnswer(
            @RequestPart("file") MultipartFile file
    ) {
        return followUpTranscriptionService.transcribe(file);
    }
}