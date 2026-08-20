package com.example.interviewfitbe.domain.followup.service;

import com.example.interviewfitbe.domain.followup.dto.FollowUpTranscriptionResponse;
import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.exception.code.FastApiErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpTranscriptionService {

    private final WebClient fastApiWebClient;


    /**
     * 꼬리질문 생성을 위한 답변 음성 STT.
     *
     * Spring
     * POST /api/followup/transcribe
     *
     * ↓
     *
     * FastAPI
     * POST /apis/speech/transcribe-file
     *
     * ↓
     *
     * {
     *   "text": "사용자가 실제로 말한 답변"
     * }
     */
    public FollowUpTranscriptionResponse transcribe(
            MultipartFile file
    ) {

        if (
                file == null
                        || file.isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "STT 변환을 위한 음성 파일이 필요합니다."
            );
        }


        try {

            /*
             * MultipartFile을 FastAPI에 전달하기 위한
             * ByteArrayResource 생성.
             *
             * getFilename()을 override하지 않으면
             * multipart의 filename이 사라질 수 있으므로
             * 반드시 원래 파일명을 반환한다.
             */
            ByteArrayResource resource =
                    new ByteArrayResource(
                            file.getBytes()
                    ) {

                        @Override
                        public String getFilename() {

                            String originalFilename =
                                    file.getOriginalFilename();

                            if (
                                    originalFilename == null
                                            || originalFilename.isBlank()
                            ) {

                                return "answer.webm";
                            }

                            return originalFilename;
                        }
                    };


            FollowUpTranscriptionResponse response =
                    fastApiWebClient
                            .post()

                            .uri(
                                    "/apis/speech/transcribe-file"
                            )

                            .contentType(
                                    MediaType.MULTIPART_FORM_DATA
                            )

                            /*
                             * FastAPI:
                             *
                             * file: UploadFile = File(...)
                             *
                             * 이므로 multipart key 이름은
                             * 반드시 "file"이어야 한다.
                             */
                            .body(
                                    BodyInserters
                                            .fromMultipartData(
                                                    "file",
                                                    resource
                                            )
                            )

                            .retrieve()

                            .bodyToMono(
                                    FollowUpTranscriptionResponse.class
                            )

                            /*
                             * Whisper 첫 호출은 모델 초기화 등으로
                             * 시간이 걸릴 수 있으므로 여유 있게 설정.
                             */
                            .timeout(
                                    Duration.ofMinutes(3)
                            )

                            .block();


            if (
                    response == null
                            || response.getText() == null
                            || response
                            .getText()
                            .isBlank()
            ) {

                log.error(
                        "FastAPI STT 응답에 text가 없습니다."
                );

                throw new BaseException(
                        FastApiErrorCode.FASTAPI_SERVER_ERROR
                );
            }


            log.info(
                    "꼬리질문용 STT 완료 - textLength={}",
                    response
                            .getText()
                            .length()
            );


            return response;


        } catch (
                WebClientResponseException e
        ) {

            log.error(
                    "FastAPI STT 요청 실패 - status={}, body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );

            throw new BaseException(
                    FastApiErrorCode.FASTAPI_SERVER_ERROR
            );


        } catch (
                IOException e
        ) {

            log.error(
                    "STT 파일 읽기 실패",
                    e
            );

            throw new BaseException(
                    FastApiErrorCode.FASTAPI_SERVER_ERROR
            );
        }
    }
}