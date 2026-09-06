package com.example.bank.web.securite;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

/**
 * Fabrique et vérifie les jetons JWT.
 *
 * POURQUOI UN JETON PLUTÔT QU'UNE SESSION — une session HTTP demande au
 * serveur de retenir qui est connecté, donc de la mémoire partagée entre
 * requêtes et un état à répliquer si l'application tourne en plusieurs
 * exemplaires. Un jeton signé déplace cette information chez le client : le
 * serveur ne retient rien, il se contente de vérifier une signature. C'est ce
 * qui rend l'API STATELESS, et ce qui permet à un front web, une application
 * mobile ou un test d'intégration de parler à la même API sans cookie.
 *
 * CE QUE LE JETON CONTIENT — le RIB, et rien d'autre. Un JWT est SIGNÉ, pas
 * CHIFFRÉ : n'importe qui peut lire sa charge utile en la décodant en base64.
 * Y mettre un mot de passe, un haché ou un solde reviendrait à le publier.
 * La signature garantit seulement que personne ne l'a MODIFIÉ.
 */
@Service
public class JwtService {

    /**
     * Une heure : assez pour une session de travail sans réauthentification,
     * assez court pour qu'un jeton volé cesse vite de servir. Sans mécanisme
     * de révocation (voir PHASE_G_EXPLICATIONS.md), l'expiration est la SEULE
     * chose qui limite la durée de vie d'un jeton compromis.
     */
    public static final Duration VALIDITE = Duration.ofHours(1);

    private static final String REVENDICATION_NOM = "nom";

    private final Key cle;

    /**
     * Le secret vient de la configuration, jamais du code. HMAC-SHA256 exige
     * au moins 256 bits : la clé est donc rejetée à la construction si elle
     * est trop courte, plutôt que de laisser passer une signature faible.
     */
    public JwtService(@Value("${bank.jwt.secret}") String secret) {
        byte[] octets = secret.getBytes(StandardCharsets.UTF_8);
        if (octets.length < 32) {
            throw new IllegalStateException(
                    "bank.jwt.secret doit faire au moins 32 caractères (256 bits pour HS256).");
        }
        this.cle = Keys.hmacShaKeyFor(octets);
    }

    /** Jeton signé pour ce client, valable {@link #VALIDITE}. */
    public String genererPour(int rib, String nom) {
        Date maintenant = new Date();
        return Jwts.builder()
                // Le SUJET du jeton est le RIB : c'est lui, et lui seul, qui
                // dira plus tard quels comptes la requête a le droit de lire.
                .setSubject(String.valueOf(rib))
                .claim(REVENDICATION_NOM, nom)
                .setIssuedAt(maintenant)
                .setExpiration(new Date(maintenant.getTime() + VALIDITE.toMillis()))
                .signWith(cle, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * RIB porté par un jeton valide, vide si le jeton est absent, mal formé,
     * mal signé ou expiré.
     *
     * TOUS LES REFUS SE RESSEMBLENT, volontairement : rendre un {@code
     * Optional} vide plutôt que de propager la {@code JwtException} évite que
     * l'appelant ne renvoie au client un message distinguant « signature
     * invalide » de « jeton expiré ». Un attaquant n'apprend pas où en est sa
     * tentative de forge.
     */
    public Optional<Integer> ribDuJeton(String jeton) {
        try {
            Claims charge = Jwts.parserBuilder()
                    .setSigningKey(cle)
                    .build()
                    // Vérifie la signature ET l'expiration : un jeton périmé
                    // lève ici, il n'y a pas de contrôle de date à écrire.
                    .parseClaimsJws(jeton)
                    .getBody();
            return Optional.of(Integer.valueOf(charge.getSubject()));
        } catch (JwtException | IllegalArgumentException refus) {
            return Optional.empty();
        }
    }
}
