package com.recipenetwork.backend.feed;

import java.util.List;

public record FeedResponse(List<FeedItemResponse> content, int page, int size, long totalElements) {
}
