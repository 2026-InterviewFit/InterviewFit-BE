package com.example.interviewfitbe.domain.eye_gaze.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GazeDirection {

    LEFT("왼쪽"),
    RIGHT("오른쪽"),
    UP("위"),
    DOWN("아래"),
    UP_LEFT("좌상단"),
    UP_RIGHT("우상단"),
    DOWN_LEFT("좌하단"),
    DOWN_RIGHT("우하단");

    private final String displayName;
}