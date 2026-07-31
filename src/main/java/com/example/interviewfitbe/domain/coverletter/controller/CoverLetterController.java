package com.example.interviewfitbe.domain.coverletter.controller;

import com.example.interviewfitbe.domain.coverletter.dto.analysis.CoverLetterAnalysisRequest;
import com.example.interviewfitbe.domain.coverletter.dto.analysis.CoverLetterAnalysisResponse;
import com.example.interviewfitbe.domain.coverletter.dto.CoverLetterDto;
import com.example.interviewfitbe.domain.coverletter.mapper.CoverLetterMapper;
import com.example.interviewfitbe.domain.coverletter.service.CoverLetterService;
import com.example.interviewfitbe.domain.coverletter.service.pdf.PdfService;
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
    private final CoverLetterMapper coverLetterMapper;

    @GetMapping("/{id}")
    public ResponseEntity<CoverLetterDto> getCoverLetter(@PathVariable Long id) {
        CoverLetterDto result = coverLetterMapper.toDto(
                coverLetterService.getCoverLetter(id)
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/analysis")
    public ResponseEntity<CoverLetterAnalysisResponse> analyzeCoverLetter(
            @RequestPart("request") CoverLetterAnalysisRequest request,
            @RequestPart(value = "coverLetterFile", required = false) MultipartFile coverLetterFile
//            Authentication authentication
    ) {
        if (coverLetterFile != null && !coverLetterFile.isEmpty()) {
            String coverLetter = pdfService.extractText(coverLetterFile);
            request.updateCoverLetter(coverLetter);
        }

//        Long userId = (Long) authentication.getPrincipal();

        CoverLetterAnalysisResponse response = coverLetterService.analyzeCoverLetter(request.getUserId(), request);
        return ResponseEntity.ok(response);
    }
}
