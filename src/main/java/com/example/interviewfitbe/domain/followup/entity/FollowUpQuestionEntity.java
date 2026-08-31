package com.example.interviewfitbe.domain.followup.entity;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;


@Entity
@Table(name = "follow_up_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FollowUpQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /*
     * 해당 면접.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private InterviewEntity interview;


    /*
     * 원 기본 질문.
     *
     * 현재 FE 질문이 하드코딩 상태이므로
     * Question DB ID가 없을 수 있다.
     *
     * 따라서 null 허용.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "source_question_id",
            nullable = true
    )
    private QuestionEntity sourceQuestion;


    /*
     * 원 기본 질문 답변.
     *
     * 현재는 STT 텍스트를 answer 컬럼에
     * 직접 저장할 수 있으므로 선택값.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_answer_id")
    private InterviewAnswerEntity sourceAnswer;


    /*
     * 직무명.
     */
    private String job;


    /*
     * 꼬리질문의 기준이 된 기본 질문 문자열.
     *
     * 현재 하드코딩 질문이 이곳에 저장된다.
     */
    @Column(columnDefinition = "TEXT")
    private String originalQuestion;


    /*
     * 기본 질문에 대한 사용자 답변.
     *
     * 이후 Whisper STT 결과를 저장한다.
     */
    @Column(columnDefinition = "TEXT")
    private String answer;


    /*
     * Gemini가 생성한 꼬리질문.
     */
    @Column(
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String followUpQuestion;


    /*
     * normal / friendly / pressure
     */
    private String interviewStyle;


    @Column(columnDefinition = "TEXT")
    private String retrievedContextJson;


    @Column(columnDefinition = "TEXT")
    private String resultJson;


    @CreationTimestamp
    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;
}