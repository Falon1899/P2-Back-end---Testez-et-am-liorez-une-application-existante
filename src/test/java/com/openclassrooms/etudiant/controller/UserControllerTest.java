
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

    // Démarre un conteneur MySQL pour les tests d'intégration.
    // La base utilisée est indépendante de la base de production.
    @Container
    static MySQLContainer<?> mySQLContainer =
            new MySQLContainer<>("mysql:8");

    // Injection des vrais composants de l'application Spring.
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    // MockMvc simule des requêtes HTTP sans démarrer
    // un navigateur ou un serveur HTTP externe.
    @Autowired
    private MockMvc mockMvc;

    // Configure automatiquement Spring pour utiliser
    // la base MySQL du conteneur de test.
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

    // Exécuté après chaque test.
    // Supprime les utilisateurs créés afin que chaque test
    // démarre avec une base vide.
    @AfterEach
    public void afterEach() {
        userRepository.deleteAll();
    }

    // ==========================================================
    // MÉTHODES UTILITAIRES
    // ==========================================================

    // Prépare un utilisateur fictif pour les scénarios.
    // L'utilisateur n'est pas encore enregistré en base.
    private User createTestUser() {

        User user = new User();

        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setLogin(LOGIN);
        user.setPassword(PASSWORD);

        return user;
    }

    // Enregistre un utilisateur dans la vraie base de test.
    // UserService se charge notamment du hachage du mot de passe.
    //
    // On récupère ensuite l'utilisateur enregistré pour obtenir
    // son identifiant généré par MySQL.
    private User saveTestUser() {

        userService.register(createTestUser());

        return userRepository.findByLogin(LOGIN)
                .orElseThrow();
    }

    // Génère un véritable JWT signé pour l'utilisateur.
    //
    // Les routes /api/users et /api/students sont protégées.
    // Nous devons donc fournir un jeton valide pour tester
    // leur comportement métier.
    private String generateTestToken(User user) {

        return jwtService.generateToken(user);
    }

    // Prépare un DTO d'inscription valide.
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

        // GIVEN : un formulaire d'inscription valide.
        RegisterDTO registerDTO = createRegisterDTO();

        // WHEN : envoi de la requête HTTP POST.
        // THEN : l'API retourne HTTP 201.
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

        // GIVEN : un formulaire sans informations obligatoires.
        RegisterDTO registerDTO = new RegisterDTO();

        // WHEN : tentative d'inscription.
        // THEN : rejet avec HTTP 400.
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

        // GIVEN : un utilisateur existe déjà dans la base.
        saveTestUser();

        // Préparation d'une inscription avec le même login.
        RegisterDTO registerDTO = createRegisterDTO();

        // WHEN : tentative d'inscription.
        // THEN : rejet avec HTTP 400.
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

        // GIVEN : un utilisateur enregistré.
        saveTestUser();

        // Préparation des identifiants corrects.
        LoginRequestDTO request = new LoginRequestDTO();

        request.setLogin(LOGIN);
        request.setPassword(PASSWORD);

        // WHEN : connexion à /api/login.
        // THEN : HTTP 200 et réponse au format JWT.
        //
        // Un JWT signé possède trois parties :
        // HEADER.PAYLOAD.SIGNATURE
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

        // Préparation d'identifiants incorrects.
        LoginRequestDTO request = new LoginRequestDTO();

        request.setLogin(LOGIN);
        request.setPassword("wrongPassword");

        // WHEN : tentative de connexion.
        // THEN : refus HTTP 401.
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

        // GIVEN : un utilisateur enregistré dans MySQL.
        User user = saveTestUser();

        // Génération d'un JWT valide pour accéder à la route.
        String token = generateTestToken(user);

        // WHEN : requête GET /api/users avec authentification.
        // THEN : HTTP 200 et présence de l'utilisateur.
        //
        // On vérifie également que le mot de passe
        // n'est jamais exposé dans la réponse JSON.
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

        // Préparation des données du nouvel étudiant.
        RegisterDTO studentDTO = new RegisterDTO();

        studentDTO.setFirstName("Student");
        studentDTO.setLastName("Example");
        studentDTO.setLogin("student123");
        studentDTO.setPassword("studentPassword123");

        // WHEN : création via POST /api/students.
        // THEN : HTTP 201.
        mockMvc.perform(MockMvcRequestBuilders.post("/api/students")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentDTO)))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        // THEN : vérification supplémentaire de la persistance.
        //
        // Le code HTTP 201 ne suffit pas à prouver
        // que l'étudiant existe réellement dans MySQL.
        org.junit.jupiter.api.Assertions.assertTrue(
                userRepository.findByLogin("student123").isPresent()
        );
    }

    // ==========================================================
    // BI-08 : CONSULTATION D'UN ÉTUDIANT PAR SON ID
    // ==========================================================

    @Test
    public void getStudentByIdReturnsStudent() throws Exception {

        // GIVEN : un étudiant enregistré en base.
        User user = saveTestUser();

        String token = generateTestToken(user);

        // L'identifiant est généré automatiquement par MySQL.
        Long studentId = user.getId();

        // WHEN : consultation de cet étudiant.
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

        // Préparation des nouvelles informations.
        UpdateStudentDTO updateDTO = new UpdateStudentDTO();

        updateDTO.setFirstName("Updated");
        updateDTO.setLastName("Student");
        updateDTO.setLogin("updatedStudent123");

        // WHEN : modification avec PUT /api/students/{id}.
        // THEN : HTTP 200 et données mises à jour.
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

        // THEN : vérification directe dans la base.
        //
        // On s'assure que les modifications sont persistées,
        // et pas seulement renvoyées dans la réponse HTTP.
        User updatedUser = userRepository.findById(studentId)
                .orElseThrow();

        org.junit.jupiter.api.Assertions.assertEquals(
                "Updated", updatedUser.getFirstName());

        org.junit.jupiter.api.Assertions.assertEquals(
                "updatedStudent123", updatedUser.getLogin());
    }

    // ==========================================================
    // BI-10 : SUPPRESSION D'UN ÉTUDIANT
    // ==========================================================

    @Test
    public void deleteStudentRemovesStudent() throws Exception {

        // GIVEN : un utilisateur authentifié et un étudiant à supprimer.
        User authenticatedUser = saveTestUser();

        String token = generateTestToken(authenticatedUser);

        // Création d'un second utilisateur qui sera supprimé.
        User student = new User();

        student.setFirstName("Student");
        student.setLastName("ToDelete");
        student.setLogin("studentToDelete");
        student.setPassword("studentPassword123");

        userService.register(student);

        // Récupération de l'identifiant généré par MySQL.
        Long studentId = userRepository
                .findByLogin("studentToDelete")
                .orElseThrow()
                .getId();

        // WHEN : suppression via DELETE /api/students/{id}.
        // THEN : HTTP 204 No Content.
        mockMvc.perform(MockMvcRequestBuilders.delete(
                                "/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        // THEN : vérification de la suppression en base.
        //
        // findById doit retourner Optional.empty()
        // puisque l'étudiant ne doit plus exister.
        org.junit.jupiter.api.Assertions.assertFalse(
                userRepository.findById(studentId).isPresent()
        );
    }
}
