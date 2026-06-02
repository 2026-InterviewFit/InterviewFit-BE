package com.example.interviewfitbe.domain.VideoStorage.service;

import com.example.interviewfitbe.domain.VideoStorage.dto.VideoUrlRequest;
import com.example.interviewfitbe.domain.VideoStorage.dto.VideoUrlResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class VideoService {

    private final String uploadDir = System.getProperty("user.dir")
            + "/uploads/videos/";

    private final String serverBaseUrl = "http://localhost:8080";

    public VideoUrlResponse saveVideoFile(MultipartFile file) throws IOException {

        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalFileName = file.getOriginalFilename();
        String savedFileName = UUID.randomUUID() + "_" + originalFileName;

        String savedPath = uploadDir + savedFileName;

        File savedFile = new File(savedPath);
        file.transferTo(savedFile);

        String videoUrl = serverBaseUrl + "/videos/" + savedFileName;

        VideoUrlRequest request = new VideoUrlRequest();
        request.setVideoUrl(videoUrl);

        try {
            sendToFastApi(request);
        } catch (Exception e) {
            System.out.println("FastAPI 전송 실패: " + e.getMessage());
        }

        return new VideoUrlResponse(
                "Video file saved",
                videoUrl
        );
    }

    private void sendToFastApi(VideoUrlRequest request) {

        String fastApiUrl = "http://127.0.0.1:8000/VideoStorage/save";

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        fastApiUrl,
                        request,
                        String.class
                );

        System.out.println("FastAPI 응답: " + response.getBody());
    }
}