package com.openclassrooms.etudiant.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RegisterDTOTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    // Initialise le moteur de validation.
    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // Libère les ressources après les tests.
    @AfterAll
    static void tearDown() {
        factory.close();
    }

    // Construit un DTO d'inscription valide.
    private RegisterDTO createValidRegisterDTO() {

        RegisterDTO dto = new RegisterDTO();

        dto.setFirstName("Test");
        dto.setLastName("User");
        dto.setLogin("testUser");
        dto.setPassword("testPassword");

        return dto;
    }

    // Vérifie qu'une inscription complète est valide.
    @Test
    void shouldAcceptValidRegistration() {

        // GIVEN
        RegisterDTO dto = createValidRegisterDTO();

        // WHEN / THEN
        assertTrue(validator.validate(dto).isEmpty());
    }

    // Vérifie individuellement chaque champ obligatoire.
    @ParameterizedTest
    @ValueSource(strings = {
            "firstName",
            "lastName",
            "login",
            "password"
    })
    void shouldRejectBlankRequiredField(String field) {

        // GIVEN
        RegisterDTO dto = createValidRegisterDTO();

        switch (field) {
            case "firstName" -> dto.setFirstName(" ");
            case "lastName" -> dto.setLastName(" ");
            case "login" -> dto.setLogin(" ");
            case "password" -> dto.setPassword(" ");
            default -> throw new IllegalArgumentException(field);
        }

        // WHEN
        var violations = validator.validate(dto);

        // THEN
        assertEquals(1, violations.size());

        assertEquals(
                field,
                violations.iterator().next()
                        .getPropertyPath().toString()
        );
    }

    // Vérifie l'égalité et la cohérence du hashCode.
    @Test
    void shouldConsiderIdenticalRegistrationDtosEqual() {

        // GIVEN
        RegisterDTO firstDto = createValidRegisterDTO();
        RegisterDTO secondDto = createValidRegisterDTO();

        // WHEN / THEN
        assertEquals(firstDto, secondDto);
        assertEquals(secondDto, firstDto);
        assertEquals(firstDto.hashCode(), secondDto.hashCode());

        assertTrue(firstDto.equals(firstDto));
        assertFalse(firstDto.equals(null));
        assertFalse(firstDto.equals(new Object()));
    }

    // Fournit les différences possibles entre deux DTO.
    static Stream<Consumer<RegisterDTO>> registrationDifferences() {

        return Stream.of(
                dto -> dto.setFirstName("Other"),
                dto -> dto.setLastName("Other"),
                dto -> dto.setLogin("otherUser"),
                dto -> dto.setPassword("otherPassword"),
                dto -> dto.setFirstName(null),
                dto -> dto.setLastName(null),
                dto -> dto.setLogin(null),
                dto -> dto.setPassword(null)
        );
    }

    // Vérifie que chaque différence rend les DTO inégaux.
    @ParameterizedTest
    @MethodSource("registrationDifferences")
    void shouldDetectDifferentRegistrationProperties(
            Consumer<RegisterDTO> modification) {

        // GIVEN
        RegisterDTO firstDto = createValidRegisterDTO();
        RegisterDTO secondDto = createValidRegisterDTO();

        // WHEN
        modification.accept(secondDto);

        // THEN
        assertNotEquals(firstDto, secondDto);
        assertNotEquals(secondDto, firstDto);
    }

    // Vérifie la comparaison des DTO contenant des valeurs nulles.
    @Test
    void shouldHandleNullRegistrationProperties() {

        // GIVEN
        RegisterDTO firstDto = new RegisterDTO();
        RegisterDTO secondDto = new RegisterDTO();

        // WHEN / THEN
        assertEquals(firstDto, secondDto);
        assertEquals(firstDto.hashCode(), secondDto.hashCode());
    }
}