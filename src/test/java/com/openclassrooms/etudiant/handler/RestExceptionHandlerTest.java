package com.openclassrooms.etudiant.handler;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.context.request.ServletWebRequest;

import java.nio.file.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler =
            new RestExceptionHandler();

    private final ServletWebRequest request =
            new ServletWebRequest(
                    new MockHttpServletRequest("POST", "/api/users")
            );

    // Vérifie le traitement d'une erreur métier.
    @Test
    void shouldReturnBadRequest() {

        // GIVEN
        IllegalArgumentException exception =
                new IllegalArgumentException("Données invalides");

        // WHEN
        ResponseEntity<Object> response =
                handler.handleConflict(exception, request);

        // THEN
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorDetails body = assertInstanceOf(
                ErrorDetails.class, response.getBody()
        );

        assertEquals("Données invalides", body.getMessage());
        assertNotNull(body.getTimestamp());
        assertEquals("uri=/api/users", body.getDetails());
    }

    // Vérifie le traitement des identifiants incorrects.
    @Test
    void shouldReturnUnauthorized() {

        // GIVEN
        BadCredentialsException exception =
                new BadCredentialsException("Identifiants incorrects");

        // WHEN
        ResponseEntity<Object> response =
                handler.handleBadCredentialsException(exception, request);

        // THEN
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        ErrorDetails body = assertInstanceOf(
                ErrorDetails.class, response.getBody()
        );

        assertEquals("Identifiants incorrects", body.getMessage());
    }

    // Vérifie le traitement d'un accès interdit.
    @Test
    void shouldReturnForbidden() {

        // GIVEN
        AccessDeniedException exception =
                new AccessDeniedException("Accès interdit");

        // WHEN
        ResponseEntity<Object> response =
                handler.handleForbiddenException(exception, request);

        // THEN
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        ErrorDetails body = assertInstanceOf(
                ErrorDetails.class, response.getBody()
        );

        assertEquals("Accès interdit", body.getMessage());
    }

    // Vérifie qu'une erreur interne ne divulgue pas
    // les informations techniques au client.
    @Test
    void shouldReturnInternalServerErrorWithoutSensitiveDetails() {

        // GIVEN
        RuntimeException exception =
                new RuntimeException("Erreur technique interne");

        // WHEN
        ResponseEntity<Object> response =
                handler.handleException(exception, request);

        // THEN
        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertEquals("Internal Server error", response.getBody());
    }
}