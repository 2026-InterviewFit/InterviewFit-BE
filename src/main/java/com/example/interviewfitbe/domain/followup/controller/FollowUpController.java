package com.example.interviewfitbe.domain.followup.controller;

import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateRequest;
import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateResponse;
import com.example.interviewfitbe.domain.followup.dto.FollowUpTranscriptionResponse;
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

    private final FollowUpTranscriptionService followUpTranscriptionService;


    /**
     * =====================================================
     * 꼬리질문 생성
     * =====================================================
     *
     * 현재 기본 질문 문자열 + STT 답변 문자열을 받아
     * FastAPI/Gemini를 통해 꼬리질문 1개 생성.
     */
    @PostMapping("/create")
    public FollowUpCreateResponse createFollowUpQuestion(
            @RequestBody FollowUpCreateRequest request
    ) {

        /*
         * FE에서 userId를 직접 보내더라도 신뢰하지 않고
         * 로그인한 JWT 사용자 ID로 덮어쓴다.
         */
        Long userId =
                AuthUtils.getCurrentUserId();


        request.setUserId(
                userId
        );


        return followUpService
                .createAndSave(
                        request
                );
    }


    /**
     * =====================================================
     * 꼬리질문용 답변 STT
     * =====================================================
     *
     * FE에서 현재 질문에 대한 답변 음성 Blob을 전송한다.
     *
     * Spring은 해당 파일을 FastAPI에 전달하고,
     * Whisper STT 결과만 FE에 반환한다.
     *
     * 요청:
     *
     * multipart/form-data
     *
     * file = answer.webm
     *
     * 응답:
     *
     * {
     *   "text": "사용자의 실제 답변..."
     * }
     */
    @PostMapping(
            value = "/transcribe",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public FollowUpTranscriptionResponse transcribeAnswer(
            @RequestPart("file")
            MultipartFile file
    ) {

        return followUpTranscriptionService
                .transcribe(
                        file
                );
    }
}