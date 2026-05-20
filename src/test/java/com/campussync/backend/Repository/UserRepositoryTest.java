package com.campussync.backend.Repository;

import com.campussync.backend.Model.User;
import com.campussync.backend.Model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for UserRepository
 * Tests all repository methods including custom queries
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Unit Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword123");
        testUser.setRole(Role.STUDENT);
        testUser.setVerified(true);
    }

    @Test
    @DisplayName("Should save and retrieve user successfully")
    void testSaveAndFindUser() {
        // When
        User savedUser = userRepository.save(testUser);

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Should find user by email")
    void testFindByEmail() {
        // Given
        userRepository.save(testUser);

        // When
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void testFindByEmailNotFound() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("Should update user successfully")
    void testUpdateUser() {
        // Given
        User savedUser = userRepository.save(testUser);

        // When
        savedUser.setName("Updated Name");
        User updatedUser = userRepository.save(savedUser);

        // Then
        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("Should delete user successfully")
    void testDeleteUser() {
        // Given
        User savedUser = userRepository.save(testUser);
        Long userId = savedUser.getId();

        // When
        userRepository.deleteById(userId);

        // Then
        Optional<User> deletedUser = userRepository.findById(userId);
        assertThat(deletedUser).isEmpty();
    }

    @Test
    @DisplayName("Should handle duplicate email gracefully")
    void testDuplicateEmail() {
        // Given
        userRepository.save(testUser);
        User duplicateUser = new User();
        duplicateUser.setEmail("test@example.com");
        duplicateUser.setName("Another User");
        duplicateUser.setPassword("password123");

        // When & Then
        assertThatThrownBy(() -> userRepository.save(duplicateUser))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should save user with different roles")
    void testSaveUserWithDifferentRoles() {
        // Test STUDENT role
        User studentUser = new User();
        studentUser.setName("Student");
        studentUser.setEmail("student@example.com");
        studentUser.setPassword("password123");
        studentUser.setRole(Role.STUDENT);
        User savedStudent = userRepository.save(studentUser);
        assertThat(savedStudent.getRole()).isEqualTo(Role.STUDENT);

        // Test SOCIETY role
        User societyUser = new User();
        societyUser.setName("Society");
        societyUser.setEmail("society@example.com");
        societyUser.setPassword("password123");
        societyUser.setRole(Role.SOCIETY);
        User savedSociety = userRepository.save(societyUser);
        assertThat(savedSociety.getRole()).isEqualTo(Role.SOCIETY);

        // Test ADMIN role
        User adminUser = new User();
        adminUser.setName("Admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("password123");
        adminUser.setRole(Role.ADMIN);
        User savedAdmin = userRepository.save(adminUser);
        assertThat(savedAdmin.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Should handle null fields appropriately")
    void testNullFieldHandling() {
        // Given - Create user with minimal required fields
        User minimalUser = new User();
        minimalUser.setEmail("minimal@example.com");
        minimalUser.setPassword("password123");

        // When
        User savedUser = userRepository.save(minimalUser);

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("minimal@example.com");
    }

    @Test
    @DisplayName("Should count total users")
    void testCountUsers() {
        // Given
        userRepository.save(testUser);
        User anotherUser = new User();
        anotherUser.setName("Another User");
        anotherUser.setEmail("another@example.com");
        anotherUser.setPassword("password123");
        userRepository.save(anotherUser);

        // When
        long count = userRepository.count();

        // Then
        assertThat(count).isGreaterThanOrEqualTo(2);
    }
}
