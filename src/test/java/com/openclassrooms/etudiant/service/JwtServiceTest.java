
package com.openclassrooms.etudiant.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.WeakKeyException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JwtServiceTest {

    // Identifiant fictif utilisé dans les différents tests.
    private static final String LOGIN = "testUser";

    // Clé fictive exclusivement réservée aux tests.
    // Elle ne doit jamais être utilisée en production.
    private static final String TEST_SECRET = Base64.getEncoder()
            .encodeToString(
                    "0123456789abcdef0123456789abcdef"
                            .getBytes(StandardCharsets.UTF_8)
            );

    // Deuxième clé permettant de simuler un token
    // signé par une autre application.
    private static final String OTHER_SECRET = Base64.getEncoder()
            .encodeToString(
                    "abcdef0123456789abcdef0123456789"
                            .getBytes(StandardCharsets.UTF_8)
            );

    // Instance réelle du service que nous souhaitons tester.
    private JwtService jwtService;

    // Utilisateur fictif utilisé pour générer les tokens.
    private UserDetails userDetails;


    // =========================================================
    // INITIALISATION DES TESTS
    // =========================================================

    @BeforeEach
    void setUp() {

        // Instancie un nouveau JwtService avant chaque test.
        // Cela garantit l'indépendance des tests.
        jwtService = new JwtService();

        // Injecte directement la clé secrète dans l'attribut privé
        // jwtSecret, normalement renseigné par Spring avec @Value.
        //
        // Nous évitons ainsi de démarrer le contexte Spring Boot
        // ou de dépendre de la variable JWT_SECRET du poste.
        ReflectionTestUtils.setField(
                jwtService,
                "jwtSecret",
                TEST_SECRET
        );

        // Crée un utilisateur fictif compatible avec Spring Security.
        userDetails = User.withUsername(LOGIN)
                .password("testPassword")
                .authorities("ROLE_USER")
                .build();
    }


    // =========================================================
    // MÉTHODES UTILITAIRES
    // =========================================================

    // Transforme une clé Base64 en clé cryptographique.
    private SecretKey createSigningKey(String secret) {

        byte[] keyBytes = Base64.getDecoder().decode(secret);

        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Génère un token avec une clé et une date d'expiration
    // personnalisées pour simuler différents scénarios.
    private String createToken(
            String username,
            Instant expiration,
            String secret
    ) {

        return Jwts.builder()
                .subject(username)
                .expiration(Date.from(expiration))
                .signWith(createSigningKey(secret))
                .compact();
    }


    // =========================================================
    // TESTS UNITAIRES : GENERATE TOKEN
    // =========================================================

    @Test
    void generateTokenShouldReturnSignedJwt() {

        // GIVEN : un utilisateur authentifié et une clé valide.

        // WHEN : génération du token par le véritable service.
        String token = jwtService.generateToken(userDetails);

        // THEN : le token doit exister.
        assertThat(token)
                .isNotNull()
                .isNotBlank();

        // Un JWT signé contient trois parties :
        // header, payload et signature.
        assertThat(token.split("\\."))
                .hasSize(3);

        // Analyse indépendamment le token généré
        // et vérifie sa signature.
        Claims claims = Jwts.parser()
                .verifyWith(createSigningKey(TEST_SECRET))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Vérifie que le sujet correspond à l'utilisateur.
        assertThat(claims.getSubject())
                .isEqualTo(LOGIN);
    }

    @Test
    void generateTokenShouldExpireAfterOneHour() {

        // GIVEN : mémorise l'heure avant la génération du token.
        Instant beforeGeneration = Instant.now();

        // WHEN : génération du token.
        String token = jwtService.generateToken(userDetails);

        // Mémorise l'heure après sa génération.
        Instant afterGeneration = Instant.now();

        // Décode le token pour récupérer ses informations.
        Claims claims = Jwts.parser()
                .verifyWith(createSigningKey(TEST_SECRET))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Récupère la date d'expiration du token.
        Instant expiration = claims.getExpiration().toInstant();

        // THEN : le token doit expirer environ une heure
        // après sa génération.
        //
        // Une seconde de tolérance est appliquée à la borne
        // inférieure car le format JWT utilise des secondes
        // et non des millisecondes.
        assertThat(expiration)
                .isBetween(
                        beforeGeneration.plusSeconds(3599),
                        afterGeneration.plusSeconds(3600)
                );
    }

    @Test
    void generateTokenShouldRejectWeakSecret() {

        // GIVEN : une clé secrète volontairement trop courte.
        String weakSecret = Base64.getEncoder()
                .encodeToString(
                        "shortKey".getBytes(StandardCharsets.UTF_8)
                );

        // Remplace la clé valide par la clé insuffisante.
        ReflectionTestUtils.setField(
                jwtService,
                "jwtSecret",
                weakSecret
        );

        // WHEN / THEN : la génération doit être refusée
        // car la clé ne respecte pas les exigences cryptographiques.
        assertThrows(
                WeakKeyException.class,
                () -> jwtService.generateToken(userDetails)
        );
    }


    // =========================================================
    // TESTS UNITAIRES : EXTRACT USERNAME
    // =========================================================

    @Test
    void extractUsernameShouldReturnCorrectUsername() {

        // GIVEN : un token valide créé indépendamment
        // de la méthode generateToken du service.
        String token = createToken(
                LOGIN,
                Instant.now().plusSeconds(3600),
                TEST_SECRET
        );

        // WHEN : extraction du sujet du token.
        String username = jwtService.extractUsername(token);

        // THEN : le sujet doit correspondre à l'utilisateur.
        assertThat(username).isEqualTo(LOGIN);
    }

    @Test
    void extractUsernameShouldReturnUsernameFromGeneratedToken() {

        // GIVEN : un token généré par notre service.
        String token = jwtService.generateToken(userDetails);

        // WHEN : extraction de l'identifiant.
        String username = jwtService.extractUsername(token);

        // THEN : les deux méthodes doivent être compatibles.
        assertThat(username).isEqualTo(LOGIN);
    }

    @Test
    void extractUsernameShouldRejectExpiredToken() {

        // GIVEN : un token ayant expiré depuis une heure.
        String expiredToken = createToken(
                LOGIN,
                Instant.now().minusSeconds(3600),
                TEST_SECRET
        );

        // WHEN / THEN : le service doit refuser le token expiré.
        assertThrows(
                ExpiredJwtException.class,
                () -> jwtService.extractUsername(expiredToken)
        );
    }

    @Test
    void extractUsernameShouldRejectTamperedToken() {

        // GIVEN : un token valide.
        String token = jwtService.generateToken(userDetails);

        // Sépare les trois parties du JWT.
        String[] parts = token.split("\\.");

        // Récupère la signature sous forme d'octets.
        byte[] signature = Base64.getUrlDecoder()
                .decode(parts[2]);

        // Modifie volontairement le premier octet de la signature.
        // Le token ne correspondra plus à sa signature originale.
        signature[0] ^= 1;

        // Réencode la signature modifiée au format Base64 URL.
        String modifiedSignature = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(signature);

        // Reconstruit le token avec la signature altérée.
        String tamperedToken = parts[0]
                + "."
                + parts[1]
                + "."
                + modifiedSignature;

        // WHEN / THEN : la signature doit être rejetée.
        assertThrows(
                SignatureException.class,
                () -> jwtService.extractUsername(tamperedToken)
        );
    }

    @Test
    void extractUsernameShouldRejectTokenSignedWithAnotherKey() {

        // GIVEN : un token signé avec une autre clé secrète.
        String token = createToken(
                LOGIN,
                Instant.now().plusSeconds(3600),
                OTHER_SECRET
        );

        // WHEN / THEN : le service doit refuser ce token,
        // car il ne peut pas vérifier sa signature
        // avec sa propre clé secrète.
        assertThrows(
                SignatureException.class,
                () -> jwtService.extractUsername(token)
        );
    }

    @Test
    void extractUsernameShouldRejectMalformedToken() {

        // GIVEN : une chaîne qui ne respecte pas le format JWT.
        String malformedToken = "invalidToken";

        // WHEN / THEN : le service doit rejeter le token.
        assertThrows(
                MalformedJwtException.class,
                () -> jwtService.extractUsername(malformedToken)
        );
    }

    @Test
    void extractUsernameShouldRejectEmptyToken() {

        // GIVEN : un token vide.
        String emptyToken = "";

        // WHEN / THEN : le service doit refuser son analyse.
        assertThrows(
                IllegalArgumentException.class,
                () -> jwtService.extractUsername(emptyToken)
        );
    }

    @Test
    void extractUsernameShouldRejectNullToken() {

        // GIVEN : aucun token n'est fourni.

        // WHEN / THEN : le service doit refuser son analyse.
        assertThrows(
                IllegalArgumentException.class,
                () -> jwtService.extractUsername(null)
        );
    }
}
