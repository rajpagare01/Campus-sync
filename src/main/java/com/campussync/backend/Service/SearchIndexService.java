package com.campussync.backend.Service;

import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.Post;
import com.campussync.backend.Model.User;
import org.springframework.stereotype.Service;

@Service
public class SearchIndexService {

    public void indexPost(Post post) {
        // Search indexing disabled. Search uses relational queries only.
    }

    public void indexEvent(Event event) {
        // Search indexing disabled. Search uses relational queries only.
    }

    public void indexUser(User user) {
        // Search indexing disabled. Search uses relational queries only.
    }

    public void deletePost(Long postId) {
        // Search indexing disabled. Search uses relational queries only.
    }

    public void deleteEvent(Long eventId) {
        // Search indexing disabled. Search uses relational queries only.
    }

    public void deleteUser(Long userId) {
        // Search indexing disabled. Search uses relational queries only.
    }
}
