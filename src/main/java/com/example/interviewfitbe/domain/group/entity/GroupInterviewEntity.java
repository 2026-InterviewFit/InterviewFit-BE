package com.example.interviewfitbe.domain.group.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_interviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupInterviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // 방 제목
    private String title;


    // 방장 유저 ID
    private Long hostUserId;


    // 직무 ID
    private Long jobRoleId;


    // 최대 참여 인원 (2~4명)
    private Integer maxParticipants;


    // 예약 시작 시간
    private LocalDateTime scheduledStartTime;


    // 실제 면접 시작 시간
    private LocalDateTime startedAt;


    // 실제 면접 종료 시간
    private LocalDateTime endedAt;


    // LiveKit Room 이름
    private String livekitRoomName;


    // WAITING, IN_PROGRESS, COMPLETED
    private String roomStatus;


    // 생성 시간
    @CreationTimestamp
    private LocalDateTime createdAt;


    // 수정 시간
    @UpdateTimestamp
    private LocalDateTime updatedAt;


    @Builder
    public GroupInterviewEntity(
            String title,
            Long hostUserId,
            Long jobRoleId,
            Integer maxParticipants,
            LocalDateTime scheduledStartTime,
            String livekitRoomName,
            String roomStatus) {

        this.title = title;
        this.hostUserId = hostUserId;
        this.jobRoleId = jobRoleId;
        this.maxParticipants = maxParticipants;
        this.scheduledStartTime = scheduledStartTime;
        this.livekitRoomName = livekitRoomName;
        this.roomStatus = roomStatus;
    }


    // 면접 시작 처리
    public void startInterview() {

        this.roomStatus = "IN_PROGRESS";
        this.startedAt = LocalDateTime.now();
    }


    // 면접 종료 처리
    public void finishInterview() {

        this.roomStatus = "COMPLETED";
        this.endedAt = LocalDateTime.now();
    }
}