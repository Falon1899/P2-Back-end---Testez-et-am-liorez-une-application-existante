package com.openclassrooms.etudiant.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ErrorDetailsTest {

    private static final LocalDateTime TIMESTAMP =
            LocalDateTime.of(2026, 1, 1, 10, 0);

    // Construit un objet représentant une erreur HTTP.
    private ErrorDetails createErrorDetails() {

        return new ErrorDetails(
                TIMESTAMP,
                "Données invalides",
                "uri=/api/users"
        );
    }

    // Vérifie l'initialisation par le constructeur complet.
    @Test
    void shouldInitializeErrorDetails() {

        // WHEN
        ErrorDetails error = createErrorDetails();

        // THEN
        assertAll(
                () -> assertEquals(
                        TIMESTAMP, error.getTimestamp()
                ),
                () -> assertEquals(
                        "Données invalides", error.getMessage()
                ),
                () -> assertEquals(
                        "uri=/api/users", error.getDetails()
                )
        );
    }

    // Vérifie la modification des propriétés d'une erreur.
    @Test
    void shouldUpdateErrorDetails() {

        // GIVEN
        ErrorDetails error = new ErrorDetails();

        // WHEN
        error.setTimestamp(TIMESTAMP);
        error.setMessage("Données invalides");
        error.setDetails("uri=/api/users");

        // THEN
        assertAll(
                () -> assertEquals(TIMESTAMP, error.getTimestamp()),
                () -> assertEquals(
                        "Données invalides", error.getMessage()
                ),
                () -> assertEquals(
                        "uri=/api/users", error.getDetails()
                )
        );
    }

    // Vérifie l'égalité et le hashCode de deux erreurs.
    @Test
    void shouldConsiderIdenticalErrorsEqual() {

        // GIVEN
        ErrorDetails firstError = createErrorDetails();
        ErrorDetails secondError = createErrorDetails();

        // WHEN / THEN
        assertEquals(firstError, secondError);
        assertEquals(secondError, firstError);
        assertEquals(
                firstError.hashCode(),
                secondError.hashCode()
        );

        assertTrue(firstError.equals(firstError));
        assertFalse(firstError.equals(null));
        assertFalse(firstError.equals(new Object()));
    }

    // Fournit les différences possibles entre deux erreurs.
    static Stream<Consumer<ErrorDetails>> errorDifferences() {

        return Stream.of(
                error -> error.setTimestamp(
                        TIMESTAMP.plusDays(1)
                ),
                error -> error.setMessage("Autre erreur"),
                error -> error.setDetails("uri=/api/login"),
                error -> error.setTimestamp(null),
                error -> error.setMessage(null),
                error -> error.setDetails(null)
        );
    }

    // Vérifie que chaque différence rend les erreurs inégales.
    @ParameterizedTest
    @MethodSource("errorDifferences")
    void shouldDetectDifferentErrorProperties(
            Consumer<ErrorDetails> modification) {

        // GIVEN
        ErrorDetails firstError = createErrorDetails();
        ErrorDetails secondError = createErrorDetails();

        // WHEN
        modification.accept(secondError);

        // THEN
        assertNotEquals(firstError, secondError);
        assertNotEquals(secondError, firstError);
    }

    // Vérifie le traitement des propriétés nulles.
    @Test
    void shouldHandleNullErrorProperties() {

        // GIVEN
        ErrorDetails firstError = new ErrorDetails();
        ErrorDetails secondError = new ErrorDetails();

        // WHEN / THEN
        assertEquals(firstError, secondError);
        assertEquals(
                firstError.hashCode(),
                secondError.hashCode()
        );
    }

    // Vérifie la représentation textuelle d'une erreur.
    @Test
    void shouldProvideReadableErrorDescription() {

        // GIVEN
        ErrorDetails error = createErrorDetails();

        // WHEN
        String description = error.toString();

        // THEN
        assertAll(
                () -> assertTrue(
                        description.contains("Données invalides")
                ),
                () -> assertTrue(
                        description.contains("uri=/api/users")
                ),
                () -> assertTrue(
                        description.contains(TIMESTAMP.toString())
                )
        );
    }
}