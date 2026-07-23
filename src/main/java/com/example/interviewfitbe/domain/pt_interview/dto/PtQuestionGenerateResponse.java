package com.example.interviewfitbe.domain.pt_interview.dto;

import java.util.List;

public record PtQuestionGenerateResponse(

        List<PtQuestionResult> questions

) {
}
