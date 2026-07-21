package com.example.interviewfitbe.domain.coverletter.service.ocr;

import com.example.interviewfitbe.domain.coverletter.dto.ocr.OcrResponse;
import com.example.interviewfitbe.domain.coverletter.exception.PdfErrorCode;
import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.exception.code.FastApiErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;

@RequiredArgsConstructor
@Service
public class OcrService {
    private final WebClient fastApiWebClient;

    public String extractText(MultipartFile file) {
        return extractPdfText(file).getText();
    }

    public OcrResponse extractPdfText(MultipartFile file) {
        try {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            return fastApiWebClient.post()
                    .uri("/apis/ocr/extract")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData("file", resource))
                    .retrieve()
                    .bodyToMono(OcrResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString();

            if (body.contains("PDF_IMAGE_EXTRACTION_FAILED")) {
                throw new BaseException(PdfErrorCode.PDF_IMAGE_EXTRACTION_FAILED);
            }

            if (body.contains("OCR_FAILED")) {
                throw new BaseException(PdfErrorCode.OCR_FAILED);
            }

            if (body.contains("OCR_SERVER_ERROR")) {
                throw new BaseException(PdfErrorCode.OCR_SERVER_ERROR);
            }

            throw new BaseException(FastApiErrorCode.FASTAPI_SERVER_ERROR);
        } catch (IOException e) {
            throw new BaseException(PdfErrorCode.PDF_READ_FAILED);
        }
    }
}
