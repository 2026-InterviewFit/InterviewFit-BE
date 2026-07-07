package com.example.interviewfitbe.domain.coverletter.service;

import com.example.interviewfitbe.domain.coverletter.exception.PdfErrorCode;
import com.example.interviewfitbe.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequiredArgsConstructor
@Service
public class PdfService {
    private final OcrService ocrService;

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(PdfErrorCode.PDF_EMPTY);
        }
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            String text = new PDFTextStripper().getText(document).trim();
            // PDF에서 추출 성공
            if (!text.isBlank()) {
                return text;
            }
        } catch (IOException e) {
            // PDF 읽기 실패 시 OCR 시도
        }

        try {
            // PDF 텍스트가 없거나 PDF 읽기를 실패하면 OCR 시도
            return ocrService.extractText(file);
        } catch (BaseException e) {
            // OCR도 실패하면 null 반환
            return null;
        }
    }
}
