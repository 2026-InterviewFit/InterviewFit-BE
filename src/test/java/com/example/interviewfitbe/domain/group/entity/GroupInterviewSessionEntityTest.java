package com.example.interviewfitbe.domain.group.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroupInterviewSessionEntityTest {

    @Test
    void completionMovesParticipantThenQuestionAndFinallyFinishes() {
        GroupInterviewSessionEntity session = GroupInterviewSessionEntity.builder().build();
        Instant now = Instant.parse("2026-08-18T00:00:00Z");
        session.start();

        assertThat(complete(session, now, 2))
                .isEqualTo(GroupInterviewSessionEntity.AnswerCompletionResult.NEXT_PARTICIPANT);
        assertThat(session.getCurrentParticipantIndex()).isEqualTo(1);
        assertThat(session.getCurrentRound()).isEqualTo(1);

        assertThat(complete(session, now, 2))
                .isEqualTo(GroupInterviewSessionEntity.AnswerCompletionResult.NEXT_QUESTION);
        assertThat(session.getCurrentParticipantIndex()).isZero();
        assertThat(session.getCurrentRound()).isEqualTo(2);
        assertThat(session.getCurrentQuestionIndex()).isEqualTo(2);

        complete(session, now, 2);
        complete(session, now, 2);
        complete(session, now, 2);
        assertThat(complete(session, now, 2))
                .isEqualTo(GroupInterviewSessionEntity.AnswerCompletionResult.INTERVIEW_FINISHED);
        assertThat(session.getStatus()).isEqualTo(InterviewStatus.FINISHED);
        assertThat(session.getAnswerDeadlineAt()).isNull();
    }

    @Test
    void answerCanOnlyCompleteWhileAnswering() {
        GroupInterviewSessionEntity session = GroupInterviewSessionEntity.builder().build();
        session.start();

        assertThatThrownBy(() -> session.completeCurrentAnswer(2))
                .isInstanceOf(IllegalStateException.class);
    }

    private GroupInterviewSessionEntity.AnswerCompletionResult complete(
            GroupInterviewSessionEntity session,
            Instant now,
            int participantCount
    ) {
        session.startAnswer(now, now.plusSeconds(60));
        return session.completeCurrentAnswer(participantCount);
    }
}
