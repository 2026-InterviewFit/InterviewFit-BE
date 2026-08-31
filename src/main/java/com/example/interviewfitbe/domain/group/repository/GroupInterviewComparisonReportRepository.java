package com.example.interviewfitbe.domain.group.repository;

import com.example.interviewfitbe.domain.group.entity.GroupComparisonScope;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewComparisonReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupInterviewComparisonReportRepository
        extends JpaRepository<GroupInterviewComparisonReportEntity, Long> {
    List<GroupInterviewComparisonReportEntity> findAllByRoomIdAndScopeOrderByRankAscParticipantUserIdAsc(
            Long roomId, GroupComparisonScope scope);

    List<GroupInterviewComparisonReportEntity>
    findAllByRoomIdAndScopeAndQuestionIndexOrderByRankAscParticipantUserIdAsc(
            Long roomId, GroupComparisonScope scope, Integer questionIndex);

    Optional<GroupInterviewComparisonReportEntity>
    findByRoomIdAndParticipantUserIdAndScopeAndQuestionIndex(
            Long roomId, Long participantUserId, GroupComparisonScope scope, Integer questionIndex);

    Optional<GroupInterviewComparisonReportEntity>
    findByRoomIdAndParticipantUserIdAndScopeAndQuestionIndexIsNull(
            Long roomId, Long participantUserId, GroupComparisonScope scope);
}
