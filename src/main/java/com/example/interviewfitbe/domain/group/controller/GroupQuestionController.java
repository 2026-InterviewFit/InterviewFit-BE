package com.example.interviewfitbe.domain.group.controller;

import com.example.interviewfitbe.domain.group.dto.GroupQuestionResponse;
import com.example.interviewfitbe.domain.group.service.GroupInterviewQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/group/questions")
@RequiredArgsConstructor
public class GroupQuestionController {

    private final GroupInterviewQuestionService groupInterviewQuestionService;

    /**
     * 다대일 면접 질문 세트 조회 API
     * GET /api/group/questions?jobRoleId=2
     */
    @GetMapping
    public ResponseEntity<List<GroupQuestionResponse>> getQuestions(@RequestParam Long jobRoleId) {
        List<GroupQuestionResponse> response = groupInterviewQuestionService.getGroupInterviewQuestions(jobRoleId);
        return ResponseEntity.ok(response);
    }
}