package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.GroupQuestionEntity;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Random;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

class GroupQuestionSelectorTest {

    @Test
    void excludesRecentlyUsedQuestionsWhenAnotherCandidateExists() {
        var recent = question(1L);
        var fresh = question(2L);

        for (int attempt = 0; attempt < 30; attempt++) {
            assertThat(GroupQuestionSelector.select(List.of(recent, fresh), Set.of("질문 1")).getId())
                    .isEqualTo(2L);
        }
    }

    @Test
    void fallsBackToFullPoolWhenEveryCandidateWasRecentlyUsed() {
        var selected = GroupQuestionSelector.select(
                List.of(question(1L), question(2L)),
                Set.of("질문 1", "질문 2")
        );

        assertThat(selected.getId()).isIn(1L, 2L);
    }

    @Test
    void thirtyVirtualSessionsProduceDiverseSelections() {
        List<GroupQuestionEntity> pool = LongStream.rangeClosed(1, 30)
                .mapToObj(this::question)
                .toList();
        Set<Long> selectedIds = new HashSet<>();
        Set<String> recentQuestionTexts = new HashSet<>();
        Random seededRandom = new Random(20261013L);

        for (int session = 0; session < 30; session++) {
            GroupQuestionEntity selected = GroupQuestionSelector.select(
                    pool,
                    recentQuestionTexts,
                    seededRandom
            );
            selectedIds.add(selected.getId());
            recentQuestionTexts.add(GroupQuestionSelector.normalize(selected.getQuestionText()));
        }

        assertThat(selectedIds).hasSize(30);
    }

    private GroupQuestionEntity question(long id) {
        return GroupQuestionEntity.builder()
                .id(id)
                .roundNumber(1)
                .questionText("질문 " + id)
                .build();
    }
}
