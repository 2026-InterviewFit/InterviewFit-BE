package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.GroupQuestionEntity;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class GroupQuestionSelector {

    private GroupQuestionSelector() {
    }

    static GroupQuestionEntity select(
            List<GroupQuestionEntity> candidates,
            Long roomId,
            int round
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        int index = roomId == null
                ? ThreadLocalRandom.current().nextInt(candidates.size())
                : Math.floorMod(Long.hashCode(roomId * 31 + round), candidates.size());

        return candidates.get(index);
    }
}
