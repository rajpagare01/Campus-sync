package com.campussync.backend.Service;

import com.campussync.backend.Dto.DiscoveryResponse;
import com.campussync.backend.Model.Comment;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.EventStatus;
import com.campussync.backend.Model.EventType;
import com.campussync.backend.Model.Post;
import com.campussync.backend.Model.User;
import com.campussync.backend.Repository.CommentRepository;
import com.campussync.backend.Repository.EventRepository;
import com.campussync.backend.Repository.PostRepository;
import com.campussync.backend.Repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private final PostRepository postRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    public SearchService(PostRepository postRepository,
                         EventRepository eventRepository,
                         UserRepository userRepository,
                         CommentRepository commentRepository) {
        this.postRepository = postRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
    }

    public Page<Post> searchPosts(String query, Pageable pageable) {
        return searchPosts(query, null, pageable);
    }

    public Page<Post> searchPosts(String query, Long authorId, Pageable pageable) {
        return authorId == null
                ? postRepository.findByContentContainsIgnoreCase(normalize(query), pageable)
                : postRepository.findByAuthorId(authorId, pageable);
    }

    public Page<Event> searchEvents(String query, Pageable pageable) {
        return searchEvents(query, null, null, pageable);
    }

    public Page<Event> searchEvents(String query, EventType type, String status, Pageable pageable) {
        EventStatus eventStatus = status == null ? null : EventStatus.valueOf(status);
        return eventRepository.searchEvents(normalize(query), type, eventStatus, pageable);
    }

    public Page<User> searchUsers(String query, Pageable pageable) {
        return searchUsers(query, null, pageable);
    }

    public Page<User> searchUsers(String query, Boolean verified, Pageable pageable) {
        if (Boolean.TRUE.equals(verified)) {
            return userRepository.findByIsVerifiedTrue(pageable);
        }
        return userRepository.searchUsers(normalize(query), pageable);
    }

    public Page<Comment> searchComments(String query, Pageable pageable) {
        return commentRepository.findByContentContainsIgnoreCase(normalize(query), pageable);
    }

    public Page<Post> searchPostsByAuthor(Long authorId, Pageable pageable) {
        return postRepository.findByAuthorId(authorId, pageable);
    }

    public Page<Event> searchEventsByLocation(String location, Pageable pageable) {
        return eventRepository.findByVenueContainsIgnoreCaseOrderByDateDesc(location, pageable);
    }

    public Page<Event> searchEventsByCategory(String category, Pageable pageable) {
        EventType type;
        try {
            type = EventType.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Page.empty(pageable);
        }
        return eventRepository.findByType(type, pageable);
    }

    public Page<User> searchUsersByDepartment(String department, Pageable pageable) {
        return userRepository.searchUsers(department, pageable);
    }

    public Page<User> getTopUsersByFollowers(Pageable pageable) {
        return userRepository.findTopUsersByFollowers(pageable);
    }

    public Page<User> getVerifiedUsers(Pageable pageable) {
        return userRepository.findByIsVerifiedTrue(pageable);
    }

    public Page<Post> getPublishedPosts(Pageable pageable) {
        return postRepository.findRecentPosts(pageable);
    }

    public Page<Comment> getPostComments(Long postId, Pageable pageable) {
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId, pageable);
    }

    public DiscoveryResponse discover(String query, int sizePerSection) {
        Pageable pageable = PageRequest.of(0, sizePerSection);

        DiscoveryResponse response = new DiscoveryResponse();
        response.setQuery(query);
        response.setPosts(searchPosts(query, pageable).getContent());
        response.setEvents(searchEvents(query, pageable).getContent());
        response.setUsers(searchUsers(query, pageable).getContent());
        response.setComments(searchComments(query, pageable).getContent());
        return response;
    }

    private String normalize(String query) {
        return query == null ? "" : query.trim();
    }
}
