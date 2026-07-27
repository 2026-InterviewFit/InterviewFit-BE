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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupInterviewQuestionService {

    private final GroupQuestionRepository groupQuestionRepository;
    private final JobRoleRepository jobRoleRepository;

    /**
     * 다대일 면접 방 시작 시 1~3라운드 질문 세트를 구성하여 반환
     */
    public List<GroupQuestionResponse> getGroupInterviewQuestions(Long jobRoleId) {
        List<GroupQuestionEntity> selectedQuestions = new ArrayList<>();

        // 1라운드: 공통 역량 질문 (랜덤 또는 첫 번째 항목)
        List<GroupQuestionEntity> round1 = groupQuestionRepository.findByRoundNumber(1);
        if (!round1.isEmpty()) {
            selectedQuestions.add(round1.get(0));
        }

        // 2라운드: 압박 면접 질문
        List<GroupQuestionEntity> round2 = groupQuestionRepository.findByRoundNumber(2);
        if (!round2.isEmpty()) {
            selectedQuestions.add(round2.get(0));
        }

        // 3라운드: 직무 토의형 질문 (선택한 직무 연동)
        JobRoleEntity jobRole = jobRoleRepository.findById(jobRoleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 직무 ID입니다: " + jobRoleId));

        List<GroupQuestionEntity> round3 = groupQuestionRepository.findByRoundNumberAndJobRole(3, jobRole);
        if (!round3.isEmpty()) {
            selectedQuestions.add(round3.get(0));
        }

        return selectedQuestions.stream()
                .map(GroupQuestionResponse::from)
                .collect(Collectors.toList());
    }
}