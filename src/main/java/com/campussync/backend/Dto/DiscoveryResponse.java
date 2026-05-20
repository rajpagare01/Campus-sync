package com.campussync.backend.Dto;

import com.campussync.backend.Model.Comment;
import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.Post;
import com.campussync.backend.Model.User;
import lombok.Data;

import java.util.List;

@Data
public class DiscoveryResponse {
    private String query;
    private List<Post> posts;
    private List<Event> events;
    private List<User> users;
    private List<Comment> comments;
}
