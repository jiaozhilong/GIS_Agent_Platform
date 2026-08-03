package com.jiaozhilong.gisagent.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    public JwtService(JwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
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
        mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(byte[] value) { return ENCODER.encodeToString(value); }
}
