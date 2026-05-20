package com.campussync.backend.Service;

import com.campussync.backend.Dto.PresenceSnapshotResponse;
import com.campussync.backend.Dto.PresenceUpdate;
import com.campussync.backend.Dto.PresenceUserDto;
import com.campussync.backend.Dto.RealtimeCommentUpdate;
import com.campussync.backend.Dto.RealtimeEventUpdate;
import com.campussync.backend.Dto.RealtimeFeedUpdate;
import com.campussync.backend.Dto.RealtimeLikeUpdate;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RealtimeService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    private final ConcurrentHashMap<String, String> sessionsById = new ConcurrentHashMap<>();
    private final Set<String> onlineEmails = ConcurrentHashMap.newKeySet();

    public void broadcastLikeUpdate(Long postId, User user, boolean liked, int likeCount) {
        RealtimeLikeUpdate update = new RealtimeLikeUpdate();
        update.setPostId(postId);
        update.setUserId(user.getId());
        update.setUserName(user.getName());
        update.setLiked(liked);
        update.setLikeCount(likeCount);
        update.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/posts/" + postId + "/likes", update);
    }

    public void broadcastCommentUpdate(RealtimeCommentUpdate update) {
        update.setTimestamp(update.getCreatedAt() != null ? update.getCreatedAt() : LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/posts/" + update.getPostId() + "/comments", update);
    }

    public void broadcastEventUpdate(Event event, String action, String message) {
        RealtimeEventUpdate update = new RealtimeEventUpdate();
        update.setEventId(event.getId());
        update.setTitle(event.getTitle());
        update.setAction(action);
        update.setMessage(message);
        update.setStatus(event.getStatus());
        update.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/events/" + event.getId() + "/updates", update);
    }

    public void broadcastFeedRefresh(String entityType, Long entityId, String action) {
        RealtimeFeedUpdate update = new RealtimeFeedUpdate();
        update.setEntityType(entityType);
        update.setEntityId(entityId);
        update.setAction(action);
        update.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/feed/updates", update);
    }

    public void handleSessionConnected(String sessionId, String email) {
        if (sessionId == null || email == null || email.isBlank()) {
            return;
        }

        sessionsById.put(sessionId, email);
        boolean becameOnline = onlineEmails.add(email);
        if (becameOnline) {
            broadcastPresence(email, true);
        }
    }

    public void handleSessionDisconnected(String sessionId) {
        if (sessionId == null) {
            return;
        }

        String email = sessionsById.remove(sessionId);
        if (email == null) {
            return;
        }

        boolean stillConnected = sessionsById.containsValue(email);
        if (!stillConnected && onlineEmails.remove(email)) {
            broadcastPresence(email, false);
        }
    }

    public PresenceSnapshotResponse getPresenceSnapshot() {
        PresenceSnapshotResponse response = new PresenceSnapshotResponse();
        List<PresenceUserDto> users = new ArrayList<>();
        for (String email : onlineEmails) {
            userRepository.findByEmail(email)
                    .ifPresent(user -> users.add(new PresenceUserDto(user.getId(), user.getEmail())));
        }
        response.setUsers(users);
        response.setOnlineCount(users.size());
        return response;
    }

    private void broadcastPresence(String email, boolean online) {
        PresenceUpdate update = new PresenceUpdate();
        userRepository.findByEmail(email).ifPresent(user -> update.setUserId(user.getId()));
        update.setEmail(email);
        update.setOnline(online);
        update.setOnlineCount(onlineEmails.size());
        update.setTimestamp(LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/presence", update);
    }
}
