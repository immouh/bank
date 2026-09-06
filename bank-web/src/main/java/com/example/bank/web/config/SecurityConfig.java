package com.example.bank.web.config;

import com.example.bank.web.dto.ErreurReponse;
import com.example.bank.web.securite.FiltreAuthentificationJwt;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Ce que Spring Security a le droit de faire ici, et surtout ce qu'il n'a PAS
 * à faire.
 *
 * IL NE VÉRIFIE AUCUN IDENTIFIANT. Pas de {@code UserDetailsService}, pas de
 * {@code PasswordEncoder}, pas d'{@code AuthenticationProvider} : ces briques
 * refaraient, en moins bien, ce que la chaîne {@code EtapeAuthentification}
 * du coeur fait déjà — existence du client, verrouillage après trois échecs,
 * comparaison BCrypt, avec la non-divulgation testée depuis la phase A. Les
 * dupliquer ici donnerait deux implémentations de la même règle, dont une
 * seule serait testée.
 *
 * Spring Security ne sert donc qu'à UNE chose : décider quelles routes
 * exigent un jeton valide, et refuser proprement celles qui n'en ont pas.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final FiltreAuthentificationJwt filtreJwt;
    private final ObjectMapper json;

    public SecurityConfig(FiltreAuthentificationJwt filtreJwt, ObjectMapper json) {
        this.filtreJwt = filtreJwt;
        this.json = json;
    }

    @Bean
    public SecurityFilterChain chaineDeFiltres(HttpSecurity http) throws Exception {
        return http
                // CSRF désactivé : la protection anti-CSRF défend les
                // formulaires HTML dont le navigateur envoie automatiquement
                // le cookie de session. Ici il n'y a ni cookie ni session —
                // le jeton est posé À LA MAIN dans un header par le code
                // appelant, ce qu'un site tiers ne peut pas faire.
                .csrf(csrf -> csrf.disable())
                // Aucune session HTTP, même pas créée à la demande : chaque
                // requête doit se justifier seule, par son jeton.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(routes -> routes
                        // La connexion est la seule porte ouverte : il faut
                        // bien pouvoir obtenir un jeton sans en avoir un.
                        .requestMatchers("/api/auth/login").permitAll()
                        .anyRequest().authenticated())
                // Le filtre JWT s'exécute AVANT le filtre de formulaire de
                // Spring : quand ce dernier arrive, l'identité est déjà posée
                // et il n'a rien à faire.
                .addFilterBefore(filtreJwt, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(erreurs ->
                        erreurs.authenticationEntryPoint(pointDEntree()))
                .build();
    }

    /**
     * Ce que voit un appelant sans jeton valide sur une route protégée.
     *
     * Déclaré explicitement pour deux raisons : le défaut de Spring Security 6
     * rend un 403 là où un 401 est la réponse juste (« authentifie-toi »,
     * pas « tu n'as pas le droit »), et le corps doit être le MÊME JSON
     * {@code {"erreur": ...}} que celui du gestionnaire d'exceptions. Sans
     * ça, l'API répondrait dans deux formats selon l'endroit où le refus se
     * décide.
     */
    private AuthenticationEntryPoint pointDEntree() {
        return (requete, reponse, refus) -> {
            reponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            reponse.setCharacterEncoding("UTF-8");
            json.writeValue(reponse.getOutputStream(),
                    new ErreurReponse("Authentification requise."));
        };
    }
}
