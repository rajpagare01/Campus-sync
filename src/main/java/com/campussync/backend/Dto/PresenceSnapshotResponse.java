package com.campussync.backend.Dto;

import lombok.Data;

import java.util.List;

@Data
public class PresenceSnapshotResponse {
    private int onlineCount;
    private List<PresenceUserDto> users;
}
