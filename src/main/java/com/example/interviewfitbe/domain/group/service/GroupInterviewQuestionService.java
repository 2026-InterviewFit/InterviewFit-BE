package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.dto.GroupQuestionResponse;
import com.example.interviewfitbe.domain.group.entity.GroupQuestionEntity;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewRepository;
import com.example.interviewfitbe.domain.group.repository.GroupQuestionRepository;
import com.example.interviewfitbe.domain.job.repository.JobRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupInterviewQuestionService {

    public static final String SELF_INTRODUCTION_QUESTION = "간단하게 자기소개를 해주세요.";

    private final GroupQuestionRepository groupQuestionRepository;
    private final JobRoleRepository jobRoleRepository;
    private final GroupInterviewRepository groupInterviewRepository;

    /**
     * 다대일 면접 방 시작 시 1~4라운드 질문 세트를 구성하여 반환
     */
    public List<GroupQuestionResponse> getGroupInterviewQuestions(Long jobRoleId) {
        return getGroupInterviewQuestions(jobRoleId, null);
    }

    /**
     * roomId가 전달되면 실제 면접 진행과 동일한 기준으로 질문을 선택한다.
     * roomId가 없는 기존 호출은 이전처럼 매번 무작위 질문 세트를 반환한다.
     */
    public List<GroupQuestionResponse> getGroupInterviewQuestions(Long jobRoleId, Long roomId) {
        jobRoleRepository.findById(jobRoleId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 직무 ID입니다: " + jobRoleId
                ));

        if (roomId != null) {
            var room = groupInterviewRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "존재하지 않는 면접 방입니다. ID: " + roomId
                    ));
            if (!jobRoleId.equals(room.getJobRoleId())) {
                throw new IllegalArgumentException(
                        "면접 방의 직무와 요청한 jobRoleId가 일치하지 않습니다."
                );
            }
        }

        List<GroupQuestionEntity> selectedQuestions = new ArrayList<>();

        // 1라운드: 자기소개
        selectedQuestions.add(selfIntroductionQuestion());

        // 2라운드: 공통 역량 질문
        List<GroupQuestionEntity> round2 = groupQuestionRepository
                .findAllByRoundNumberAndJobRoleIsNullOrderByIdAsc(2);
        if (!round2.isEmpty()) {
            selectedQuestions.add(GroupQuestionSelector.select(round2, roomId, 2));
        }

        // 3라운드: 압박 면접 질문
        List<GroupQuestionEntity> round3 = groupQuestionRepository
                .findAllByRoundNumberAndJobRoleIsNullOrderByIdAsc(3);
        if (!round3.isEmpty()) {
            selectedQuestions.add(GroupQuestionSelector.select(round3, roomId, 3));
        }

        // 4라운드: 직무 토의형 질문 (선택한 직무 연동)
        List<GroupQuestionEntity> round4 = groupQuestionRepository
                .findAllByRoundNumberAndJobRoleIdOrderByIdAsc(4, jobRoleId);
        if (!round4.isEmpty()) {
            selectedQuestions.add(GroupQuestionSelector.select(round4, roomId, 4));
        }

        return selectedQuestions.stream()
                .map(GroupQuestionResponse::from)
                .collect(Collectors.toList());
    }

    private GroupQuestionEntity selfIntroductionQuestion() {
        return groupQuestionRepository
                .findFirstByRoundNumberAndQuestionText(1, SELF_INTRODUCTION_QUESTION)
                .orElseThrow(() -> new IllegalStateException("자기소개 질문 데이터가 없습니다."));
    }
}
