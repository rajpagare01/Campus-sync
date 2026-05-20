package com.campussync.backend.Controller;

import com.campussync.backend.Dto.PresenceSnapshotResponse;
import com.campussync.backend.Service.RealtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/realtime", "/api/v1/realtime"})
@RequiredArgsConstructor
public class RealtimeController {

    private final RealtimeService realtimeService;

    @GetMapping("/presence")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PresenceSnapshotResponse> getPresence() {
        return ResponseEntity.ok(realtimeService.getPresenceSnapshot());
    }
}
