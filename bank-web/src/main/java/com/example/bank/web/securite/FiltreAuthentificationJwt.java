package com.example.bank.web.securite;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Le portier de l'API : lit le jeton de chaque requête et dit à Spring
 * Security QUI la fait.
 *
 * UN FILTRE, c'est du code qui s'exécute avant que la requête n'atteigne le
 * moindre contrôleur. Spring Security n'est d'ailleurs qu'une chaîne de
 * filtres : celui-ci s'y insère avant celui qui vérifie les identifiants d'un
 * formulaire, dont ce projet ne se sert pas.
 *
 * CE QU'IL NE FAIT PAS — il ne vérifie aucun mot de passe et ne consulte
 * aucun verrouillage. Ces deux contrôles appartiennent à la chaîne
 * {@code EtapeAuthentification} du coeur, qui ne s'exécute qu'une fois, à la
 * connexion. Ici on ne fait que constater qu'un jeton valide a déjà été
 * délivré.
 *
 * UN REFUS N'EST PAS UNE ERREUR ICI — jeton absent, mal signé ou expiré : le
 * filtre laisse simplement passer la requête SANS poser d'authentification.
 * C'est la configuration de sécurité qui conclut, en refusant l'accès à une
 * route protégée non authentifiée (401, voir {@code SecurityConfig}). Écrire
 * la réponse d'erreur ici mélangerait deux responsabilités et donnerait deux
 * endroits où le format du refus pourrait diverger.
 */
@Component
public class FiltreAuthentificationJwt extends OncePerRequestFilter {

    private static final String PREFIXE = "Bearer ";

    private final JwtService jwtService;

    public FiltreAuthentificationJwt(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete,
                                    HttpServletResponse reponse,
                                    FilterChain suite) throws ServletException, IOException {
        jetonDe(requete)
                .flatMap(jwtService::ribDuJeton)
                .ifPresent(rib -> authentifier(rib, requete));
        suite.doFilter(requete, reponse);
    }

    /**
     * Pose l'identité dans le CONTEXTE DE SÉCURITÉ — le porte-documents que
     * Spring attache au thread de la requête. Les contrôleurs y liront le RIB
     * (voir {@link ClientAuthentifie}) au lieu de le recevoir en paramètre :
     * c'est ce qui rend impossible de lire les comptes d'un autre client en
     * changeant un identifiant dans l'URL.
     *
     * La liste d'autorités est vide : l'application n'a pas de rôles. Être
     * authentifié suffit à tout faire — sur SES propres comptes.
     */
    private void authentifier(int rib, HttpServletRequest requete) {
        UsernamePasswordAuthenticationToken authentification =
                new UsernamePasswordAuthenticationToken(rib, null, List.of());
        authentification.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(requete));
        SecurityContextHolder.getContext().setAuthentication(authentification);
    }

    /** Valeur du header {@code Authorization: Bearer <jeton>}, si elle est là. */
    private java.util.Optional<String> jetonDe(HttpServletRequest requete) {
        String entete = requete.getHeader(HttpHeaders.AUTHORIZATION);
        if (entete == null || !entete.startsWith(PREFIXE)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(entete.substring(PREFIXE.length()).trim());
    }
}
