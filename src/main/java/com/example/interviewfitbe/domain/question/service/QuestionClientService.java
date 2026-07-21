package com.example.interviewfitbe.domain.question.service;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionAnalysisEntity;
import com.example.interviewfitbe.domain.coverletter.mapper.CoverLetterQuestionAnalysisMapper;
import com.example.interviewfitbe.domain.question.dto.client.CoverLetterQuestionAnalysis2;
import com.example.interviewfitbe.domain.question.dto.client.QuestionClientRequest;
import com.example.interviewfitbe.domain.question.dto.client.QuestionClientResponse;
import com.example.interviewfitbe.domain.question.exception.QuestionErrorCode;
import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.exception.code.FastApiErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Service
public class QuestionClientService {
    private final WebClient fastApiWebClient;
    private final CoverLetterQuestionAnalysisMapper coverLetterQuestionAnalysisMapper;

    public QuestionClientResponse createQuestions(int count, String jobName, List<CoverLetterQuestionAnalysisEntity> coverLetterQuestionAnalysis, List<String> previousQuestions) {
        List<CoverLetterQuestionAnalysis2> analyses = coverLetterQuestionAnalysis.stream()
                .map(coverLetterQuestionAnalysisMapper::toClient)
                .sorted(Comparator.comparing(
                        CoverLetterQuestionAnalysis2::getImportanceScore,
                                Comparator.nullsLast(Float::compareTo)
                        ).reversed()
                )
                .toList();

        QuestionClientRequest request = QuestionClientRequest.builder()
                .count(count)
                .jobName(jobName)
                .analyses(analyses)
                .previousQuestions(previousQuestions)
                .build();

        try {
            return fastApiWebClient.post()
                    .uri("/apis/question/one-to-one/create")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(QuestionClientResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString();

            if (body.contains("QUESTION_GENERATION_FAILED")) {
                throw new BaseException(QuestionErrorCode.QUESTION_GENERATION_FAILED);
            }
            if (body.contains("INVALID_QUESTION_RESPONSE")) {
                throw new BaseException(QuestionErrorCode.INVALID_QUESTION_RESPONSE);
            }
            if (body.contains("INVALID_QUESTION_COUNT")) {
                throw new BaseException(QuestionErrorCode.INVALID_QUESTION_COUNT);
            }
            if (body.contains("INVALID_QUESTION_VALIDATION")) {
                throw new BaseException(QuestionErrorCode.INVALID_QUESTION_VALIDATION);
            }
            throw new BaseException(FastApiErrorCode.FASTAPI_SERVER_ERROR);
        }
    }
}
