package com.example.interviewfitbe.domain.question.controller;

import com.example.interviewfitbe.domain.question.dto.QuestionRequest;
import com.example.interviewfitbe.domain.question.dto.QuestionResponse;
import com.example.interviewfitbe.domain.question.service.QuestionService;
import com.example.interviewfitbe.global.jwt.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/question")
@RequiredArgsConstructor
@RestController
public class QuestionController {
    private final QuestionService questionService;

    @PostMapping("/one-to-one/create")
    public ResponseEntity<List<QuestionResponse>> createQuestions(@RequestBody QuestionRequest request) {
        Long userId = AuthUtils.getCurrentUserId();
        List<QuestionResponse> responses = questionService.createQuestions(userId, request);
        return ResponseEntity.ok(responses);
    }
}
