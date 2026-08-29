package com.example.interviewfitbe.domain.community.ranking.service;

import com.example.interviewfitbe.domain.community.answer.entity.CommunityBestAnswerEntity;
import com.example.interviewfitbe.domain.community.answer.repository.CommunityBestAnswerRepository;
import com.example.interviewfitbe.domain.community.ranking.dto.CommunityRankerAnswerResponse;
import com.example.interviewfitbe.domain.community.ranking.dto.CommunityRankerProfileResponse;
import com.example.interviewfitbe.domain.community.ranking.dto.CommunityRankingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommunityRankingService {

    private static final int TOP_LIMIT = 10;

    private final CommunityBestAnswerRepository bestAnswerRepository;

    /**
     * 랭킹은 사용자가 명시적으로 공개한 community_best_answers만 이용한다.
     * 비공개 면접 기록은 랭킹 계산에 사용하지 않는다.
     *
     * 순위 기준:
     * 1) 공개 우수답변 평균 점수
     * 2) 공개 우수답변 수
     * 3) 받은 좋아요 합계
     */
    @Transactional(readOnly = true)
    public List<CommunityRankingResponse> getTopRankings() {
        List<RankerAggregate> aggregates = buildAggregates();

        List<CommunityRankingResponse> result = new ArrayList<>();

        for (int i = 0; i < Math.min(TOP_LIMIT, aggregates.size()); i++) {
            RankerAggregate aggregate = aggregates.get(i);
            result.add(toRankingResponse(aggregate, i + 1));
        }

        return result;
    }

    /**
     * 랭커 상세에서도 공개 허용된 우수답변만 노출한다.
     */
    @Transactional(readOnly = true)
    public CommunityRankerProfileResponse getRankerProfile(Long userId) {
        List<RankerAggregate> aggregates = buildAggregates();

        for (int i = 0; i < aggregates.size(); i++) {
            RankerAggregate aggregate = aggregates.get(i);

            if (aggregate.userId().equals(userId)) {
                List<CommunityRankerAnswerResponse> answers = aggregate.answers()
                        .stream()
                        .sorted(
                                Comparator
                                        .comparing(
                                                CommunityBestAnswerEntity::getScore,
                                                Comparator.reverseOrder()
                                        )
                                        .thenComparing(
                                                answer -> answer.getLikeCount() == null
                                                        ? 0
                                                        : answer.getLikeCount(),
                                                Comparator.reverseOrder()
                                        )
                                        .thenComparing(
                                                CommunityBestAnswerEntity::getCreatedAt,
                                                Comparator.reverseOrder()
                                        )
                        )
                        .map(CommunityRankerAnswerResponse::from)
                        .toList();

                return CommunityRankerProfileResponse.builder()
                        .userId(aggregate.userId())
                        .rank(i + 1)
                        .name(maskName(aggregate.userName()))
                        .score(aggregate.averageScore())
                        .tier(resolveTier(aggregate.averageScore()))
                        .publishedAnswerCount(aggregate.answers().size())
                        .totalLikeCount(aggregate.totalLikeCount())
                        .answers(answers)
                        .build();
            }
        }

        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "공개 우수답변이 있는 랭커를 찾을 수 없습니다. userId=" + userId
        );
    }

    private List<RankerAggregate> buildAggregates() {
        List<CommunityBestAnswerEntity> answers =
                bestAnswerRepository.findAllByOrderByCreatedAtDesc();

        Map<Long, List<CommunityBestAnswerEntity>> grouped =
                new LinkedHashMap<>();

        for (CommunityBestAnswerEntity answer : answers) {
            Long userId = answer.getUser().getId();
            grouped.computeIfAbsent(userId, ignored -> new ArrayList<>())
                    .add(answer);
        }

        List<RankerAggregate> aggregates = new ArrayList<>();

        for (Map.Entry<Long, List<CommunityBestAnswerEntity>> entry
                : grouped.entrySet()) {
            List<CommunityBestAnswerEntity> userAnswers = entry.getValue();

            if (userAnswers.isEmpty()) {
                continue;
            }

            int averageScore = (int) Math.round(
                    userAnswers.stream()
                            .mapToInt(CommunityBestAnswerEntity::getScore)
                            .average()
                            .orElse(0.0)
            );

            int totalLikeCount = userAnswers.stream()
                    .mapToInt(answer ->
                            answer.getLikeCount() == null
                                    ? 0
                                    : answer.getLikeCount()
                    )
                    .sum();

            CommunityBestAnswerEntity representative = userAnswers.stream()
                    .max(
                            Comparator
                                    .comparing(CommunityBestAnswerEntity::getScore)
                                    .thenComparing(answer ->
                                            answer.getLikeCount() == null
                                                    ? 0
                                                    : answer.getLikeCount()
                                    )
                                    .thenComparing(CommunityBestAnswerEntity::getCreatedAt)
                    )
                    .orElse(userAnswers.get(0));

            aggregates.add(
                    new RankerAggregate(
                            entry.getKey(),
                            representative.getUser().getName(),
                            averageScore,
                            totalLikeCount,
                            representative,
                            List.copyOf(userAnswers)
                    )
            );
        }

        aggregates.sort(
                Comparator
                        .comparingInt(RankerAggregate::averageScore)
                        .reversed()
                        .thenComparing(
                                Comparator.comparingInt(
                                        (RankerAggregate ranker) ->
                                                ranker.answers().size()
                                ).reversed()
                        )
                        .thenComparing(
                                Comparator.comparingInt(
                                        RankerAggregate::totalLikeCount
                                ).reversed()
                        )
                        .thenComparing(RankerAggregate::userId)
        );

        return aggregates;
    }

    private CommunityRankingResponse toRankingResponse(
            RankerAggregate aggregate,
            int rank
    ) {
        CommunityBestAnswerEntity representative =
                aggregate.representative();

        return CommunityRankingResponse.builder()
                .userId(aggregate.userId())
                .rank(rank)
                .name(maskName(aggregate.userName()))
                .score(aggregate.averageScore())
                .tier(resolveTier(aggregate.averageScore()))
                .metricType("score")
                .metricValue("평균 " + aggregate.averageScore() + "점")
                .script(representative.getAnswerScript())
                .publishedAnswerCount(aggregate.answers().size())
                .totalLikeCount(aggregate.totalLikeCount())
                .role(representative.getJobRole().getName())
                .build();
    }

    private String resolveTier(int score) {
        if (score >= 95) return "Challenger";
        if (score >= 92) return "Grandmaster";
        if (score >= 89) return "Master";
        if (score >= 86) return "Diamond";
        if (score >= 83) return "Platinum";
        if (score >= 80) return "Gold";
        return "Silver";
    }

    private String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "익명";
        }

        String trimmed = name.trim();

        if (trimmed.length() == 1) {
            return "*";
        }

        return trimmed.substring(0, 1)
                + "*".repeat(trimmed.length() - 1);
    }

    private record RankerAggregate(
            Long userId,
            String userName,
            int averageScore,
            int totalLikeCount,
            CommunityBestAnswerEntity representative,
            List<CommunityBestAnswerEntity> answers
    ) {
    }
}
