package app.powerzone.progress;

import app.powerzone.progress.dto.ProgressRequest;
import app.powerzone.progress.dto.ProgressResponse;
import app.powerzone.progress.dto.ProgressSummaryResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @PostMapping
    public ResponseEntity<ProgressResponse> register(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProgressRequest request) {
        ProgressResponse response = progressService.register(subjectId(jwt), request);
        return ResponseEntity.created(URI.create("/api/v1/progress/" + response.id()))
                .body(response);
    }

    @GetMapping
    public List<ProgressResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long exerciseId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return progressService.list(subjectId(jwt), exerciseId, from, to);
    }

    @GetMapping("/summary")
    public List<ProgressSummaryResponse> summary(@AuthenticationPrincipal Jwt jwt) {
        return progressService.summary(subjectId(jwt));
    }

    static long subjectId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
