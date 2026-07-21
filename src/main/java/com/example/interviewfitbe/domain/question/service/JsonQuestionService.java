package com.example.interviewfitbe.domain.question.service;

import com.example.interviewfitbe.domain.job.exception.JobRoleErrorCode;
import com.example.interviewfitbe.domain.question.dto.client.QuestionClientResponse;
import com.example.interviewfitbe.domain.question.exception.JsonQuestionErrorCode;
import com.example.interviewfitbe.global.exception.BaseException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class JsonQuestionService {
    private final ObjectMapper objectMapper;
    private Map<String, List<String>> questionMap;

    @PostConstruct
    public void init() {
        try (InputStream inputStream = new ClassPathResource("interview/questions.json").getInputStream()) {
            questionMap = objectMapper.readValue(
                    inputStream,
                    new TypeReference<>() {
                    }
            );
        } catch (IOException e) {
            throw new BaseException(JsonQuestionErrorCode.QUESTION_DATA_LOAD_FAILED);
        }
    }

    public QuestionClientResponse getRandomQuestions(String jobType, int count) {
        List<String> questions = questionMap.get(jobType);

        if (questions == null || questions.isEmpty()) {
            throw new BaseException(JobRoleErrorCode.JOB_ROLE_NOT_FOUND);
        }

        List<String> shuffled = new ArrayList<>(questions);
        Collections.shuffle(shuffled);

        return QuestionClientResponse.builder()
                .questions(
                        shuffled.stream()
                                .limit(Math.min(count, shuffled.size()))
                                .toList()
                )
                .build();
    }
}
