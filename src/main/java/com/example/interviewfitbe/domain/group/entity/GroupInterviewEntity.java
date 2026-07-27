package com.example.interviewfitbe.domain.group.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_interviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupInterviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long hostUserId; // 방장 유저 ID
    private Long jobRoleId;  // 직무 ID

    private String roomStatus; // "WAITING", "IN_PROGRESS", "FINISHED"

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Builder
    public GroupInterviewEntity(Long hostUserId, Long jobRoleId, String roomStatus) {
        this.hostUserId = hostUserId;
        this.jobRoleId = jobRoleId;
        this.roomStatus = roomStatus;
    }

    public void updateStatus(String status) {
        this.roomStatus = status;
    }
}