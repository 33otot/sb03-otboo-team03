package com.samsamotot.otboo.common.security.jwt;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.samsamotot.otboo.common.exception.ErrorCode;
import com.samsamotot.otboo.common.exception.OtbooException;

import lombok.extern.slf4j.Slf4j;

/**
 * JWT(JSON Web Token) 토큰 생성 및 검증을 담당하는 핵심 클래스
 * 
 * <p>이 클래스는 JWT 표준을 준수하여 액세스 토큰과 리프레시 토큰을 생성하고,
 * 토큰의 유효성을 검증하는 기능을 제공합니다.</p>
 * 
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li><strong>토큰 생성</strong>: 액세스 토큰(1시간), 리프레시 토큰(7일) 생성</li>
 *   <li><strong>토큰 검증</strong>: 서명, 만료시간, 발급자, 사용자 ID 검증</li>
 *   <li><strong>토큰 파싱</strong>: 토큰에서 사용자 ID, 만료시간, 토큰 타입 추출</li>
 *   <li><strong>HMAC-SHA256 서명</strong>: 안전한 토큰 서명 및 검증</li>
 * </ul>
 * 
 * <h3>JWT 표준 준수:</h3>
 * <ul>
 *   <li><strong>iss (issuer)</strong>: 토큰 발급자</li>
 *   <li><strong>sub (subject)</strong>: 토큰 주체 (사용자 ID)</li>
 *   <li><strong>iat (issued at)</strong>: 토큰 발급 시간</li>
 *   <li><strong>exp (expiration)</strong>: 토큰 만료 시간</li>
 *   <li><strong>jti (JWT ID)</strong>: 토큰 고유 식별자</li>
 * </ul>
 * 
 * <h3>커스텀 클레임:</h3>
 * <ul>
 *   <li><strong>userId</strong>: 사용자 고유 식별자</li>
 *   <li><strong>tokenType</strong>: 토큰 타입 (access, refresh)</li>
 * </ul>
 * 
 * <h3>보안 특징:</h3>
 * <ul>
 *   <li><strong>HMAC-SHA256</strong>: 안전한 서명 알고리즘 사용</li>
 *   <li><strong>만료 시간 검증</strong>: 만료된 토큰 자동 거부</li>
 *   <li><strong>발급자 검증</strong>: 신뢰할 수 있는 발급자만 허용</li>
 *   <li><strong>예외 처리</strong>: 토큰 파싱 실패 시 적절한 예외 발생</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtTokenProvider {
    
    private static final String USER_ID_CLAIM = "userId";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    
    private final String secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final String issuer;
    
    public JwtTokenProvider(
            @Value("${otboo.jwt.secret}") String secretKey,
            @Value("${otboo.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${otboo.jwt.refresh-token-expiration}") long refreshTokenExpiration,
            @Value("${otboo.jwt.issuer}") String issuer) {
        this.secretKey = secretKey;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.issuer = issuer;
    }
    
    /**
     * 액세스 토큰을 생성합니다.
     *
     * @param userId 사용자 ID
     * @return 생성된 액세스 토큰
     */
    public String createAccessToken(UUID userId) {
        return createToken(userId, ACCESS_TOKEN_TYPE, accessTokenExpiration);
    }
    
    /**
     * 리프레시 토큰을 생성합니다.
     *
     * @param userId 사용자 ID
     * @return 생성된 리프레시 토큰
     */
    public String createRefreshToken(UUID userId) {
        return createToken(userId, REFRESH_TOKEN_TYPE, refreshTokenExpiration);
    }
    
    /**
     * JWT 토큰을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param tokenType 토큰 타입 (access, refresh)
     * @param expiration 만료 시간 (밀리초)
     * @return 생성된 JWT 토큰
     */
    private String createToken(UUID userId, String tokenType, long expiration) {
        try {
            Instant now = Instant.now();
            Instant expirationTime = now.plusMillis(expiration);
            
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(userId.toString())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expirationTime))
                    .claim(USER_ID_CLAIM, userId.toString())
                    .claim(TOKEN_TYPE_CLAIM, tokenType)
                    .jwtID(UUID.randomUUID().toString())
                    .build();
            
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
            SignedJWT signedJWT = new SignedJWT(header, claims);
            
            JWSSigner signer = new MACSigner(secretKey);
            signedJWT.sign(signer);
            
            log.debug("JWT 토큰 생성 완료 - 사용자 ID: {}, 토큰 타입: {}", userId, tokenType);
            return signedJWT.serialize();
            
        } catch (JOSEException e) {
            log.error("JWT 토큰 생성 실패 - 사용자 ID: {}, 토큰 타입: {}", userId, tokenType, e);
            throw new OtbooException(ErrorCode.INTERNAL_SERVER_ERROR, "JWT 토큰 생성에 실패했습니다.");
        }
    }
    
    /**
     * JWT 토큰을 검증하고 사용자 ID를 추출합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 ID
     * @throws Exception 
     */
    public UUID getUserIdFromToken(String token) throws Exception {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            
            // 토큰 서명 검증
            JWSVerifier verifier = new MACVerifier(secretKey);
            if (!signedJWT.verify(verifier)) {
                throw new OtbooException(ErrorCode.TOKEN_INVALID);
            }
            
            // 토큰 만료 시간 검증
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            if (claims.getExpirationTime().before(new Date())) {
                throw new OtbooException(ErrorCode.TOKEN_EXPIRED);
            }
            
            // 발급자 검증
            if (!issuer.equals(claims.getIssuer())) {
                throw new OtbooException(ErrorCode.TOKEN_INVALID);
            }
            
            String userIdStr = claims.getStringClaim(USER_ID_CLAIM);
            if (userIdStr == null) {
                throw new OtbooException(ErrorCode.TOKEN_INVALID);
            }
            
            return UUID.fromString(userIdStr);
            
        } catch (JOSEException e) {
            log.error("JWT 토큰 검증 실패 - 토큰: {}", token, e);
            throw new OtbooException(ErrorCode.TOKEN_INVALID);
        } catch (Exception e) {
            if (e instanceof OtbooException) {
                throw e;
            }
            log.error("JWT 토큰 검증 실패 - 토큰: {}", token, e);
            throw new OtbooException(ErrorCode.TOKEN_INVALID);
        }
    }
    
    /**
     * 토큰이 유효한지 검증합니다.
     *
     * @param token JWT 토큰
     * @return 토큰 유효성 여부
     */
    public boolean validateToken(String token) {
        try {
            getUserIdFromToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 토큰의 만료 시간을 반환합니다.
     *
     * @param token JWT 토큰
     * @return 만료 시간 (초)
     */
    public Long getExpirationTime(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            return claims.getExpirationTime().getTime() / 1000;
        } catch (Exception e) {
            log.error("토큰 만료 시간 조회 실패 - 토큰: {}", token, e);
            return null;
        }
    }

    /**
     * 토큰 발급 시간(iat)을 epoch seconds로 반환합니다.
     */
    public Long getIssuedAtEpochSeconds(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date issuedAt = claims.getIssueTime();
            return issuedAt != null ? issuedAt.getTime() / 1000 : null;
        } catch (Exception e) {
            log.error("토큰 발급 시간 조회 실패 - 토큰: {}", token, e);
            return null;
        }
    }

    /**
     * 토큰 고유 ID(jti)를 반환합니다.
     */
    public String getJti(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getJWTID();
        } catch (Exception e) {
            log.error("토큰 JTI 조회 실패 - 토큰: {}", token, e);
            return null;
        }
    }
    
    /**
     * 토큰 타입을 반환합니다.
     *
     * @param token JWT 토큰
     * @return 토큰 타입 (access, refresh)
     */
    public String getTokenType(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            return claims.getStringClaim(TOKEN_TYPE_CLAIM);
        } catch (Exception e) {
            log.error("토큰 타입 조회 실패 - 토큰: {}", token, e);
            return null;
        }
    }

    /**
     * 인터셉터/필터에서 쓰기 좋은 얕은 검증. 유효하면 true, 아니면 false를 반환한다.
     */
    public boolean validate(String token) {
        if (token == null || token.isBlank()) return false;
        try {
            SignedJWT jwt = SignedJWT.parse(token);

            // 서명 검증
            JWSVerifier verifier = new MACVerifier(secretKey);
            if (!jwt.verify(verifier)) return false;

            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            // 만료/발급자 검증
            Date exp = claims.getExpirationTime();
            if (exp == null || exp.before(new Date())) return false;
            if (!issuer.equals(claims.getIssuer())) return false;

            // 필수 클레임 검증 (userId 존재 여부)
            String userIdStr = claims.getStringClaim(USER_ID_CLAIM);
            if (userIdStr == null) return false;

            // 파싱 가능해야 함
            UUID.fromString(userIdStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 유효한 토큰에서 사용자 ID를 반환한다.
     * 유효하지 않으면 OtbooException(TOKEN_INVALID/TOKEN_EXPIRED)을 던진다.
     */
    public UUID getUserId(String token) {
        try {
            return getUserIdFromToken(token); // 이미 서명/만료/issuer 검증 포함
        } catch (OtbooException e) {
            // getUserIdFromToken 이 내부에서 적절한 코드로 던지므로 그대로 전파
            throw e;
        } catch (Exception e) {
            throw new OtbooException(ErrorCode.TOKEN_INVALID);
        }
    }
}
