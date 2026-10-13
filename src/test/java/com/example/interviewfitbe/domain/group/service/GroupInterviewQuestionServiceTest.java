package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.GroupQuestionEntity;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewRepository;
import com.example.interviewfitbe.domain.group.repository.GroupQuestionRepository;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewSessionRepository;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewSessionEntity;
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
    private GroupInterviewSessionRepository sessionRepository;

    @BeforeEach
    void setUp() {
        questionRepository = mock(GroupQuestionRepository.class);
        JobRoleRepository jobRoleRepository = mock(JobRoleRepository.class);
        GroupInterviewRepository interviewRepository = mock(GroupInterviewRepository.class);
        sessionRepository = mock(GroupInterviewSessionRepository.class);
        service = new GroupInterviewQuestionService(
                questionRepository,
                jobRoleRepository,
                interviewRepository,
                sessionRepository
        );

        when(jobRoleRepository.findById(2L)).thenReturn(Optional.of(mock(JobRoleEntity.class)));
        GroupInterviewEntity room = mock(GroupInterviewEntity.class);
        when(room.getJobRoleId()).thenReturn(2L);
        when(interviewRepository.findById(10L)).thenReturn(Optional.of(room));
        when(questionRepository.findFirstByRoundNumberAndQuestionText(
                1,
                GroupInterviewQuestionService.SELF_INTRODUCTION_QUESTION
        )).thenReturn(Optional.of(question(1L, GroupInterviewQuestionService.SELF_INTRODUCTION_QUESTION)));
        when(questionRepository.findAllByRoundNumberAndJobRoleIsNullAndSelectionEnabledTrueOrderByIdAsc(1))
                .thenReturn(List.of(question(10L, "1-1"), question(11L, "1-2")));
        when(questionRepository.findAllByRoundNumberAndJobRoleIsNullAndSelectionEnabledTrueOrderByIdAsc(2))
                .thenReturn(List.of(question(20L, "2-1"), question(21L, "2-2")));
        when(questionRepository.findAllByRoundNumberAndJobRoleIsNullAndSelectionEnabledTrueOrderByIdAsc(3))
                .thenReturn(List.of(question(30L, "3-1"), question(31L, "3-2"), question(32L, "3-3")));
        when(questionRepository.findSelectableRoundFourQuestions(2L))
                .thenReturn(List.of(question(40L, "4-1"), question(41L, "4-2")));
    }

    @Test
    void sameRoomAlwaysReturnsSameQuestionSet() {
        GroupInterviewSessionEntity session = GroupInterviewSessionEntity.builder().build();
        session.assignQuestion(1, 10L);
        session.assignQuestion(2, 20L);
        session.assignQuestion(3, 30L);
        session.assignQuestion(4, 40L);
        when(sessionRepository.findByInterviewId(10L)).thenReturn(Optional.of(session));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question(10L, "1-1")));
        when(questionRepository.findById(20L)).thenReturn(Optional.of(question(20L, "2-1")));
        when(questionRepository.findById(30L)).thenReturn(Optional.of(question(30L, "3-1")));
        when(questionRepository.findById(40L)).thenReturn(Optional.of(question(40L, "4-1")));

        var first = service.getGroupInterviewQuestions(2L, 10L);
        var second = service.getGroupInterviewQuestions(2L, 10L);

        assertThat(second).extracting("id")
                .containsExactlyElementsOf(first.stream().map(response -> response.getId()).toList());
    }

    @Test
    void previewReturnsOneQuestionFromEachRoundPool() {
        var questions = service.getGroupInterviewQuestions(2L, 10L);

        assertThat(questions).hasSize(4);
        assertThat(questions.get(0).getId()).isIn(10L, 11L);
        assertThat(questions.get(1).getId()).isIn(20L, 21L);
        assertThat(questions.get(2).getId()).isIn(30L, 31L, 32L);
        assertThat(questions.get(3).getId()).isIn(40L, 41L);
    }

    private GroupQuestionEntity question(Long id, String text) {
        return GroupQuestionEntity.builder()
                .id(id)
                .questionText(text)
                .build();
    }
}
