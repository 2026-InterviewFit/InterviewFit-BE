package com.example.interviewfitbe.domain.coverletter.controller;

import com.example.interviewfitbe.domain.coverletter.dto.analysis.CoverLetterAnalysisResponse;
import com.example.interviewfitbe.domain.coverletter.exception.CoverLetterErrorCode;
import com.example.interviewfitbe.domain.coverletter.service.CoverLetterService;
import com.example.interviewfitbe.domain.coverletter.service.pdf.PdfService;
import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.jwt.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/api/coverletter")
@RequiredArgsConstructor
@RestController
public class CoverLetterController {
    private final CoverLetterService coverLetterService;
    private final PdfService pdfService;

    @PostMapping("/analysis")
    public ResponseEntity<CoverLetterAnalysisResponse> analyzeCoverLetter(@RequestPart(value = "coverLetterFile", required = false) MultipartFile coverLetterFile) {
        if (coverLetterFile.isEmpty()) {
            throw new BaseException(CoverLetterErrorCode.COVER_LETTER_FILE_EMPTY);
        }
        Long userId = AuthUtils.getCurrentUserId();
        String coverLetter = pdfService.extractText(coverLetterFile);
        CoverLetterAnalysisResponse response = coverLetterService.analyzeCoverLetter(userId, coverLetter);
        return ResponseEntity.ok(response);
    }
}
