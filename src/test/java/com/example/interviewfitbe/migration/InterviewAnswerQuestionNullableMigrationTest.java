package com.example.interviewfitbe.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InterviewAnswerQuestionNullableMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void interviewAnswerQuestionIdIsNullableAfterMigration() {
        assertQuestionIdIsNullable();
    }

    @Test
    void droppingNotNullAgainIsSafeWhenColumnIsAlreadyNullable() {
        jdbcTemplate.execute("""
                ALTER TABLE interview_answers
                    ALTER COLUMN question_id DROP NOT NULL
                """);

        assertQuestionIdIsNullable();
    }

    private void assertQuestionIdIsNullable() {
        String nullable = jdbcTemplate.queryForObject(
                """
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'interview_answers'
                  AND column_name = 'question_id'
                """,
                String.class
        );

        assertThat(nullable).isEqualTo("YES");
    }
}
