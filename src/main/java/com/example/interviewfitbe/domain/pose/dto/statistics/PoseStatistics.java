package com.example.interviewfitbe.domain.pose.dto.statistics;

import lombok.*;

@Getter
@NoArgsConstructor
public class PoseStatistics {
    // 전체 집계
    int badCount;
    int badDuration;

    // BAD
    int shoulderBadCount;
    int faceBadCount;

    int shoulderBadDuration;
    int faceBadDuration;

    // 이전 상태 (연속 구간 체크용)
    boolean prevShoulderBad;
    boolean prevFaceBad;


    // 어깨 BAD 처리 (공통 로직)
    public void handleShoulderBad(boolean isBad) {

        if (isBad) {
            increaseBadDuration();
            increaseShoulderBadDuration();
        }

        if (isBad && !prevShoulderBad) {
            increaseBadCount();
            increaseShoulderBadCount();
        }

        prevShoulderBad = isBad;
    }

    // 얼굴 BAD 처리 (공통 로직)
    public void handleFaceBad(boolean isBad) {

        if (isBad) {
            increaseBadDuration();
            increaseFaceBadDuration();
        }

        if (isBad && !prevFaceBad) {
            increaseBadCount();
            increaseFaceBadCount();
        }

        prevFaceBad = isBad;
    }


    // increment methods
    private void increaseBadDuration() {
        this.badDuration++;
    }

    private void increaseBadCount() {
        this.badCount++;
    }

    private void increaseShoulderBadDuration() {
        this.shoulderBadDuration++;
    }

    private void increaseShoulderBadCount() {
        this.shoulderBadCount++;
    }

    private void increaseFaceBadDuration() {
        this.faceBadDuration++;
    }

    private void increaseFaceBadCount() {
        this.faceBadCount++;
    }
}
