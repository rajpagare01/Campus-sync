package com.campussync.backend.config;

import com.campussync.backend.Dto.UserProfileResponse;
import com.campussync.backend.Model.Role;
import com.campussync.backend.Service.FeedService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RedisSerializationConfigTest {

    private final RedisSerializer<Object> serializer = new RedisConfig().redisValueSerializer();

    @Test
    void serializesUserProfileResponseWithJavaTimeFields() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        UserProfileResponse profile = new UserProfileResponse(
                1L,
                "Test User",
                "test@example.com",
                Role.STUDENT,
                "bio",
                "profile.png",
                true,
                now,
                now,
                1,
                2,
                3,
                4,
                5,
                6,
                true,
                false,
                false
        );

        Object restored = serializer.deserialize(serializer.serialize(profile));

        assertThat(restored).isInstanceOf(UserProfileResponse.class);
        assertThat(restored).usingRecursiveComparison().isEqualTo(profile);
    }

    @Test
    void serializesFeedStatsCachePayload() {
        FeedService.FeedStats stats = new FeedService.FeedStats(10L, 20L, 5L);

        Object restored = serializer.deserialize(serializer.serialize(stats));

        assertThat(restored).isInstanceOf(FeedService.FeedStats.class);
        assertThat(restored).usingRecursiveComparison().isEqualTo(stats);
    }
}
