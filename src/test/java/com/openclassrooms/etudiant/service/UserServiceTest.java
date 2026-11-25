
package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.dto.UpdateStudentDTO;
import com.openclassrooms.etudiant.dto.UserResponseDTO;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Initialise Mockito sans démarrer le contexte Spring Boot.
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String LOGIN = "testUser";
    private static final String PASSWORD = "testPassword";
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final String JWT_TOKEN = "testJwtToken";
    private static final Long USER_ID = 1L;

    // Simule les interactions avec la base de données.
    @Mock
    private UserRepository userRepository;

    // Simule le mécanisme de hachage des mots de passe.
    @Mock
    private PasswordEncoder passwordEncoder;

    // Simule la génération des tokens JWT.
    @Mock
    private JwtService jwtService;

    // Instancie le véritable UserService et injecte les mocks.
    @InjectMocks
    private UserService userService;


    // =========================================================
    // MÉTHODES UTILITAIRES
    // =========================================================

    // Crée un utilisateur fictif pour les différents tests.
    private User createTestUser() {

        User user = new User();

        user.setId(USER_ID);
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setLogin(LOGIN);
        user.setPassword(PASSWORD);

        return user;
    }

    // Crée un DTO contenant les nouvelles informations
    // d'un étudiant.
    private UpdateStudentDTO createUpdateDTO(String login) {

        UpdateStudentDTO dto = new UpdateStudentDTO();

        dto.setFirstName("UpdatedFirstName");
        dto.setLastName("UpdatedLastName");
        dto.setLogin(login);

        return dto;
    }


    // =========================================================
    // TESTS UNITAIRES : REGISTER
    // =========================================================

    @Test
    public void test_create_null_user_throws_IllegalArgumentException() {

        // GIVEN : aucun utilisateur n'est fourni.

        // WHEN : tentative d'inscription d'un utilisateur null.
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(null)
        );

        // THEN : vérification de l'exception.
        assertThat(exception.getMessage())
                .isEqualTo("User must not be null");

        // Vérifie qu'aucune dépendance n'a été sollicitée.
        verifyNoInteractions(
                userRepository,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    public void test_create_already_exist_user_throws_IllegalArgumentException() {

        // GIVEN : un utilisateur possède déjà ce login.
        User user = createTestUser();

        when(userRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(user));

        // WHEN : tentative d'inscription avec le même login.
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(user)
        );

        // THEN : l'inscription doit être refusée.
        assertThat(exception.getMessage())
                .contains("already exists");

        // Vérifie que la recherche du login a été effectuée.
        verify(userRepository).findByLogin(LOGIN);

        // Aucun utilisateur ne doit être sauvegardé.
        verify(userRepository, never()).save(any(User.class));

        // Le mot de passe ne doit pas être encodé.
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    public void test_create_user() {

        // GIVEN : un utilisateur dont le login est disponible.
        User user = createTestUser();

        when(userRepository.findByLogin(LOGIN))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode(PASSWORD))
                .thenReturn(ENCODED_PASSWORD);

        // WHEN : inscription de l'utilisateur.
        userService.register(user);

        // THEN : capture de l'utilisateur transmis au repository.
        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        // Vérifie les informations réellement sauvegardées.
        assertThat(savedUser.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(savedUser.getLastName()).isEqualTo(LAST_NAME);
        assertThat(savedUser.getLogin()).isEqualTo(LOGIN);

        // Vérifie que le mot de passe sauvegardé est encodé.
        assertThat(savedUser.getPassword())
                .isEqualTo(ENCODED_PASSWORD)
                .isNotEqualTo(PASSWORD);

        verify(passwordEncoder).encode(PASSWORD);
        verify(userRepository).findByLogin(LOGIN);
    }

    @Test
    public void registerShouldPropagateRepositoryException() {

        // GIVEN : un utilisateur valide.
        User user = createTestUser();

        when(userRepository.findByLogin(LOGIN))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode(PASSWORD))
                .thenReturn(ENCODED_PASSWORD);

        // Simule une erreur lors de la sauvegarde.
        when(userRepository.save(any(User.class)))
                .thenThrow(new RuntimeException("Database error"));

        // WHEN : tentative d'inscription.
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.register(user)
        );

        // THEN : l'erreur doit être propagée par le service.
        assertThat(exception.getMessage())
                .isEqualTo("Database error");

        verify(userRepository).save(user);
    }


    // =========================================================
    // TESTS UNITAIRES : LOGIN
    // =========================================================

    @Test
    public void loginShouldReturnJwtWhenCredentialsAreValid() {

        // GIVEN : un utilisateur enregistré avec un mot de passe encodé.
        User user = createTestUser();
        user.setPassword(ENCODED_PASSWORD);

        when(userRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(user));

        // Simule une correspondance entre les mots de passe.
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD))
                .thenReturn(true);

        // Simule la génération d'un token JWT.
        when(jwtService.generateToken(any(UserDetails.class)))
                .thenReturn(JWT_TOKEN);

        // WHEN : authentification de l'utilisateur.
        String token = userService.login(LOGIN, PASSWORD);

        // THEN : le token retourné doit être celui généré.
        assertThat(token).isEqualTo(JWT_TOKEN);

        // Capture les informations transmises au service JWT.
        ArgumentCaptor<UserDetails> userDetailsCaptor =
                ArgumentCaptor.forClass(UserDetails.class);

        verify(jwtService)
                .generateToken(userDetailsCaptor.capture());

        UserDetails capturedUser = userDetailsCaptor.getValue();

        assertThat(capturedUser.getUsername()).isEqualTo(LOGIN);
        assertThat(capturedUser.getPassword())
                .isEqualTo(ENCODED_PASSWORD);

        verify(passwordEncoder)
                .matches(PASSWORD, ENCODED_PASSWORD);
    }

    @Test
    public void loginShouldThrowExceptionWhenPasswordIsIncorrect() {

        // GIVEN : un utilisateur existant.
        User user = createTestUser();
        user.setPassword(ENCODED_PASSWORD);

        when(userRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(user));

        // Simule un mot de passe incorrect.
        when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD))
                .thenReturn(false);

        // WHEN : tentative d'authentification.
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> userService.login(LOGIN, PASSWORD)
        );

        // THEN : l'authentification doit être refusée.
        assertThat(exception.getMessage())
                .isEqualTo("Invalid credentials");

        // Aucun token ne doit être généré.
        verifyNoInteractions(jwtService);
    }

    @Test
    public void loginShouldThrowExceptionWhenUserDoesNotExist() {

        // GIVEN : aucun utilisateur ne possède ce login.
        when(userRepository.findByLogin(LOGIN))
                .thenReturn(Optional.empty());

        // WHEN : tentative d'authentification.
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> userService.login(LOGIN, PASSWORD)
        );

        // THEN : l'authentification doit être refusée.
        assertThat(exception.getMessage())
                .isEqualTo("Invalid credentials");

        // Aucun mot de passe ne doit être vérifié.
        // Aucun token ne doit être généré.
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    public void loginShouldThrowExceptionWhenLoginIsNull() {

        // GIVEN : un login null.

        // WHEN : tentative d'authentification.
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.login(null, PASSWORD)
        );

        // THEN : vérification du message d'erreur.
        assertThat(exception.getMessage())
                .isEqualTo("Login must not be null");

        verifyNoInteractions(
                userRepository,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    public void loginShouldThrowExceptionWhenPasswordIsNull() {

        // GIVEN : un mot de passe null.

        // WHEN : tentative d'authentification.
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.login(LOGIN, null)
        );

        // THEN : vérification du message d'erreur.
        assertThat(exception.getMessage())
                .isEqualTo("Password must not be null");

        verifyNoInteractions(
                userRepository,
                passwordEncoder,
                jwtService
        );
    }


    // =========================================================
    // TESTS UNITAIRES : GET ALL USERS
    // =========================================================

    @Test
    public void getAllUsersShouldReturnEmptyList() {

        // GIVEN : la base de données ne contient aucun utilisateur.
        when(userRepository.findAll())
                .thenReturn(List.of());

        // WHEN : récupération des utilisateurs.
        List<UserResponseDTO> result = userService.getAllUsers();

        // THEN : la liste retournée doit être vide.
        assertThat(result).isNotNull().isEmpty();

        verify(userRepository).findAll();
    }

    @Test
    public void getAllUsersShouldReturnMappedUserList() {

        // GIVEN : deux utilisateurs présents en base.
        User firstUser = createTestUser();

        User secondUser = createTestUser();
        secondUser.setId(2L);
        secondUser.setFirstName("Jane");
        secondUser.setLastName("Smith");
        secondUser.setLogin("testUser2");

        when(userRepository.findAll())
                .thenReturn(List.of(firstUser, secondUser));

        // WHEN : récupération de la liste des utilisateurs.
        List<UserResponseDTO> result = userService.getAllUsers();

        // THEN : vérifie le nombre d'utilisateurs retournés.
        assertThat(result).hasSize(2);

        // Vérifie le mapping du premier utilisateur.
        assertThat(result.get(0).getId()).isEqualTo(USER_ID);
        assertThat(result.get(0).getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(result.get(0).getLastName()).isEqualTo(LAST_NAME);
        assertThat(result.get(0).getLogin()).isEqualTo(LOGIN);

        // Vérifie le mapping du second utilisateur.
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getFirstName()).isEqualTo("Jane");
        assertThat(result.get(1).getLastName()).isEqualTo("Smith");
        assertThat(result.get(1).getLogin()).isEqualTo("testUser2");

        verify(userRepository).findAll();
    }


    // =========================================================
    // TESTS UNITAIRES : GET USER BY ID
    // =========================================================

    @Test
    public void getUserByIdShouldReturnUserWhenIdExists() {

        // GIVEN : un utilisateur existe avec cet identifiant.
        User user = createTestUser();

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        // WHEN : recherche de l'utilisateur.
        UserResponseDTO result = userService.getUserById(USER_ID);

        // THEN : vérifie le mapping des informations.
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(result.getLastName()).isEqualTo(LAST_NAME);
        assertThat(result.getLogin()).isEqualTo(LOGIN);

        verify(userRepository).findById(USER_ID);
    }

    @Test
    public void getUserByIdShouldThrowNotFoundWhenIdDoesNotExist() {

        // GIVEN : aucun utilisateur ne possède cet identifiant.
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        // WHEN : recherche de l'utilisateur.
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.getUserById(USER_ID)
        );

        // THEN : vérifie le statut HTTP 404.
        assertThat(exception.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(exception.getReason())
                .isEqualTo("Utilisateur introuvable");

        verify(userRepository).findById(USER_ID);
    }


    // =========================================================
    // TESTS UNITAIRES : UPDATE STUDENT
    // =========================================================

    @Test
    public void updateStudentShouldUpdateUserWhenLoginIsUnchanged() {

        // GIVEN : un utilisateur existant conserve son login.
        User user = createTestUser();

        UpdateStudentDTO dto = createUpdateDTO(LOGIN);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        // Simule le retour de l'utilisateur sauvegardé.
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN : modification de l'utilisateur.
        UserResponseDTO result =
                userService.updateStudent(USER_ID, dto);

        // THEN : vérifie les informations retournées.
        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getFirstName()).isEqualTo(dto.getFirstName());
        assertThat(result.getLastName()).isEqualTo(dto.getLastName());
        assertThat(result.getLogin()).isEqualTo(LOGIN);

        // Le mot de passe doit rester inchangé.
        assertThat(user.getPassword()).isEqualTo(PASSWORD);

        // Le login étant inchangé, aucune recherche
        // de doublon ne doit être effectuée.
        verify(userRepository, never()).findByLogin(any());

        verify(userRepository).save(user);
    }

    @Test
    public void updateStudentShouldUpdateUserWhenNewLoginIsAvailable() {

        // GIVEN : l'utilisateur souhaite modifier son login.
        User user = createTestUser();

        String newLogin = "newTestUser";

        UpdateStudentDTO dto = createUpdateDTO(newLogin);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        // Le nouveau login est disponible.
        when(userRepository.findByLogin(newLogin))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN : modification de l'utilisateur.
        UserResponseDTO result =
                userService.updateStudent(USER_ID, dto);

        // THEN : vérifie les nouvelles informations.
        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getLogin()).isEqualTo(newLogin);
        assertThat(result.getFirstName()).isEqualTo(dto.getFirstName());
        assertThat(result.getLastName()).isEqualTo(dto.getLastName());

        // Vérifie également l'entité transmise au repository.
        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getLogin())
                .isEqualTo(newLogin);

        assertThat(userCaptor.getValue().getPassword())
                .isEqualTo(PASSWORD);

        verify(userRepository).findByLogin(newLogin);
    }

    @Test
    public void updateStudentShouldThrowConflictWhenLoginAlreadyExists() {

        // GIVEN : un utilisateur souhaite utiliser un login
        // déjà attribué à un autre utilisateur.
        User user = createTestUser();

        String existingLogin = "existingUser";

        User existingUser = createTestUser();
        existingUser.setId(2L);
        existingUser.setLogin(existingLogin);

        UpdateStudentDTO dto = createUpdateDTO(existingLogin);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(userRepository.findByLogin(existingLogin))
                .thenReturn(Optional.of(existingUser));

        // WHEN : tentative de modification.
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateStudent(USER_ID, dto)
        );

        // THEN : vérifie le statut HTTP 409.
        assertThat(exception.getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(exception.getReason())
                .isEqualTo("Ce login est déjà utilisé");

        // Aucune modification ne doit être sauvegardée.
        verify(userRepository, never()).save(any(User.class));

        // Les informations initiales doivent rester inchangées.
        assertThat(user.getLogin()).isEqualTo(LOGIN);
        assertThat(user.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(user.getLastName()).isEqualTo(LAST_NAME);
    }

    @Test
    public void updateStudentShouldThrowNotFoundWhenUserDoesNotExist() {

        // GIVEN : aucun utilisateur ne possède cet identifiant.
        UpdateStudentDTO dto = createUpdateDTO(LOGIN);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        // WHEN : tentative de modification.
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateStudent(USER_ID, dto)
        );

        // THEN : vérifie le statut HTTP 404.
        assertThat(exception.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(exception.getReason())
                .isEqualTo("Utilisateur introuvable");

        verify(userRepository, never()).save(any(User.class));
        verify(userRepository, never()).findByLogin(any());
    }


    // =========================================================
    // TESTS UNITAIRES : DELETE STUDENT
    // =========================================================

    @Test
    public void deleteStudentShouldDeleteUserWhenIdExists() {

        // GIVEN : un utilisateur existe avec cet identifiant.
        User user = createTestUser();

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        // WHEN : suppression de l'utilisateur.
        userService.deleteStudent(USER_ID);

        // THEN : vérifie que le repository a reçu
        // l'instruction de supprimer le bon utilisateur.
        verify(userRepository).findById(USER_ID);
        verify(userRepository).delete(user);
    }

    @Test
    public void deleteStudentShouldThrowNotFoundWhenUserDoesNotExist() {

        // GIVEN : aucun utilisateur ne possède cet identifiant.
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        // WHEN : tentative de suppression.
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.deleteStudent(USER_ID)
        );

        // THEN : vérifie le statut HTTP 404.
        assertThat(exception.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(exception.getReason())
                .isEqualTo("Utilisateur introuvable");

        // Aucune suppression ne doit être effectuée.
        verify(userRepository, never()).delete(any(User.class));
    }
}
