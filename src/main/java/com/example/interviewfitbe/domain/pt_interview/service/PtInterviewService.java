package com.example.interviewfitbe.domain.pt_interview.service;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import com.example.interviewfitbe.domain.pt_interview.dto.PtInterviewCreateRequest;
import com.example.interviewfitbe.domain.pt_interview.dto.PtInterviewResponse;
import com.example.interviewfitbe.domain.pt_interview.entity.PtInterviewEntity;
import com.example.interviewfitbe.domain.pt_interview.repository.PtInterviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PtInterviewService {

    private final PtInterviewRepository ptInterviewRepository;

    private final InterviewRepository interviewRepository;

    @Transactional
    public PtInterviewResponse create(
            PtInterviewCreateRequest request
    ) {

        InterviewEntity interview = interviewRepository
                .findById(request.getInterviewId())
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "존재하지 않는 면접입니다."
                        )
                );

        if (
                ptInterviewRepository
                        .findByInterviewId(interview.getId())
                        .isPresent()
        ) {
            throw new IllegalStateException(
                    "이미 PT 면접 정보가 존재합니다."
            );
        }

        PtInterviewEntity ptInterview =
                PtInterviewEntity.create(
                        interview,
                        request.getTopic(),
                        request.getInstruction(),
                        request.getPreparationSeconds(),
                        request.getPresentationSeconds()
                );

        PtInterviewEntity saved =
                ptInterviewRepository.save(ptInterview);

        return PtInterviewResponse.from(saved);
    }
}
