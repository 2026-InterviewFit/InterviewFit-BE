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

    private final String uploadDir = "uploads/videos/";

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

        VideoUrlRequest request = new VideoUrlRequest();
        request.setVideoUrl(savedPath);

        sendToFastApi(request);

        return new VideoUrlResponse(
                "Video file saved and sent to FastAPI",
                savedPath
        );
    }

    private void sendToFastApi(VideoUrlRequest request) {
        String fastApiUrl = "http://127.0.0.1:8000/analyze";

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
