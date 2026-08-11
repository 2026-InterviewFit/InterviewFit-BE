package com.example.interviewfitbe.domain.livekit.service;

import io.livekit.server.AccessToken;
import io.livekit.server.CanPublish;
import io.livekit.server.CanPublishData;
import io.livekit.server.CanSubscribe;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class LiveKitService {


    @Value("${livekit.api-key}")
    private String apiKey;


    @Value("${livekit.api-secret}")
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
}