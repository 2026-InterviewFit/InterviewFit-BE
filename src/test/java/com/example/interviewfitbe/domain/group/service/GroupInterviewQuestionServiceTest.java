package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.GroupQuestionEntity;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewRepository;
import com.example.interviewfitbe.domain.group.repository.GroupQuestionRepository;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.repository.JobRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupInterviewQuestionServiceTest {

    private GroupQuestionRepository questionRepository;
    private GroupInterviewQuestionService service;

    @BeforeEach
    void setUp() {
        questionRepository = mock(GroupQuestionRepository.class);
        JobRoleRepository jobRoleRepository = mock(JobRoleRepository.class);
        GroupInterviewRepository interviewRepository = mock(GroupInterviewRepository.class);
        service = new GroupInterviewQuestionService(
                questionRepository,
                jobRoleRepository,
                interviewRepository
        );

        when(jobRoleRepository.findById(2L)).thenReturn(Optional.of(mock(JobRoleEntity.class)));
        GroupInterviewEntity room = mock(GroupInterviewEntity.class);
        when(room.getJobRoleId()).thenReturn(2L);
        when(interviewRepository.findById(10L)).thenReturn(Optional.of(room));
        when(questionRepository.findFirstByRoundNumberAndQuestionText(
                1,
                GroupInterviewQuestionService.SELF_INTRODUCTION_QUESTION
        )).thenReturn(Optional.of(question(1L, GroupInterviewQuestionService.SELF_INTRODUCTION_QUESTION)));
        when(questionRepository.findAllByRoundNumberAndJobRoleIsNullOrderByIdAsc(2))
                .thenReturn(List.of(question(20L, "2-1"), question(21L, "2-2")));
        when(questionRepository.findAllByRoundNumberAndJobRoleIsNullOrderByIdAsc(3))
                .thenReturn(List.of(question(30L, "3-1"), question(31L, "3-2"), question(32L, "3-3")));
        when(questionRepository.findAllByRoundNumberAndJobRoleIdOrderByIdAsc(4, 2L))
                .thenReturn(List.of(question(40L, "4-1"), question(41L, "4-2")));
    }

    @Test
    void sameRoomAlwaysReturnsSameQuestionSet() {
        var first = service.getGroupInterviewQuestions(2L, 10L);
        var second = service.getGroupInterviewQuestions(2L, 10L);

        assertThat(second).extracting("id")
                .containsExactlyElementsOf(first.stream().map(response -> response.getId()).toList());
    }

    @Test
    void previewUsesSameSelectionAsInterviewFlow() {
        long roomId = 10L;
        var questions = service.getGroupInterviewQuestions(2L, roomId);

        assertThat(questions.get(1).getId())
                .isEqualTo(GroupQuestionSelector.select(
                        questionRepository.findAllByRoundNumberAndJobRoleIsNullOrderByIdAsc(2), roomId, 2
                ).getId());
        assertThat(questions.get(2).getId())
                .isEqualTo(GroupQuestionSelector.select(
                        questionRepository.findAllByRoundNumberAndJobRoleIsNullOrderByIdAsc(3), roomId, 3
                ).getId());
        assertThat(questions.get(3).getId())
                .isEqualTo(GroupQuestionSelector.select(
                        questionRepository.findAllByRoundNumberAndJobRoleIdOrderByIdAsc(4, 2L), roomId, 4
                ).getId());
    }

    private GroupQuestionEntity question(Long id, String text) {
        return GroupQuestionEntity.builder()
                .id(id)
                .questionText(text)
                .build();
    }
}
