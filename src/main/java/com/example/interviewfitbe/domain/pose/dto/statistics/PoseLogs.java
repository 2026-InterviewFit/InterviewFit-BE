package com.example.interviewfitbe.domain.pose.dto.statistics;

import com.example.interviewfitbe.domain.pose.entity.PoseBadLogEntity;
import com.example.interviewfitbe.domain.pose.entity.PoseNotDetectedLogEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PoseLogs {
    List<PoseBadLogEntity> badLogs;
    List<PoseNotDetectedLogEntity> notDetectedLogs;
}
