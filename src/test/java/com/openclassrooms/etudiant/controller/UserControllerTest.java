
package com.openclassrooms.etudiant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.RegisterDTO;
import com.openclassrooms.etudiant.dto.UpdateStudentDTO;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.UserRepository;
import com.openclassrooms.etudiant.service.JwtService;
import com.openclassrooms.etudiant.service.UserService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public class UserControllerTest {

    private static final String URL = "/api/register";
    private static final String FIRST_NAME = "Test";
    private static final String LAST_NAME = "User";
    private static final String LOGIN = "testUser";
    private static final String PASSWORD = "testPassword123";

    // ==========================================================
    // CONFIGURATION DE L'ENVIRONNEMENT DE TEST
    // ==========================================================

    // Utilisation de MySQL 8, version validée dans notre projet.
    // Testcontainers démarre une base temporaire pour les tests.
    @Container
    static MySQLContainer<?> mySQLContainer =
            new MySQLContainer<>("mysql:8");

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    // MockMvc permet de simuler des requêtes HTTP
    // qui traversent les contrôleurs et les filtres Spring.
    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url",
                () -> mySQLContainer.getJdbcUrl());

        registry.add("spring.datasource.username",
                () -> mySQLContainer.getUsername());

        registry.add("spring.datasource.password",
                () -> mySQLContainer.getPassword());

        registry.add("spring.jpa.hibernate.ddl-auto",
                () -> "create");
    }

    // Nettoyage après chaque test pour éviter
    // qu'un scénario influence les suivants.
    @AfterEach
    public void afterEach() {
        userRepository.deleteAll();
    }

    // ==========================================================
    // MÉTHODES UTILITAIRES
    // ==========================================================

    // Prépare un utilisateur fictif sans l'enregistrer.
    private User createTestUser() {

        User user = new User();

        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setLogin(LOGIN);
        user.setPassword(PASSWORD);

        return user;
    }

    // Enregistre l'utilisateur avec le vrai service métier.
    // Le mot de passe est haché par UserService.
    private User saveTestUser() {

        userService.register(createTestUser());

        return userRepository.findByLogin(LOGIN)
                .orElseThrow();
    }

    // Génère un JWT réel signé avec la clé de l'application.
    private String generateTestToken(User user) {

        return jwtService.generateToken(user);
    }

    // Prépare les données nécessaires à une inscription.
    private RegisterDTO createRegisterDTO() {

        RegisterDTO dto = new RegisterDTO();

        dto.setFirstName(FIRST_NAME);
        dto.setLastName(LAST_NAME);
        dto.setLogin(LOGIN);
        dto.setPassword(PASSWORD);

        return dto;
    }

    // ==========================================================
    // BI-01 : INSCRIPTION VALIDE
    // ==========================================================

    @Test
    public void registerUserSuccessful() throws Exception {

        // GIVEN : des données d'inscription valides.
        RegisterDTO registerDTO = createRegisterDTO();

        // WHEN : envoi de la requête HTTP.
        // THEN : réponse HTTP 201.
        mockMvc.perform(MockMvcRequestBuilders.post(URL)
                        .content(objectMapper.writeValueAsString(registerDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    // ==========================================================
    // BI-02 : INSCRIPTION AVEC DONNÉES MANQUANTES
    // ==========================================================

    @Test
    public void registerUserWithoutRequiredData() throws Exception {

        // GIVEN : un formulaire d'inscription vide.
        RegisterDTO registerDTO = new RegisterDTO();

        // WHEN : tentative d'inscription.
        // THEN : rejet HTTP 400.
        mockMvc.perform(MockMvcRequestBuilders.post(URL)
                        .content(objectMapper.writeValueAsString(registerDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    // ==========================================================
    // BI-03 : INSCRIPTION EN DOUBLON
    // ==========================================================

    @Test
    public void registerAlreadyExistUser() throws Exception {

        // GIVEN : un utilisateur déjà enregistré.
        saveTestUser();

        RegisterDTO registerDTO = createRegisterDTO();

        // WHEN : tentative d'inscription avec le même login.
        // THEN : rejet HTTP 400.
        mockMvc.perform(MockMvcRequestBuilders.post(URL)
                        .content(objectMapper.writeValueAsString(registerDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    // ==========================================================
    // BI-04 : CONNEXION AVEC IDENTIFIANTS VALIDES
    // ==========================================================

    @Test
    public void loginWithValidCredentialsReturnsJwt() throws Exception {

        // GIVEN : un utilisateur existant.
        saveTestUser();

        LoginRequestDTO request = new LoginRequestDTO();
        request.setLogin(LOGIN);
        request.setPassword(PASSWORD);

        // WHEN : connexion avec les bons identifiants.
        // THEN : HTTP 200 et chaîne au format JWT.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(
                        matchesPattern(
                                "^[^.\\s]+\\.[^.\\s]+\\.[^.\\s]+$")));
    }

    // ==========================================================
    // BI-05 : CONNEXION AVEC MOT DE PASSE INCORRECT
    // ==========================================================

    @Test
    public void loginWithWrongPasswordReturnsUnauthorized() throws Exception {

        // GIVEN : un utilisateur enregistré.
        saveTestUser();

        LoginRequestDTO request = new LoginRequestDTO();
        request.setLogin(LOGIN);
        request.setPassword("wrongPassword");

        // WHEN : connexion avec un mot de passe incorrect.
        // THEN : rejet HTTP 401.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    // ==========================================================
    // BI-06 : RÉCUPÉRATION DE LA LISTE DES UTILISATEURS
    // ==========================================================

    @Test
    public void getAllUsersReturnsUserList() throws Exception {

        // GIVEN : un utilisateur authentifié.
        User user = saveTestUser();
        String token = generateTestToken(user);

        // WHEN : GET /api/users avec un JWT valide.
        // THEN : HTTP 200 et informations de l'utilisateur.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].login")
                        .value(LOGIN))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].firstName")
                        .value(FIRST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].password")
                        .doesNotExist());
    }

    // ==========================================================
    // BI-07 : CRÉATION D'UN ÉTUDIANT
    // ==========================================================

    @Test
    public void createStudentReturnsCreated() throws Exception {

        // GIVEN : un utilisateur authentifié.
        User user = saveTestUser();
        String token = generateTestToken(user);

        RegisterDTO studentDTO = new RegisterDTO();
        studentDTO.setFirstName("Student");
        studentDTO.setLastName("Example");
        studentDTO.setLogin("student123");
        studentDTO.setPassword("studentPassword123");

        // WHEN : création d'un nouvel étudiant.
        // THEN : HTTP 201.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/students")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentDTO)))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        // Vérification de la persistance dans MySQL.
        assertTrue(
                userRepository.findByLogin("student123").isPresent()
        );
    }

    // ==========================================================
    // BI-08 : CONSULTATION D'UN ÉTUDIANT
    // ==========================================================

    @Test
    public void getStudentByIdReturnsStudent() throws Exception {

        // GIVEN : un étudiant existant.
        User user = saveTestUser();
        String token = generateTestToken(user);
        Long studentId = user.getId();

        // WHEN : consultation de l'étudiant par son ID.
        // THEN : HTTP 200 et informations correspondantes.
        mockMvc.perform(MockMvcRequestBuilders.get(
                                "/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id")
                        .value(studentId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.login")
                        .value(LOGIN))
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName")
                        .value(FIRST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$.password")
                        .doesNotExist());
    }

    // ==========================================================
    // BI-09 : MODIFICATION D'UN ÉTUDIANT
    // ==========================================================

    @Test
    public void updateStudentPersistsChanges() throws Exception {

        // GIVEN : un étudiant existant.
        User user = saveTestUser();
        String token = generateTestToken(user);
        Long studentId = user.getId();

        UpdateStudentDTO updateDTO = new UpdateStudentDTO();
        updateDTO.setFirstName("Updated");
        updateDTO.setLastName("Student");
        updateDTO.setLogin("updatedStudent123");

        // WHEN : modification de l'étudiant.
        // THEN : HTTP 200 et nouvelles données.
        mockMvc.perform(MockMvcRequestBuilders.put(
                                "/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName")
                        .value("Updated"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.login")
                        .value("updatedStudent123"));

        // Vérification des modifications en base.
        User updatedUser = userRepository.findById(studentId)
                .orElseThrow();

        assertEquals("Updated", updatedUser.getFirstName());
        assertEquals("updatedStudent123", updatedUser.getLogin());
    }

    // ==========================================================
    // BI-10 : SUPPRESSION D'UN ÉTUDIANT
    // ==========================================================

    @Test
    public void deleteStudentRemovesStudent() throws Exception {

        // GIVEN : un utilisateur authentifié.
        User authenticatedUser = saveTestUser();
        String token = generateTestToken(authenticatedUser);

        // Création d'un étudiant distinct à supprimer.
        User student = new User();
        student.setFirstName("Student");
        student.setLastName("ToDelete");
        student.setLogin("studentToDelete");
        student.setPassword("studentPassword123");

        userService.register(student);

        Long studentId = userRepository
                .findByLogin("studentToDelete")
                .orElseThrow()
                .getId();

        // WHEN : suppression de l'étudiant.
        // THEN : HTTP 204.
        mockMvc.perform(MockMvcRequestBuilders.delete(
                                "/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        // Vérification que l'étudiant n'existe plus en base.
        assertFalse(userRepository.findById(studentId).isPresent());
    }

    // ==========================================================
    // BI-11 : ACCÈS À UNE ROUTE PROTÉGÉE SANS JWT
    // ==========================================================

    @Test
    public void getAllUsersWithoutJwtReturnsUnauthorized() throws Exception {

        // GIVEN : aucune authentification.
        //
        // Nous ne créons volontairement aucun utilisateur
        // et nous ne générons aucun jeton JWT.
        //
        // L'objectif est de tester la sécurité de la route,
        // pas le contenu de la base de données.

        // WHEN : appel de GET /api/users sans en-tête Authorization.
        //
        // MockMvc fait traverser la requête aux filtres
        // de Spring Security avant d'atteindre le contrôleur.

        // THEN : Spring Security doit refuser la requête
        // avec HTTP 401 Unauthorized.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    // ==========================================================
    // BI-12 : ACCÈS À UNE ROUTE PROTÉGÉE AVEC JWT INVALIDE
    // ==========================================================

    @Test
    public void getAllUsersWithInvalidJwtReturnsUnauthorized() throws Exception {

        // GIVEN : un jeton JWT volontairement invalide.
        //
        // Cette chaîne possède trois parties séparées par
        // des points, mais ne contient pas de signature valide.
        //
        // JwtAuthenticationFilter doit tenter de traiter
        // le jeton et refuser l'authentification.

        String invalidToken = "invalid.header.signature";

        // WHEN : appel de GET /api/users avec le mauvais JWT.
        //
        // L'en-tête Authorization utilise bien le préfixe
        // Bearer attendu par notre filtre de sécurité.

        // THEN : le filtre doit répondre HTTP 401.
        //
        // Aucun utilisateur ne doit être authentifié
        // à partir de ce jeton invalide.
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users")
                        .header("Authorization", "Bearer " + invalidToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }
}
