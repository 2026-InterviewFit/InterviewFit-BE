package com.example.interviewfitbe.domain.question.controller;

import com.example.interviewfitbe.domain.question.dto.QuestionDto;
import com.example.interviewfitbe.domain.question.dto.QuestionRequest;
import com.example.interviewfitbe.domain.coverletter.service.PdfService;
import com.example.interviewfitbe.domain.question.dto.QuestionResponse;
import com.example.interviewfitbe.domain.question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/api/question")
@RequiredArgsConstructor
@RestController
public class QuestionController {
    private final QuestionService questionService;
    private final PdfService pdfService;

    @GetMapping
    public ResponseEntity<List<QuestionResponse>> getAllQuestions() {
        List<QuestionResponse> results = questionService.getAllQuestions();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionResponse> getQuestion(@PathVariable Long id) {
        QuestionResponse result = questionService.getQuestion(id);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<QuestionDto>> createQuestions(
            @RequestPart("request") QuestionRequest request,
            @RequestPart(value = "coverLetterFile", required = false) MultipartFile coverLetterFile
    ) {
        if (coverLetterFile != null && !coverLetterFile.isEmpty()) {
            String coverLetter = pdfService.extractText(coverLetterFile);
            request.updateCoverLetter(coverLetter);
        }
        List<QuestionDto> responses = questionService.createQuestions(request.getUserId(), request);
        return ResponseEntity.ok(responses);
    }
}
