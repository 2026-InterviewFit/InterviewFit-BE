package com.example.interviewfitbe.domain.group.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long roomId;   // 속한 방 ID (GroupInterviewEntity의 id)
    private Long userId;   // 참여한 유저 ID
    private String status; // "READY", "JOINED", "COMPLETED"

    @CreationTimestamp
    private LocalDateTime joinedAt;

    @Builder
    public GroupParticipantEntity(Long roomId, Long userId, String status) {
        this.roomId = roomId;
        this.userId = userId;
        this.status = status;
    }

    public void ready() {
        this.status = "READY";
    }

    public void unready() {
        this.status = "JOINED";
    }
}
