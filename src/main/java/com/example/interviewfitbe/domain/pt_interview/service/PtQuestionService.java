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

    /*
     * PT 발표자료를 기반으로 예상 질문 생성
     */
    @Transactional
    public List<PtQuestionResponse> generateQuestions(
            Long ptInterviewId
    ) {

        /*
         * 1. PT 면접 조회
         */
        PtInterviewEntity ptInterview =
                ptInterviewRepository
                        .findById(ptInterviewId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "존재하지 않는 PT 면접입니다."
                                )
                        );

        /*
         * 2. 발표자료 조회
         */
        PtDocumentEntity document =
                ptDocumentRepository
                        .findByPtInterviewId(
                                ptInterviewId
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "업로드된 PT 발표자료가 없습니다."
                                )
                        );

        /*
         * 문서 분석 완료 여부 확인
         */
        if (
                document.getAnalysisStatus()
                        != PtDocumentStatus.COMPLETED
        ) {

            throw new IllegalStateException(
                    "PT 발표자료 분석이 완료되지 않았습니다."
            );
        }

        /*
         * 3. 슬라이드 조회
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
         * 4. Gemini에 전달할 슬라이드 내용 구성
         */
        String slidesText =
                buildSlidesText(slides);

        String documentSummary =
                document.getSummary();

        /*
         * 전체 요약이 비어 있으면
         * 슬라이드 텍스트를 대신 사용
         */
        if (
                documentSummary == null
                        || documentSummary.isBlank()
        ) {

            documentSummary = slidesText;
        }

        /*
         * 5. FastAPI → Gemini 질문 생성
         */
        PtQuestionGenerateResponse response =
                ptQuestionAnalysisService
                        .generateQuestions(
                                documentSummary,
                                slidesText,
                                DEFAULT_QUESTION_COUNT
                        );

        if (
                response == null
                        || response.questions() == null
                        || response.questions().isEmpty()
        ) {

            throw new IllegalStateException(
                    "PT 예상 질문 생성 결과가 없습니다."
            );
        }

        /*
         * 기존 질문 삭제
         *
         * 재생성할 경우 중복 방지
         */
        ptQuestionRepository
                .deleteAllByPtInterviewId(
                        ptInterviewId
                );

        /*
         * 6. 질문 DB 저장
         */
        List<PtQuestionResponse> results =
                new ArrayList<>();

        int fallbackNumber = 1;

        for (
                PtQuestionResult questionResult
                : response.questions()
        ) {

            if (
                    questionResult.question() == null
                            || questionResult
                            .question()
                            .isBlank()
            ) {
                continue;
            }

            Integer questionNumber =
                    questionResult.questionNumber();

            if (questionNumber == null) {
                questionNumber =
                        fallbackNumber;
            }

            PtQuestionEntity entity =
                    PtQuestionEntity.create(
                            ptInterview,
                            questionNumber,
                            questionResult.question(),
                            questionResult.reason()
                    );

            PtQuestionEntity saved =
                    ptQuestionRepository
                            .save(entity);

            results.add(
                    new PtQuestionResponse(
                            saved.getId(),
                            saved.getQuestionNumber(),
                            saved.getQuestion(),
                            saved.getReason()
                    )
            );

            fallbackNumber++;
        }

        if (results.isEmpty()) {

            throw new IllegalStateException(
                    "저장할 수 있는 PT 예상 질문이 없습니다."
            );
        }

        return results;
    }

    /*
     * 저장된 PT 질문 조회
     */
    @Transactional(readOnly = true)
    public List<PtQuestionResponse> getQuestions(
            Long ptInterviewId
    ) {

        if (
                !ptInterviewRepository
                        .existsById(ptInterviewId)
        ) {

            throw new IllegalArgumentException(
                    "존재하지 않는 PT 면접입니다."
            );
        }

        return ptQuestionRepository
                .findAllByPtInterviewIdOrderByQuestionNumberAsc(
                        ptInterviewId
                )
                .stream()
                .map(
                        question ->
                                new PtQuestionResponse(
                                        question.getId(),
                                        question.getQuestionNumber(),
                                        question.getQuestion(),
                                        question.getReason()
                                )
                )
                .toList();
    }

    /*
     * Gemini에 전달할 슬라이드별 내용 구성
     */
    private String buildSlidesText(
            List<PtSlideEntity> slides
    ) {

        StringBuilder builder =
                new StringBuilder();

        for (PtSlideEntity slide : slides) {

            builder.append(
                    "[슬라이드 "
            );

            builder.append(
                    slide.getSlideNumber()
            );

            builder.append("]\n");

            if (
                    slide.getTitle() != null
                            && !slide.getTitle().isBlank()
            ) {

                builder.append("제목: ")
                        .append(
                                slide.getTitle()
                        )
                        .append("\n");
            }

            if (
                    slide.getExtractedText() != null
                            && !slide
                            .getExtractedText()
                            .isBlank()
            ) {

                builder.append("원본 내용:\n")
                        .append(
                                slide.getExtractedText()
                        )
                        .append("\n");
            }

            if (
                    slide.getSummary() != null
                            && !slide
                            .getSummary()
                            .isBlank()
            ) {

                builder.append("요약:\n")
                        .append(
                                slide.getSummary()
                        )
                        .append("\n");
            }

            builder.append("\n");
        }

        return builder.toString();
    }
}