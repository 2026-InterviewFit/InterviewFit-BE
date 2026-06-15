package com.example.interviewfitbe.domain.question.controller;

import com.example.interviewfitbe.domain.question.dto.QuestionDto;
import com.example.interviewfitbe.domain.question.dto.QuestionRequest;
import com.example.interviewfitbe.domain.question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/question")
@RequiredArgsConstructor
@RestController
public class QuestionController {
    private final QuestionService questionService;

    @PostMapping("/create")
    public ResponseEntity<List<QuestionDto>> createQuestions(@RequestBody QuestionRequest request) {
        List<QuestionDto> responses = this.questionService.createQuestions(request);
        return ResponseEntity.ok(responses);
    }
}
