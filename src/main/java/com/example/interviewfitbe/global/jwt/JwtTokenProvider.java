package com.example.interviewfitbe.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";

    private static final String ACCESS_TOKEN_TYPE = "access";

    private static final String REFRESH_TOKEN_TYPE = "refresh";


    private final SecretKey key;

    private final long accessTokenValidityInMs;

    private final long refreshTokenValidityInMs;


    public JwtTokenProvider(
            @Value("${jwt.secret}")
            String secret,

            @Value("${jwt.access-expiration-ms}")
            long accessTokenValidityInMs,

            @Value("${jwt.refresh-expiration-ms}")
            long refreshTokenValidityInMs
    ) {
        this.key =
                Keys.hmacShaKeyFor(
                        secret.getBytes()
                );

        this.accessTokenValidityInMs =
                accessTokenValidityInMs;

        this.refreshTokenValidityInMs =
                refreshTokenValidityInMs;
    }


    /**
     * Access Token 생성
     *
     * 일반 API 인증에 사용한다.
     *
     * 기본 설정:
     * 30분
     */
    public String createAccessToken(
            Long userId
    ) {
        return createTokenInternal(
                userId,
                ACCESS_TOKEN_TYPE,
                accessTokenValidityInMs
        );
    }


    /**
     * Refresh Token 생성
     *
     * Access Token이 만료됐을 때
     * 새로운 Access Token을 발급받기 위해 사용한다.
     *
     * 기본 설정:
     * 14일
     */
    public String createRefreshToken(
            Long userId
    ) {
        return createTokenInternal(
                userId,
                REFRESH_TOKEN_TYPE,
                refreshTokenValidityInMs
        );
    }


    /**
     * JWT 공통 생성 로직
     */
    private String createTokenInternal(
            Long userId,
            String tokenType,
            long validityInMs
    ) {
        if (userId == null) {
            throw new IllegalArgumentException(
                    "JWT 생성에 필요한 userId가 없습니다."
            );
        }

        Date now =
                new Date();

        Date expiry =
                new Date(
                        now.getTime()
                                + validityInMs
                );


        return Jwts.builder()

                /*
                 * JWT subject에
                 * InterviewFit User ID 저장
                 */
                .subject(
                        String.valueOf(
                                userId
                        )
                )

                /*
                 * Access/Refresh Token을
                 * 확실하게 구분하기 위한 claim.
                 *
                 * Refresh Token을
                 * 일반 API 인증에 사용하는 것을 막는다.
                 */
                .claim(
                        TOKEN_TYPE_CLAIM,
                        tokenType
                )

                .issuedAt(
                        now
                )

                .expiration(
                        expiry
                )

                .signWith(
                        key
                )

                .compact();
    }


    /**
     * JWT에서 userId 추출
     */
    public Long getUserId(
            String token
    ) {
        Claims claims =
                parseClaims(
                        token
                );

        return Long.valueOf(
                claims.getSubject()
        );
    }


    /**
     * Access Token 검증
     */
    public boolean validateAccessToken(
            String token
    ) {
        return validateTokenType(
                token,
                ACCESS_TOKEN_TYPE
        );
    }


    /**
     * Refresh Token 검증
     */
    public boolean validateRefreshToken(
            String token
    ) {
        return validateTokenType(
                token,
                REFRESH_TOKEN_TYPE
        );
    }


    /**
     * JWT가 유효한지 + 원하는 종류의 토큰인지 검사
     */
    private boolean validateTokenType(
            String token,
            String expectedTokenType
    ) {
        if (
                token == null
                        || token.isBlank()
        ) {
            return false;
        }

        try {
            Claims claims =
                    parseClaims(
                            token
                    );

            String actualTokenType =
                    claims.get(
                            TOKEN_TYPE_CLAIM,
                            String.class
                    );


            return expectedTokenType.equals(
                    actualTokenType
            );

        } catch (Exception e) {
            /*
             * 다음 경우 모두 false:
             *
             * - JWT 만료
             * - 서명 불일치
             * - 잘못된 JWT
             * - tokenType 없음
             */
            return false;
        }
    }


    /**
     * JWT Claims 파싱
     */
    private Claims parseClaims(
            String token
    ) {
        return Jwts.parser()

                .verifyWith(
                        key
                )

                .build()

                .parseSignedClaims(
                        token
                )

                .getPayload();
    }


    /**
     * =====================================================
     * 기존 코드 호환용
     * =====================================================
     *
     * 아직 UserController와 JwtAuthenticationFilter가
     * 기존 createToken(), validateToken()을 사용하고 있다.
     *
     * 다음 단계에서 해당 파일들을 수정하기 전까지
     * 프로젝트가 깨지지 않도록 남겨둔다.
     */


    /**
     * 기존 createToken()
     *
     * 이제 Access Token 생성으로 연결한다.
     */
    public String createToken(
            Long userId
    ) {
        return createAccessToken(
                userId
        );
    }


    /**
     * 기존 validateToken()
     *
     * Access Token만 정상 인증 토큰으로 인정한다.
     */
    public boolean validateToken(
            String token
    ) {
        return validateAccessToken(
                token
        );
    }


    /**
     * Access Token 만료시간(ms)
     */
    public long getAccessTokenValidityInMs() {
        return accessTokenValidityInMs;
    }


    /**
     * Refresh Token 만료시간(ms)
     */
    public long getRefreshTokenValidityInMs() {
        return refreshTokenValidityInMs;
    }
}