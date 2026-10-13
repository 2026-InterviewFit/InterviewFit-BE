package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.GroupQuestionEntity;

import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.concurrent.ThreadLocalRandom;

final class GroupQuestionSelector {

    private GroupQuestionSelector() {
    }

    static GroupQuestionEntity select(
            List<GroupQuestionEntity> candidates,
            Set<String> excludedQuestionTexts
    ) {
        return select(candidates, excludedQuestionTexts, ThreadLocalRandom.current());
    }

    static GroupQuestionEntity select(
            List<GroupQuestionEntity> candidates,
            Set<String> excludedQuestionTexts,
            RandomGenerator random
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        List<GroupQuestionEntity> preferred = candidates.stream()
                .filter(question -> excludedQuestionTexts == null
                        || !excludedQuestionTexts.contains(normalize(question.getQuestionText())))
                .toList();
        List<GroupQuestionEntity> pool = preferred.isEmpty() ? candidates : preferred;
        int index = random.nextInt(pool.size());

        return pool.get(index);
    }

    static String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }
}
