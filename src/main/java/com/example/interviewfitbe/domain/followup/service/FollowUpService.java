package com.example.interviewfitbe.domain.followup.service;

import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateRequest;
import com.example.interviewfitbe.domain.followup.dto.FollowUpCreateResponse;
import com.example.interviewfitbe.domain.followup.entity.FollowUpQuestionEntity;
import com.example.interviewfitbe.domain.followup.repository.FollowUpQuestionRepository;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.service.JobRoleService;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import com.example.interviewfitbe.domain.question.repository.QuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowUpService {

    private final FollowUpQuestionRepository followUpQuestionRepository;
    private final InterviewRepository interviewRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final QuestionRepository questionRepository;
    private final JobRoleService jobRoleService;
    private final FollowUpAnalysisService followUpAnalysisService;


    /**
     * 기본 질문 + 사용자 답변을 기반으로
     * 꼬리질문 1개를 생성하고 DB에 저장한다.
     *
     * 현재 기본 질문은 FE 하드코딩 상태이므로
     * sourceQuestionId 없이 originalQuestion 문자열만으로도
     * 꼬리질문 생성이 가능하도록 한다.
     *
     * 추후 일반 질문 담당 팀원이 DB Question 연결을 완료하면
     * sourceQuestionId가 전달될 경우 자동으로 관계를 저장한다.
     */
    @Transactional
    public FollowUpCreateResponse createAndSave(
            FollowUpCreateRequest request
    ) {

        validateRequest(request);


        /*
         * Interview ID는 현재 실제 면접 생성 API에서
         * 전달받을 수 있으므로 있으면 조회한다.
         */
        InterviewEntity interview =
                findInterviewOrNull(
                        request.getInterviewId()
                );


        /*
         * 현재 하드코딩 질문에는 Question DB ID가 없으므로
         * sourceQuestionId가 null이어도 허용한다.
         */
        QuestionEntity sourceQuestion =
                findQuestionOrNull(
                        request.getSourceQuestionId()
                );


        /*
         * sourceAnswerId 역시 현재는 선택값.
         *
         * STT 결과를 answer 문자열로 직접 전달할 수 있다.
         */
        InterviewAnswerEntity sourceAnswer =
                findAnswerOrNull(
                        request.getSourceAnswerId()
                );


        /*
         * interviewId는 없지만
         * DB Question이 연결된 경우
         * Question에서 Interview를 가져온다.
         */
        if (
                interview == null
                        && sourceQuestion != null
                        && sourceQuestion.getInterview() != null
        ) {

            interview =
                    sourceQuestion.getInterview();
        }


        /*
         * sourceAnswer만 존재하는 경우
         * Answer에서 Interview를 찾는다.
         */
        if (
                interview == null
                        && sourceAnswer != null
        ) {

            interview =
                    sourceAnswer.getInterviewSafely();
        }


        /*
         * 직무명 결정.
         */
        String jobName =
                resolveJobName(
                        request,
                        interview,
                        sourceQuestion
                );


        /*
         * 현재 화면의 기본 질문 문자열.
         */
        String originalQuestion =
                resolveOriginalQuestion(
                        request,
                        sourceQuestion
                );


        /*
         * 사용자의 기본 질문 답변.
         *
         * 이후 STT 결과가 여기에 들어온다.
         */
        String answerText =
                resolveAnswerText(
                        request,
                        sourceAnswer
                );


        String interviewStyle =
                resolveInterviewStyle(
                        request.getInterviewStyle()
                );


        /*
         * Spring → FastAPI → Gemini
         *
         * 여기서 실제 꼬리질문 1개 생성.
         */
        String generatedFollowUpQuestion =
                followUpAnalysisService
                        .createFollowUpQuestion(
                                jobName,
                                originalQuestion,
                                answerText,
                                interviewStyle
                        );


        /*
         * 생성 결과 저장.
         *
         * sourceQuestion/sourceAnswer는 현재 null일 수 있다.
         */
        FollowUpQuestionEntity saved =
                followUpQuestionRepository.save(
                        FollowUpQuestionEntity.builder()
                                .interview(interview)
                                .sourceQuestion(sourceQuestion)
                                .sourceAnswer(sourceAnswer)
                                .job(jobName)
                                .originalQuestion(originalQuestion)
                                .answer(answerText)
                                .followUpQuestion(
                                        generatedFollowUpQuestion
                                )
                                .interviewStyle(interviewStyle)
                                .retrievedContextJson(
                                        request.getRetrievedContextJson()
                                )
                                .resultJson(
                                        request.getResultJson()
                                )
                                .build()
                );


        return FollowUpCreateResponse.builder()
                .message(
                        "꼬리질문 생성 및 저장 완료"
                )
                .followUpQuestionId(
                        saved.getId()
                )
                .interviewId(
                        interview == null
                                ? null
                                : interview.getId()
                )
                .sourceQuestionId(
                        sourceQuestion == null
                                ? null
                                : sourceQuestion.getId()
                )
                .sourceAnswerId(
                        sourceAnswer == null
                                ? null
                                : sourceAnswer.getId()
                )
                .job(
                        saved.getJob()
                )
                .originalQuestion(
                        saved.getOriginalQuestion()
                )
                .answer(
                        saved.getAnswer()
                )
                .followUpQuestion(
                        saved.getFollowUpQuestion()
                )
                .interviewStyle(
                        saved.getInterviewStyle()
                )
                .build();
    }


    /**
     * 요청값 검증.
     *
     * 현재는 sourceQuestionId 필수가 아니다.
     *
     * 원 질문:
     * originalQuestion 또는 sourceQuestionId 중 하나
     *
     * 원 답변:
     * answer 또는 sourceAnswerId 중 하나
     */
    private void validateRequest(
            FollowUpCreateRequest request
    ) {

        boolean hasQuestionId =
                request.getSourceQuestionId()
                        != null;


        boolean hasQuestionText =
                request.getOriginalQuestion()
                        != null
                        && !request
                        .getOriginalQuestion()
                        .isBlank();


        if (
                !hasQuestionId
                        && !hasQuestionText
        ) {

            throw new IllegalArgumentException(
                    "originalQuestion 또는 sourceQuestionId 중 하나는 필수입니다."
            );
        }


        boolean hasAnswerText =
                request.getAnswer()
                        != null
                        && !request
                        .getAnswer()
                        .isBlank();


        boolean hasAnswerId =
                request.getSourceAnswerId()
                        != null;


        if (
                !hasAnswerText
                        && !hasAnswerId
        ) {

            throw new IllegalArgumentException(
                    "answer 텍스트 또는 sourceAnswerId 값 중 하나는 필수입니다."
            );
        }
    }


    /**
     * Interview 조회.
     */
    private InterviewEntity findInterviewOrNull(
            Long interviewId
    ) {

        if (
                interviewId == null
        ) {
            return null;
        }


        return interviewRepository
                .findById(interviewId)
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "해당 면접 정보를 찾을 수 없습니다. interviewId="
                                                + interviewId
                                )
                );
    }


    /**
     * Question 조회.
     *
     * 현재 하드코딩 질문에서는 ID가 없으므로
     * null 허용.
     */
    private QuestionEntity findQuestionOrNull(
            Long sourceQuestionId
    ) {

        if (
                sourceQuestionId == null
        ) {
            return null;
        }


        return questionRepository
                .findById(sourceQuestionId)
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "해당 원 질문 정보를 찾을 수 없습니다. sourceQuestionId="
                                                + sourceQuestionId
                                )
                );
    }


    /**
     * 기존 Answer 조회.
     */
    private InterviewAnswerEntity findAnswerOrNull(
            Long sourceAnswerId
    ) {

        if (
                sourceAnswerId == null
        ) {
            return null;
        }


        return interviewAnswerRepository
                .findById(sourceAnswerId)
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "해당 원 답변 정보를 찾을 수 없습니다. sourceAnswerId="
                                                + sourceAnswerId
                                )
                );
    }


    /**
     * 직무명 결정.
     *
     * 우선순위:
     *
     * 1. Interview의 JobRole
     * 2. Question의 JobRole
     * 3. request.jobRoleId
     * 4. request.job
     * 5. "일반"
     */
    private String resolveJobName(
            FollowUpCreateRequest request,
            InterviewEntity interview,
            QuestionEntity sourceQuestion
    ) {

        if (
                interview != null
                        && interview.getJobRole() != null
        ) {

            return interview
                    .getJobRole()
                    .getName();
        }


        if (
                sourceQuestion != null
                        && sourceQuestion.getJobRole() != null
        ) {

            return sourceQuestion
                    .getJobRole()
                    .getName();
        }


        if (
                request.getJobRoleId()
                        != null
        ) {

            JobRoleEntity jobRole =
                    jobRoleService
                            .getJobRoleEntity(
                                    request.getJobRoleId()
                            );

            return jobRole.getName();
        }


        if (
                request.getJob()
                        != null
                        && !request
                        .getJob()
                        .isBlank()
        ) {

            return request.getJob();
        }


        return "일반";
    }


    /**
     * 기본 질문 문자열 결정.
     */
    private String resolveOriginalQuestion(
            FollowUpCreateRequest request,
            QuestionEntity sourceQuestion
    ) {

        /*
         * 현재 하드코딩 질문은
         * 이 값으로 들어온다.
         */
        if (
                request.getOriginalQuestion()
                        != null
                        && !request
                        .getOriginalQuestion()
                        .isBlank()
        ) {

            return request
                    .getOriginalQuestion()
                    .trim();
        }


        /*
         * 나중에 Question DB 연결이 완료된 경우.
         */
        if (
                sourceQuestion != null
                        && sourceQuestion.getQuestion() != null
                        && !sourceQuestion
                        .getQuestion()
                        .isBlank()
        ) {

            return sourceQuestion
                    .getQuestion()
                    .trim();
        }


        throw new IllegalArgumentException(
                "원 질문 내용을 찾을 수 없습니다."
        );
    }


    /**
     * 사용자 답변 결정.
     */
    private String resolveAnswerText(
            FollowUpCreateRequest request,
            InterviewAnswerEntity sourceAnswer
    ) {

        /*
         * 이후 Whisper STT 결과가
         * request.answer로 들어온다.
         */
        if (
                request.getAnswer()
                        != null
                        && !request
                        .getAnswer()
                        .isBlank()
        ) {

            return request
                    .getAnswer()
                    .trim();
        }


        /*
         * 저장된 답변 ID가 있는 경우.
         */
        if (
                sourceAnswer != null
                        && sourceAnswer.getAnswerText() != null
                        && !sourceAnswer
                        .getAnswerText()
                        .isBlank()
        ) {

            return sourceAnswer
                    .getAnswerText()
                    .trim();
        }


        throw new IllegalArgumentException(
                "answer 텍스트를 찾을 수 없습니다."
        );
    }


    /**
     * 면접 스타일 기본값.
     */
    private String resolveInterviewStyle(
            String interviewStyle
    ) {

        if (
                interviewStyle == null
                        || interviewStyle.isBlank()
        ) {

            return "normal";
        }


        return interviewStyle.trim();
    }
}