package com.campussync.backend.Dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaginatedEventResponse extends PaginatedResponse<EventResponse> {
    public PaginatedEventResponse(List<EventResponse> content, int page, int size, long totalElements, int totalPages, boolean first, boolean last, boolean empty) {
        super(content, page, size, totalElements, totalPages, first, last, empty);
    }
}
