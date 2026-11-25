package com.openclassrooms.etudiant.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private User createUser() {
        return new User(
                1L,
                "Test",
                "User",
                "testUser",
                "encodedPassword",
                LocalDateTime.of(2026, 1, 1, 10, 0),
                LocalDateTime.of(2026, 1, 2, 10, 0)
        );
    }

    // Vérifie les informations utilisées par Spring Security.
    @Test
    void shouldProvideAuthenticationInformation() {

        // GIVEN
        User user = new User();
        user.setLogin("testUser");
        user.setPassword("encodedPassword");

        // WHEN / THEN
        assertAll(
                () -> assertEquals("testUser", user.getUsername()),
                () -> assertEquals("encodedPassword", user.getPassword()),
                () -> assertNotNull(user.getAuthorities()),
                () -> assertTrue(user.getAuthorities().isEmpty())
        );
    }

    // Vérifie la politique de disponibilité des comptes.
    @Test
    void shouldAllowAuthenticationForActiveAccount() {

        // GIVEN
        User user = new User();

        // WHEN / THEN
        assertAll(
                () -> assertTrue(user.isEnabled()),
                () -> assertTrue(user.isAccountNonExpired()),
                () -> assertTrue(user.isAccountNonLocked()),
                () -> assertTrue(user.isCredentialsNonExpired())
        );
    }

    // Vérifie le contrat d'égalité et de hashCode.
    @Test
    void shouldConsiderIdenticalUsersEqual() {

        // GIVEN
        User firstUser = createUser();
        User secondUser = createUser();

        // WHEN / THEN
        assertEquals(firstUser, secondUser);
        assertEquals(secondUser, firstUser);
        assertEquals(firstUser.hashCode(), secondUser.hashCode());

        assertTrue(firstUser.equals(firstUser));
        assertFalse(firstUser.equals(null));
        assertFalse(firstUser.equals(new Object()));
    }

    // Fournit les différences à tester sur chaque propriété.
    static Stream<Consumer<User>> userDifferences() {

        return Stream.of(
                user -> user.setId(2L),
                user -> user.setFirstName("Other"),
                user -> user.setLastName("Other"),
                user -> user.setLogin("otherUser"),
                user -> user.setPassword("otherPassword"),
                user -> user.setCreated_at(
                        LocalDateTime.of(2026, 2, 1, 10, 0)
                ),
                user -> user.setUpdated_at(
                        LocalDateTime.of(2026, 2, 2, 10, 0)
                ),
                user -> user.setId(null),
                user -> user.setFirstName(null),
                user -> user.setLastName(null),
                user -> user.setLogin(null),
                user -> user.setPassword(null),
                user -> user.setCreated_at(null),
                user -> user.setUpdated_at(null)
        );
    }

    // Vérifie que chaque différence rend les objets inégaux.
    @ParameterizedTest
    @MethodSource("userDifferences")
    void shouldDetectDifferentUserProperties(
            Consumer<User> modification) {

        // GIVEN
        User firstUser = createUser();
        User secondUser = createUser();

        // WHEN
        modification.accept(secondUser);

        // THEN
        assertNotEquals(firstUser, secondUser);
        assertNotEquals(secondUser, firstUser);
    }

    // Vérifie le traitement des propriétés nulles.
    @Test
    void shouldHandleNullUserProperties() {

        // GIVEN
        User firstUser = new User();
        User secondUser = new User();
        User completeUser = createUser();

        // WHEN / THEN
        assertEquals(firstUser, secondUser);
        assertEquals(firstUser.hashCode(), secondUser.hashCode());

        assertNotEquals(firstUser, completeUser);
        assertNotEquals(completeUser, firstUser);
    }

    // Vérifie l'initialisation par le constructeur complet.
    @Test
    void shouldInitializeAllUserProperties() {

        // WHEN
        User user = createUser();

        // THEN
        assertAll(
                () -> assertEquals(1L, user.getId()),
                () -> assertEquals("Test", user.getFirstName()),
                () -> assertEquals("User", user.getLastName()),
                () -> assertEquals("testUser", user.getLogin()),
                () -> assertEquals(
                        "encodedPassword", user.getPassword()
                ),
                () -> assertEquals(
                        LocalDateTime.of(2026, 1, 1, 10, 0),
                        user.getCreated_at()
                ),
                () -> assertEquals(
                        LocalDateTime.of(2026, 1, 2, 10, 0),
                        user.getUpdated_at()
                )
        );
    }
}