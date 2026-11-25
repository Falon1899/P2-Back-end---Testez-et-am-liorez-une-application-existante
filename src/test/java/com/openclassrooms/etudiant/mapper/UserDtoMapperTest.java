package com.openclassrooms.etudiant.mapper;

import com.openclassrooms.etudiant.dto.RegisterDTO;
import com.openclassrooms.etudiant.entities.User;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class UserDtoMapperTest {

    // Récupération de l'implémentation réelle générée par MapStruct.
    // Aucun contexte Spring ni mock n'est nécessaire.
    private final UserDtoMapper userDtoMapper =
            Mappers.getMapper(UserDtoMapper.class);

    /**
     * Vérifie que toutes les données d'inscription sont
     * correctement transférées vers l'entité User.
     */
    @Test
    void shouldMapRegisterDtoToUser() {

        // GIVEN
        RegisterDTO registerDTO = new RegisterDTO();

        registerDTO.setFirstName("Test");
        registerDTO.setLastName("User");
        registerDTO.setLogin("testUser");
        registerDTO.setPassword("testPassword");

        // WHEN
        User result = userDtoMapper.toEntity(registerDTO);

        // THEN
        assertNotNull(result);

        assertAll(
                () -> assertEquals(
                        registerDTO.getFirstName(),
                        result.getFirstName()
                ),
                () -> assertEquals(
                        registerDTO.getLastName(),
                        result.getLastName()
                ),
                () -> assertEquals(
                        registerDTO.getLogin(),
                        result.getLogin()
                ),
                () -> assertEquals(
                        registerDTO.getPassword(),
                        result.getPassword()
                )
        );
    }

    /**
     * Vérifie que les champs gérés par la persistance
     * ne sont pas initialisés par le mapper.
     */
    @Test
    void shouldIgnorePersistenceFields() {

        // GIVEN
        RegisterDTO registerDTO = new RegisterDTO();

        registerDTO.setFirstName("Test");
        registerDTO.setLastName("User");
        registerDTO.setLogin("testUser");
        registerDTO.setPassword("testPassword");

        // WHEN
        User result = userDtoMapper.toEntity(registerDTO);

        // THEN
        assertNotNull(result);

        assertAll(
                () -> assertNull(result.getId()),
                () -> assertNull(result.getCreated_at()),
                () -> assertNull(result.getUpdated_at())
        );
    }

    /**
     * Vérifie que le mapper retourne null lorsque
     * le DTO fourni est null.
     */
    @Test
    void shouldReturnNullWhenRegisterDtoIsNull() {

        // GIVEN
        RegisterDTO registerDTO = null;

        // WHEN
        User result = userDtoMapper.toEntity(registerDTO);

        // THEN
        assertNull(result);
    }

    /**
     * Vérifie que le mapper ne modifie pas les données
     * et ne réalise aucune validation métier.
     */
    @Test
    void shouldPreserveEmptyFields() {

        // GIVEN
        RegisterDTO registerDTO = new RegisterDTO();

        registerDTO.setFirstName("");
        registerDTO.setLastName("");
        registerDTO.setLogin("");
        registerDTO.setPassword("");

        // WHEN
        User result = userDtoMapper.toEntity(registerDTO);

        // THEN
        assertNotNull(result);

        assertAll(
                () -> assertEquals("", result.getFirstName()),
                () -> assertEquals("", result.getLastName()),
                () -> assertEquals("", result.getLogin()),
                () -> assertEquals("", result.getPassword())
        );
    }
}