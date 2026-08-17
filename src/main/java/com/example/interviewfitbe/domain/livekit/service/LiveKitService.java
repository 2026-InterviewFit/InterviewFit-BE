package com.example.interviewfitbe.domain.livekit.service;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewRepository;
import com.example.interviewfitbe.domain.group.repository.GroupParticipantRepository;
import com.example.interviewfitbe.domain.livekit.dto.LiveKitTokenResponse;
import com.example.interviewfitbe.global.config.LiveKitConfig;
import io.livekit.server.AccessToken;
import io.livekit.server.CanPublish;
import io.livekit.server.CanPublishData;
import io.livekit.server.CanSubscribe;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class LiveKitService {

    private final GroupInterviewRepository groupInterviewRepository;

    private final GroupParticipantRepository groupParticipantRepository;

    private final LiveKitConfig liveKitConfig;


    @Value("${livekit.api-key:${LIVEKIT_API_KEY}}")
    private String apiKey;


    @Value("${livekit.api-secret:${LIVEKIT_API_SECRET}}")
    private String apiSecret;



    public String createToken(
            String roomName,
            Long userId
    ) {

        AccessToken token = new AccessToken(
                apiKey,
                apiSecret
        );


        token.setIdentity(
                String.valueOf(userId)
        );


        token.addGrants(
                new RoomJoin(true),
                new RoomName(roomName),
                new CanPublish(true),
                new CanSubscribe(true),
                new CanPublishData(true)
        );


        return token.toJwt();
    }

    /**
     * 다대일 면접 참가자의 LiveKit 입장 토큰을 발급한다.
     */
    @Transactional(readOnly = true)
    public LiveKitTokenResponse createGroupInterviewToken(
            Long roomId,
            Long userId
    ) {
        GroupInterviewEntity interview = groupInterviewRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 면접 방입니다. ID: " + roomId
                ));

        if (!groupParticipantRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new AccessDeniedException(
                    "해당 면접 방에 참여하지 않은 사용자입니다."
            );
        }

        String token = createToken(
                interview.getLivekitRoomName(),
                userId
        );

        return new LiveKitTokenResponse(
                liveKitConfig.getUrl(),
                token
        );
    }
}
