package com.example.interviewfitbe.domain.pt_interview.service;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.interview.service.InterviewAnswerAnalysisJobService;
import com.example.interviewfitbe.domain.pt_interview.dto.PtAnswerResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtAnswerUploadResponse;
import com.example.interviewfitbe.domain.pt_interview.entity.PtQuestionEntity;
import com.example.interviewfitbe.domain.pt_interview.repository.PtQuestionRepository;
import com.example.interviewfitbe.domain.video_storage.dto.VideoUrlResponse;
import com.example.interviewfitbe.domain.video_storage.entity.VideoType;
import com.example.interviewfitbe.domain.video_storage.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PtAnswerService {

    private final PtQuestionRepository ptQuestionRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final VideoService videoService;
    private final InterviewAnswerAnalysisJobService
            interviewAnswerAnalysisJobService;

    /**
     * PT 질문 답변 영상 업로드
     */
    public PtAnswerUploadResponse uploadAnswer(
            Long ptInterviewId,
            Long ptQuestionId,
            MultipartFile videoFile
    ) throws IOException {

        PtQuestionEntity ptQuestion =
                ptQuestionRepository
                        .findWithPtInterviewById(
                                ptQuestionId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 PT 질문입니다. "
                                                + "ptQuestionId="
                                                + ptQuestionId
                                )
                        );

        validateQuestionBelongsToInterview(
                ptInterviewId,
                ptQuestion
        );

        /*
         * 현재는 질문 하나당 답변 하나만 저장한다.
         */
        if (
                interviewAnswerRepository
                        .existsByPtQuestion_Id(
                                ptQuestionId
                        )
        ) {

            throw new IllegalStateException(
                    "이미 답변이 등록된 PT 질문입니다. "
                            + "ptQuestionId="
                            + ptQuestionId
            );
        }

        InterviewEntity interview =
                ptQuestion
                        .getPtInterview()
                        .getInterview();

        if (interview == null) {

            throw new IllegalStateException(
                    "PT 면접에 연결된 인터뷰 정보를 찾을 수 없습니다. "
                            + "ptInterviewId="
                            + ptInterviewId
            );
        }

        /*
         * 영상 MinIO 업로드 및 videos 테이블 저장.
         *
         * 현재 VideoType에 PT 답변 전용 타입이 없으므로
         * PT_PRESENTATION을 사용한다.
         */
        VideoUrlResponse videoResponse =
                videoService.saveVideoFile(
                        interview.getId(),
                        videoFile,
                        VideoType.PT_PRESENTATION
                );

        String videoObjectKey =
                videoResponse.getVideoUrl();

        InterviewAnswerEntity answer =
                InterviewAnswerEntity.builder()
                        .interview(interview)
                        .question(null)
                        .ptQuestion(ptQuestion)
                        .answerOrder(
                                ptQuestion.getQuestionNumber()
                        )
                        .startTime(null)
                        .endTime(null)
                        .answerText(null)
                        .build();

        answer.markUploaded();

        InterviewAnswerEntity savedAnswer =
                interviewAnswerRepository.save(
                        answer
                );

        /*
         * 비동기로 시선, 깜빡임, 자세 분석 실행.
         */
        interviewAnswerAnalysisJobService
                .analyzeAnswerAsync(
                        savedAnswer.getId(),
                        videoObjectKey
                );

        return new PtAnswerUploadResponse(
                savedAnswer.getId(),
                ptInterviewId,
                ptQuestion.getId(),
                ptQuestion.getQuestionNumber(),
                ptQuestion.getQuestion(),
                savedAnswer.getAnalysisStatus()
        );
    }

    /**
     * PT 면접 답변 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PtAnswerResponse> getAnswers(
            Long ptInterviewId
    ) {

        return interviewAnswerRepository
                .findAllByPtQuestion_PtInterview_IdOrderByAnswerOrderAsc(
                        ptInterviewId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 특정 PT 질문의 최근 답변 조회
     */
    @Transactional(readOnly = true)
    public PtAnswerResponse getAnswer(
            Long ptInterviewId,
            Long ptQuestionId
    ) {

        PtQuestionEntity ptQuestion =
                ptQuestionRepository
                        .findWithPtInterviewById(
                                ptQuestionId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 PT 질문입니다. "
                                                + "ptQuestionId="
                                                + ptQuestionId
                                )
                        );

        validateQuestionBelongsToInterview(
                ptInterviewId,
                ptQuestion
        );

        InterviewAnswerEntity answer =
                interviewAnswerRepository
                        .findTopByPtQuestion_IdOrderByIdDesc(
                                ptQuestionId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "해당 PT 질문의 답변을 찾을 수 없습니다. "
                                                + "ptQuestionId="
                                                + ptQuestionId
                                )
                        );

        return toResponse(
                answer
        );
    }

    private void validateQuestionBelongsToInterview(
            Long ptInterviewId,
            PtQuestionEntity ptQuestion
    ) {

        if (
                ptQuestion.getPtInterview() == null
                        || ptQuestion
                        .getPtInterview()
                        .getId() == null
                        || !ptQuestion
                        .getPtInterview()
                        .getId()
                        .equals(ptInterviewId)
        ) {

            throw new IllegalArgumentException(
                    "해당 질문은 요청한 PT 면접에 속하지 않습니다. "
                            + "ptInterviewId="
                            + ptInterviewId
                            + ", ptQuestionId="
                            + ptQuestion.getId()
            );
        }
    }

    private PtAnswerResponse toResponse(
            InterviewAnswerEntity answer
    ) {

        PtQuestionEntity ptQuestion =
                answer.getPtQuestion();

        if (ptQuestion == null) {

            throw new IllegalStateException(
                    "PT 질문이 연결되지 않은 답변입니다. "
                            + "answerId="
                            + answer.getId()
            );
        }

        return new PtAnswerResponse(
                answer.getId(),
                ptQuestion.getId(),
                ptQuestion.getQuestionNumber(),
                ptQuestion.getQuestion(),
                answer.getAnswerText(),
                answer.getAnalysisStatus(),
                answer.getCreatedAt()
        );
    }
}
