package com.example.interviewfitbe.domain.pt_interview.service;

import com.example.interviewfitbe.domain.pt_interview.dto.PtQuestionGenerateResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtQuestionResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtQuestionResult;
import com.example.interviewfitbe.domain.pt_interview.entity.PtDocumentEntity;
import com.example.interviewfitbe.domain.pt_interview.entity.PtInterviewEntity;
import com.example.interviewfitbe.domain.pt_interview.entity.PtQuestionEntity;
import com.example.interviewfitbe.domain.pt_interview.entity.PtSlideEntity;
import com.example.interviewfitbe.domain.pt_interview.enums.PtDocumentStatus;
import com.example.interviewfitbe.domain.pt_interview.repository.PtDocumentRepository;
import com.example.interviewfitbe.domain.pt_interview.repository.PtInterviewRepository;
import com.example.interviewfitbe.domain.pt_interview.repository.PtQuestionRepository;
import com.example.interviewfitbe.domain.pt_interview.repository.PtSlideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PtQuestionService {

    private static final int DEFAULT_QUESTION_COUNT = 5;

    private final PtInterviewRepository ptInterviewRepository;
    private final PtDocumentRepository ptDocumentRepository;
    private final PtSlideRepository ptSlideRepository;
    private final PtQuestionRepository ptQuestionRepository;
    private final PtQuestionAnalysisService ptQuestionAnalysisService;

    /**
     * PT 발표자료를 기반으로 예상 질문 생성
     *
     * 동일 PT 면접에 대해 이미 질문이 생성되어 있으면
     * Gemini를 다시 호출하지 않고 기존 질문을 반환한다.
     */
    @Transactional
    public synchronized List<PtQuestionResponse> generateQuestions(
            Long ptInterviewId
    ) {

        /*
         * 1. PT 면접 존재 여부 확인
         */
        PtInterviewEntity ptInterview =
                ptInterviewRepository
                        .findById(ptInterviewId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "존재하지 않는 PT 면접입니다. ptInterviewId="
                                                + ptInterviewId
                                )
                        );

        /*
         * 2. 이미 생성된 질문이 있으면 그대로 반환
         *
         * waiting 페이지에서 API가 중복 호출되더라도
         * 질문을 다시 생성/저장하지 않는다.
         */
        List<PtQuestionEntity> existingQuestions =
                ptQuestionRepository
                        .findAllByPtInterviewIdOrderByQuestionNumberAsc(
                                ptInterviewId
                        );

        if (!existingQuestions.isEmpty()) {

            return existingQuestions
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        /*
         * 3. 발표자료 조회
         */
        PtDocumentEntity document =
                ptDocumentRepository
                        .findByPtInterviewId(ptInterviewId)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "업로드된 PT 발표자료가 없습니다. ptInterviewId="
                                                + ptInterviewId
                                )
                        );

        /*
         * 4. 문서 분석 완료 여부 확인
         */
        if (
                document.getAnalysisStatus()
                        != PtDocumentStatus.COMPLETED
        ) {

            throw new IllegalStateException(
                    "PT 발표자료 분석이 완료되지 않았습니다. 현재 상태="
                            + document.getAnalysisStatus()
            );
        }

        /*
         * 5. 슬라이드 조회
         */
        List<PtSlideEntity> slides =
                ptSlideRepository
                        .findAllByDocumentIdOrderBySlideNumberAsc(
                                document.getId()
                        );

        if (slides.isEmpty()) {

            throw new IllegalStateException(
                    "분석된 PT 슬라이드가 없습니다."
            );
        }

        /*
         * 6. Gemini에 전달할 슬라이드 내용 구성
         */
        String slidesText =
                buildSlidesText(
                        slides
                );

        if (slidesText.isBlank()) {

            throw new IllegalStateException(
                    "PT 슬라이드에서 질문 생성에 사용할 내용을 찾을 수 없습니다."
            );
        }

        String documentSummary =
                document.getSummary();

        /*
         * 전체 요약이 비어 있으면
         * 슬라이드 원문을 대신 사용
         */
        if (
                documentSummary == null
                        || documentSummary.isBlank()
        ) {

            documentSummary =
                    slidesText;
        }

        /*
         * 7. FastAPI → Gemini 질문 생성
         */
        PtQuestionGenerateResponse response =
                ptQuestionAnalysisService
                        .generateQuestions(
                                documentSummary,
                                slidesText,
                                DEFAULT_QUESTION_COUNT
                        );

        validateQuestionResponse(
                response
        );

        /*
         * 8. 생성된 질문 DB 저장
         */
        List<PtQuestionResponse> results =
                new ArrayList<>();

        int questionNumber = 1;

        for (
                PtQuestionResult questionResult
                : response.questions()
        ) {

            if (questionResult == null) {
                continue;
            }

            String question =
                    normalizeText(
                            questionResult.question()
                    );

            if (question == null) {
                continue;
            }

            String reason =
                    normalizeText(
                            questionResult.reason()
                    );

            PtQuestionEntity entity =
                    PtQuestionEntity.create(
                            ptInterview,
                            questionNumber,
                            question,
                            reason
                    );

            PtQuestionEntity saved =
                    ptQuestionRepository.save(
                            entity
                    );

            results.add(
                    toResponse(
                            saved
                    )
            );

            questionNumber++;

            /*
             * 기본 질문 개수까지만 저장
             */
            if (
                    questionNumber
                            > DEFAULT_QUESTION_COUNT
            ) {
                break;
            }
        }

        if (results.isEmpty()) {

            throw new IllegalStateException(
                    "저장할 수 있는 PT 예상 질문이 없습니다."
            );
        }

        return results;
    }

    /**
     * 저장된 PT 질문 조회
     */
    @Transactional(readOnly = true)
    public List<PtQuestionResponse> getQuestions(
            Long ptInterviewId
    ) {

        if (
                !ptInterviewRepository
                        .existsById(
                                ptInterviewId
                        )
        ) {

            throw new IllegalArgumentException(
                    "존재하지 않는 PT 면접입니다. ptInterviewId="
                            + ptInterviewId
            );
        }

        return ptQuestionRepository
                .findAllByPtInterviewIdOrderByQuestionNumberAsc(
                        ptInterviewId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * FastAPI 질문 생성 응답 검증
     */
    private void validateQuestionResponse(
            PtQuestionGenerateResponse response
    ) {

        if (response == null) {

            throw new IllegalStateException(
                    "FastAPI 질문 생성 응답이 없습니다."
            );
        }

        if (
                response.questions() == null
                        || response.questions().isEmpty()
        ) {

            throw new IllegalStateException(
                    "PT 예상 질문 생성 결과가 없습니다."
            );
        }
    }

    /**
     * Entity → Response
     */
    private PtQuestionResponse toResponse(
            PtQuestionEntity question
    ) {

        return new PtQuestionResponse(
                question.getId(),
                question.getQuestionNumber(),
                question.getQuestion(),
                question.getReason()
        );
    }

    /**
     * Gemini에 전달할 슬라이드별 내용 구성
     */
    private String buildSlidesText(
            List<PtSlideEntity> slides
    ) {

        StringBuilder builder =
                new StringBuilder();

        for (
                PtSlideEntity slide
                : slides
        ) {

            builder.append(
                            "[슬라이드 "
                    )
                    .append(
                            slide.getSlideNumber()
                    )
                    .append(
                            "]\n"
                    );

            String title =
                    normalizeText(
                            slide.getTitle()
                    );

            if (title != null) {

                builder.append(
                                "제목: "
                        )
                        .append(
                                title
                        )
                        .append(
                                "\n"
                        );
            }

            String extractedText =
                    normalizeText(
                            slide.getExtractedText()
                    );

            if (extractedText != null) {

                builder.append(
                                "원본 내용:\n"
                        )
                        .append(
                                extractedText
                        )
                        .append(
                                "\n"
                        );
            }

            String summary =
                    normalizeText(
                            slide.getSummary()
                    );

            if (summary != null) {

                builder.append(
                                "요약:\n"
                        )
                        .append(
                                summary
                        )
                        .append(
                                "\n"
                        );
            }

            builder.append(
                    "\n"
            );
        }

        return builder
                .toString()
                .trim();
    }

    /**
     * 문자열 공백 정리
     */
    private String normalizeText(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return null;
        }

        return value.trim();
    }
}