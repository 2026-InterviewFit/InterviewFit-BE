package com.example.interviewfitbe.domain.group.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class GroupInterviewDashboardRepositoryTest {

    @Autowired
    GroupInterviewRepository interviewRepository;

    @Autowired
    GroupInterviewAnswerRepository answerRepository;

    @Test
    void dashboardNativeQueriesExecuteAgainstCurrentSchema() {
        assertThat(interviewRepository.findCompletedHistoryByUserId(-1L)).isEmpty();
        assertThat(answerRepository.findMyAnswersWithAnalysis(-1L, -1L)).isEmpty();
    }
}
