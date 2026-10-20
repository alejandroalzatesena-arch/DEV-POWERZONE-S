package app.powerzone.auth;

import app.powerzone.users.UserAccount;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final String issuer;
    private final long accessSeconds;

    public JwtService(JwtEncoder encoder,
                      @Value("${powerzone.jwt.issuer:powerzone-api}") String issuer,
                      @Value("${powerzone.jwt.access-seconds:900}") long accessSeconds) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.accessSeconds = accessSeconds;
    }

    public IssuedToken issue(UserAccount user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessSeconds, ChronoUnit.SECONDS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(Long.toString(user.id()))
                .claim("email", user.email())
                .claim("name", user.firstName() + " " + user.lastName())
                .claim("gymId", user.gymId())
                .claim("roles", List.of(user.role()))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, accessSeconds);
    }

    public record IssuedToken(String value, long expiresInSeconds) {
    }
}
