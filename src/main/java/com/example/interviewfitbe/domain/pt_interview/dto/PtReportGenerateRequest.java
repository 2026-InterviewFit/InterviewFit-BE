package com.example.interviewfitbe.domain.pt_interview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * FastAPI PT 종합 리포트 생성 요청
 */
public record PtReportGenerateRequest(

        /**
         * 업로드한 PPT/PDF 자체의 분석 요약
         */
        @JsonProperty("presentation_material_summary")
        String presentationMaterialSummary,

        /**
         * 사용자가 실제 발표 영상에서 말한 내용 STT
         */
        @JsonProperty("presentation_transcript")
        String presentationTranscript,

        /**
         * 질문 + 사용자의 실제 답변
         */
        @JsonProperty("qa_items")
        List<PtQuestionAnswerRequest> qaItems,

        /**
         * 시선 분석 점수
         */
        @JsonProperty("eye_scores")
        List<Double> eyeScores,

        /**
         * 눈 깜빡임 분석 점수
         */
        @JsonProperty("blink_scores")
        List<Double> blinkScores,

        /**
         * 자세 분석 점수
         */
        @JsonProperty("pose_scores")
        List<Double> poseScores,

        /**
         * 음성 분석 점수
         */
        @JsonProperty("speech_scores")
        List<Double> speechScores

) {
}