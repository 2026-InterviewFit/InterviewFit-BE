package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.GroupAnswerCompletionType;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewAnswerEntity;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewAnswerRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupInterviewAnswerCompletionServiceTest {

    private final GroupInterviewAnswerRepository answerRepository =
            mock(GroupInterviewAnswerRepository.class);
    private final GroupInterviewAnswerCompletionService service =
            new GroupInterviewAnswerCompletionService(answerRepository);

    @Test
    void completesExistingAnswer() {
        GroupInterviewAnswerEntity answer = GroupInterviewAnswerEntity.builder()
                .id(123L)
                .build();
        Instant completedAt = Instant.parse("2026-08-20T00:00:00Z");
        when(answerRepository.findById(123L)).thenReturn(Optional.of(answer));

        service.complete(123L, GroupAnswerCompletionType.COMPLETED, completedAt);

        assertThat(answer.getCompletionType()).isEqualTo(GroupAnswerCompletionType.COMPLETED);
        assertThat(answer.getAnswerCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void missingAnswerFailsInsteadOfSilentlyContinuing() {
        when(answerRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete(
                123L,
                GroupAnswerCompletionType.COMPLETED,
                Instant.now()
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("answerId=123");
    }
}
