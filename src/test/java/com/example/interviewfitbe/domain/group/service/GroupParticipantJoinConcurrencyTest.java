package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.repository.GroupParticipantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GroupParticipantJoinConcurrencyTest {

    private static final Long TEST_ROOM_ID = -9_999_991L;
    private static final Long TEST_USER_ID = -9_999_992L;

    @Autowired
    GroupParticipantJoinWriter joinWriter;

    @Autowired
    GroupParticipantRepository participantRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void concurrentInsertCreatesOnlyOneParticipantRow() throws Exception {
        deleteTestParticipant();
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            var first = executor.submit(() -> insertAtSameTime(ready, start));
            var second = executor.submit(() -> insertAtSameTime(ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int successes = first.get(10, TimeUnit.SECONDS) + second.get(10, TimeUnit.SECONDS);
            assertThat(successes).isEqualTo(1);
            assertThat(participantRepository.countByRoomIdAndUserId(TEST_ROOM_ID, TEST_USER_ID))
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();
            deleteTestParticipant();
        }
    }

    private int insertAtSameTime(CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await(5, TimeUnit.SECONDS);
            joinWriter.insert(TEST_ROOM_ID, TEST_USER_ID);
            return 1;
        } catch (DataIntegrityViolationException exception) {
            return 0;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private void deleteTestParticipant() {
        new TransactionTemplate(transactionManager).executeWithoutResult(
                status -> participantRepository.deleteByRoomIdAndUserId(TEST_ROOM_ID, TEST_USER_ID)
        );
    }
}
