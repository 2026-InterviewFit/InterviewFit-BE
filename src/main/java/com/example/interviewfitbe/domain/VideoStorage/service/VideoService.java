package com.example.interviewfitbe.domain.VideoStorage.service;

import com.example.interviewfitbe.domain.VideoStorage.dto.VideoUrlRequest;
import com.example.interviewfitbe.domain.VideoStorage.dto.VideoUrlResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class VideoService {

    public VideoUrlResponse saveVideoUrl(VideoUrlRequest request) {

        String fastApiUrl = "http://127.0.0.1:8000/analyze";

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> fastApiResponse =
                restTemplate.postForEntity(
                        fastApiUrl,
                        request,
                        String.class
                );

        System.out.println("FastAPI 응답: " + fastApiResponse.getBody());

        return new VideoUrlResponse(
                "Video URL received and sent to FastAPI",
                request.getVideoUrl()
        );
    }
}

