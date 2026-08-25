package com.example.interviewfitbe.domain.pt_interview.service;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.pt_interview.entity.PtInterviewEntity;
import com.example.interviewfitbe.domain.pt_interview.repository.PtInterviewRepository;
import com.example.interviewfitbe.domain.speech_coaching.dto.SpeechAnalyzeRequest;
import com.example.interviewfitbe.domain.speech_coaching.service.SpeechReportService;
import com.example.interviewfitbe.domain.video_storage.dto.VideoUrlResponse;
import com.example.interviewfitbe.domain.video_storage.entity.VideoType;
import com.example.interviewfitbe.domain.video_storage.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PtPresentationService {

    private final PtInterviewRepository ptInterviewRepository;
    private final VideoService videoService;
    private final SpeechReportService speechReportService;

    /**
     * PT 발표 전체 영상 업로드 및 Speech/STT 분석
     */
    public Long uploadAndAnalyze(
            Long ptInterviewId,
            MultipartFile videoFile
    ) throws IOException {

        PtInterviewEntity ptInterview =
                ptInterviewRepository
                        .findById(ptInterviewId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 PT 면접입니다. "
                                                + "ptInterviewId="
                                                + ptInterviewId
                                )
                        );

        InterviewEntity interview =
                ptInterview.getInterview();

        if (interview == null) {
            throw new IllegalStateException(
                    "PT 면접에 연결된 Interview가 없습니다. "
                            + "ptInterviewId="
                            + ptInterviewId
            );
        }

        /*
         * 1. 발표 전체 영상 MinIO 저장
         */
        VideoUrlResponse videoResponse =
                videoService.saveVideoFile(
                        interview.getId(),
                        videoFile,
                        VideoType.PT_PRESENTATION
                );

        String videoObjectKey =
                videoResponse.getVideoUrl();

        log.info(
                "PT 발표 영상 저장 완료: ptInterviewId={}, objectKey={}",
                ptInterviewId,
                videoObjectKey
        );

        /*
         * 2. 직무 카테고리
         */
        String jobCategory = "";

        if (
                interview.getJobRole() != null
                        && interview.getJobRole().getCategory() != null
        ) {
            jobCategory =
                    interview
                            .getJobRole()
                            .getCategory();
        }

        /*
         * 3. FastAPI Speech 분석
         *
         * 발표 전체 영상이므로
         * interviewAnswerId / questionId / originalQuestion은 null.
         */
        SpeechAnalyzeRequest request =
                SpeechAnalyzeRequest.builder()
                        .sourceType("minio")
                        .sourceValue(videoObjectKey)
                        .jobCategory(jobCategory)
                        .interviewId(interview.getId())
                        .interviewAnswerId(null)
                        .questionId(null)
                        .originalQuestion(null)
                        .build();

        Long speechReportId =
                speechReportService
                        .analyzeAndSave(request);

        log.info(
                "PT 발표 Speech 분석 완료: ptInterviewId={}, speechReportId={}",
                ptInterviewId,
                speechReportId
        );

        return speechReportId;
    }
}