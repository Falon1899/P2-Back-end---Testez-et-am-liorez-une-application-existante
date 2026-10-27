package com.openclassrooms.etudiant.service;


import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

//import de la classe necessaire à la génération du token 
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;

//import de l'annotation @Value
import org.springframework.beans.factory.annotation.Value;
//import de la classe qui nou spermettra de décoder la clé Base64
import io.jsonwebtoken.io.Decoders;
//import de l'outil de création de clé 
import io.jsonwebtoken.security.Keys;
//import de la classe de la bibliothèque JJWT qui permet notamment de créer une clé cryptographique à partir d'un tableau d'octets
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    public String generateToken(UserDetails userDetails) {
        // Décode la clé secrète Base64 en tableau d'octets pour pouvoir signer le JWT
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        // Crée une clé cryptographique à partir des octets pour signer le JWT
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        // Signe le JWT avec la clé secrète pour garantir son authenticité et son intégrité

        // Définit une date d'expiration à 1 heure après la génération du JWT
        Date expiration = Date.from(Instant.now().plusSeconds(3600));

        return Jwts.builder().subject(userDetails.getUsername()).expiration(expiration).signWith(key).compact();
    }

}
