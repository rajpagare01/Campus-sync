package com.campussync.backend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LikeToggleResponse {
    private boolean liked;
    private int likeCount;
    private LikeResponse like;
}
