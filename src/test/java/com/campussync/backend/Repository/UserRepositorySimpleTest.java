package com.campussync.backend.Repository;
import com.campussync.backend.Model.User;
import com.campussync.backend.Model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("User Repository Tests")
class UserRepositorySimpleTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save user")
    void testSaveUser() {
        User user = new User();
        user.setName("John");
        user.setEmail("john@test.com");
        user.setPassword("password");
        user.setRole(Role.STUDENT);

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("john@test.com");
    }

    @Test
    @DisplayName("Should find user by email")
    void testFindByEmail() {
        User user = new User();
        user.setName("Jane");
        user.setEmail("jane@test.com");
        user.setPassword("password");
        user.setRole(Role.STUDENT);
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("jane@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Jane");
    }

    @Test
    @DisplayName("Should return empty when user not found")
    void testFindByEmailNotFound() {
        Optional<User> found = userRepository.findByEmail("nonexistent@test.com");

        assertThat(found).isEmpty();
    }
}
