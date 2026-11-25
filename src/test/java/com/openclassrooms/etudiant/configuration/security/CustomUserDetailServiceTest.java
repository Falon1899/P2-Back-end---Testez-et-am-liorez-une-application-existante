
package com.openclassrooms.etudiant.configuration.security;

import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailServiceTest {

    // Simule le repository pour éviter toute interaction
    // avec la véritable base de données.
    @Mock
    private UserRepository userRepository;

    // Instancie le véritable service et lui injecte
    // le repository simulé par Mockito.
    @InjectMocks
    private CustomUserDetailService customUserDetailService;


    // =========================================================
    // CAS NOMINAL : UTILISATEUR EXISTANT
    // =========================================================

    @Test
    void loadUserByUsernameShouldReturnUserWhenUserExists() {

        // GIVEN : création d'un utilisateur fictif.
        String login = "testUser";

        User user = new User();
        user.setLogin(login);
        user.setPassword("testPassword");

        // Simule la présence de l'utilisateur en base de données.
        when(userRepository.findByLogin(login))
                .thenReturn(Optional.of(user));

        // WHEN : appel de la véritable méthode du service.
        UserDetails result =
                customUserDetailService.loadUserByUsername(login);

        // THEN : vérifie que le service retourne exactement
        // l'utilisateur fourni par le repository.
        assertThat(result).isSameAs(user);

        // Vérifie que le repository a été interrogé une seule fois
        // avec le bon identifiant.
        verify(userRepository, times(1)).findByLogin(login);
    }


    // =========================================================
    // CAS D'ERREUR : UTILISATEUR INEXISTANT
    // =========================================================

    @Test
    void loadUserByUsernameShouldThrowExceptionWhenUserDoesNotExist() {

        // GIVEN : un identifiant qui n'existe pas en base.
        String login = "unknownUser";

        // Simule l'absence de l'utilisateur dans le repository.
        when(userRepository.findByLogin(login))
                .thenReturn(Optional.empty());

        // WHEN / THEN : vérifie que le service déclenche
        // l'exception attendue lorsque l'utilisateur est absent.
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailService.loadUserByUsername(login)
        );

        // Vérifie que le message de l'exception correspond
        // au comportement défini dans le code de production.
        assertThat(exception.getMessage())
                .isEqualTo("User Not Found with username: " + login);

        // Vérifie que le repository a bien été interrogé
        // une seule fois avec l'identifiant demandé.
        verify(userRepository, times(1)).findByLogin(login);
    }
}
