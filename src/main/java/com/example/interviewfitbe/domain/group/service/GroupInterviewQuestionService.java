package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.dto.GroupQuestionResponse;
import com.example.interviewfitbe.domain.group.entity.GroupQuestionEntity;
import com.example.interviewfitbe.domain.group.repository.GroupQuestionRepository;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.repository.JobRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupInterviewQuestionService {

    public static final String SELF_INTRODUCTION_QUESTION = "간단하게 자기소개를 해주세요.";

    private final GroupQuestionRepository groupQuestionRepository;
    private final JobRoleRepository jobRoleRepository;

    /**
     * 다대일 면접 방 시작 시 1~4라운드 질문 세트를 구성하여 반환
     */
    public List<GroupQuestionResponse> getGroupInterviewQuestions(Long jobRoleId) {
        List<GroupQuestionEntity> selectedQuestions = new ArrayList<>();

        // 1라운드: 자기소개
        selectedQuestions.add(selfIntroductionQuestion());

        // 2라운드: 공통 역량 질문
        List<GroupQuestionEntity> round2 = groupQuestionRepository.findByRoundNumber(2);
        if (!round2.isEmpty()) {
            selectedQuestions.add(randomQuestion(round2));
        }

        // 3라운드: 압박 면접 질문
        List<GroupQuestionEntity> round3 = groupQuestionRepository.findByRoundNumber(3);
        if (!round3.isEmpty()) {
            selectedQuestions.add(randomQuestion(round3));
        }

        // 4라운드: 직무 토의형 질문 (선택한 직무 연동)
        JobRoleEntity jobRole = jobRoleRepository.findById(jobRoleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 직무 ID입니다: " + jobRoleId));

        List<GroupQuestionEntity> round4 = groupQuestionRepository.findByRoundNumberAndJobRole(4, jobRole);
        if (!round4.isEmpty()) {
            selectedQuestions.add(randomQuestion(round4));
        }

        return selectedQuestions.stream()
                .map(GroupQuestionResponse::from)
                .collect(Collectors.toList());
    }

    private GroupQuestionEntity randomQuestion(List<GroupQuestionEntity> questions) {
        List<GroupQuestionEntity> shuffled = new ArrayList<>(questions);
        Collections.shuffle(shuffled);
        return shuffled.get(0);
    }

    private GroupQuestionEntity selfIntroductionQuestion() {
        return groupQuestionRepository
                .findFirstByRoundNumberAndQuestionText(1, SELF_INTRODUCTION_QUESTION)
                .orElseThrow(() -> new IllegalStateException("자기소개 질문 데이터가 없습니다."));
    }
}
