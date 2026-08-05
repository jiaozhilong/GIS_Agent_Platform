package com.jiaozhilong.gisagent.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private final JwtProperties properties;
    private final ObjectMapper objectMapper;
    private final byte[] signingKey;

    public JwtService(JwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        if (properties.secret() == null || properties.secret().isBlank()) {
            this.signingKey = new byte[32];
            new SecureRandom().nextBytes(this.signingKey);
            log.warn("GIS_AGENT_JWT_SECRET is not configured. Using an in-memory JWT key for local development.");
        } else {
            this.signingKey = properties.secret().getBytes(StandardCharsets.UTF_8);
        }
    }

    public String create(PlatformUserPrincipal principal) {
        try {
            long issuedAt = Instant.now().getEpochSecond();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", principal.getUsername());
            payload.put("uid", principal.id().toString());
            payload.put("iat", issuedAt);
            payload.put("exp", issuedAt + properties.expirationSeconds());
            payload.put("authorities", principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
            String header = encode(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String body = encode(objectMapper.writeValueAsBytes(payload));
            String unsigned = header + "." + body;
            return unsigned + "." + encode(sign(unsigned));
        } catch (Exception exception) {
            throw new IllegalStateException("无法创建访问令牌", exception);
        }
    }

    public String subject(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String unsigned = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(unsigned), DECODER.decode(parts[2]))) return null;
            Map<String, Object> payload = objectMapper.readValue(DECODER.decode(parts[1]), new TypeReference<>() {});
            Number expiration = (Number) payload.get("exp");
            if (expiration == null || expiration.longValue() <= Instant.now().getEpochSecond()) return null;
            return String.valueOf(payload.get("sub"));
        } catch (Exception ignored) {
            return null;
        }
    }

    public long expirationSeconds() { return properties.expirationSeconds(); }

    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(byte[] value) { return ENCODER.encodeToString(value); }
}
