package com.example.interviewfitbe.domain.coverletter.controller;

import com.example.interviewfitbe.domain.coverletter.dto.CoverLetterDto;
import com.example.interviewfitbe.domain.coverletter.service.CoverLetterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/coverletter")
@RequiredArgsConstructor
@RestController
public class CoverLetterController {
    private final CoverLetterService coverLetterService;

    @GetMapping("/{id}")
    public ResponseEntity<CoverLetterDto> getCoverLetter(@PathVariable Long id) {
        CoverLetterDto result = coverLetterService.getCoverLetter(id);
        return ResponseEntity.ok(result);
    }
}
