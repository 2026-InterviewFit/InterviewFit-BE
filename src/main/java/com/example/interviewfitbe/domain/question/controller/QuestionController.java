package com.example.interviewfitbe.domain.question.controller;

import com.example.interviewfitbe.domain.question.dto.QuestionDto;
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

    @GetMapping
    public ResponseEntity<List<QuestionDto>> getAllQuestions() {
        List<QuestionDto> results = questionService.getAllQuestions();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionDto> getQuestion(@PathVariable Long id) {
        QuestionDto result = questionService.getQuestion(id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/one-to-one/create")
    public ResponseEntity<List<QuestionResponse>> createQuestions(@RequestBody QuestionRequest request) {
        Long userId = AuthUtils.getCurrentUserId();
        List<QuestionResponse> responses = questionService.createQuestions(userId, request);
        return ResponseEntity.ok(responses);
    }
}
