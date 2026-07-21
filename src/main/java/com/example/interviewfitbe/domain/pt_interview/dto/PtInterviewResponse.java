package com.example.interviewfitbe.domain.pt_interview.dto;

import com.example.interviewfitbe.domain.pt_interview.entity.PtInterviewEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PtInterviewResponse {

    private Long id;

    private Long interviewId;

    private String topic;

    private String instruction;

    private Integer preparationSeconds;

    private Integer presentationSeconds;

    private String status;

    public static PtInterviewResponse from(
            PtInterviewEntity entity
    ) {
        return PtInterviewResponse.builder()
                .id(entity.getId())
                .interviewId(entity.getInterview().getId())
                .topic(entity.getTopic())
                .instruction(entity.getInstruction())
                .preparationSeconds(entity.getPreparationSeconds())
                .presentationSeconds(entity.getPresentationSeconds())
                .status(entity.getStatus().name())
                .build();
    }
}
