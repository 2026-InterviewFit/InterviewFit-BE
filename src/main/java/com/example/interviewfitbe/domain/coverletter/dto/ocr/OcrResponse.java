package com.example.interviewfitbe.domain.coverletter.dto.ocr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OcrResponse {
    private String text;
}
