package com.campussync.backend.Repository;

import com.campussync.backend.Model.Follow;
import com.campussync.backend.Model.User;
import com.campussync.backend.Model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Follow Repository Tests")
class  FollowRepositorySimpleTest {

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserRepository userRepository;

    private User follower;
    private User following;

    @BeforeEach
    void setUp() {
        followRepository.deleteAll();
        userRepository.deleteAll();

        follower = new User();
        follower.setName("Follower");
        follower.setEmail("follower@test.com");
        follower.setPassword("password");
        follower.setRole(Role.STUDENT);
        follower = userRepository.save(follower);

        following = new User();
        following.setName("Following");
        following.setEmail("following@test.com");
        following.setPassword("password");
        following.setRole(Role.SOCIETY);
        following = userRepository.save(following);
    }

    @Test
    @DisplayName("Should save follow")
    void testSaveFollow() {
        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        follow.setCreatedAt(LocalDateTime.now());

        Follow saved = followRepository.save(follow);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFollower().getId()).isEqualTo(follower.getId());
    }

    @Test
    @DisplayName("Should check if follow exists")
    void testExistsByFollowerAndFollowing() {
        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        follow.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow);

        boolean exists = followRepository.existsByFollowerIdAndFollowingId(follower.getId(), following.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should count followers")
    void testCountByFollowingId() {
        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        follow.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow);

        long count = followRepository.countByFollowingId(following.getId());

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should count following")
    void testCountByFollowerId() {
        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        follow.setCreatedAt(LocalDateTime.now());
        followRepository.save(follow);

        long count = followRepository.countByFollowerId(follower.getId());

        assertThat(count).isEqualTo(1);
    }
}
