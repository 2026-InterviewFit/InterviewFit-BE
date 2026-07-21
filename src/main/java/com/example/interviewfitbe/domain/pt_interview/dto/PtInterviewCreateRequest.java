package com.example.interviewfitbe.domain.pt_interview.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PtInterviewCreateRequest {

    private Long interviewId;

    private String topic;

    private String instruction;

    private Integer preparationSeconds;

    private Integer presentationSeconds;
}
