package com.example.bank.web.controller;

import com.example.bank.web.BaseTestApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La connexion, vue depuis HTTP.
 *
 * CE QUI COMPTE LE PLUS ICI — que la couche web n'ait pas réintroduit la fuite
 * d'information que le coeur s'interdit depuis la phase A. Un code HTTP
 * différent entre « ce RIB n'existe pas » et « ce mot de passe est faux »
 * transformerait l'API en énumérateur de comptes, exactement comme un message
 * d'erreur différent le ferait côté Swing.
 */
@TestPropertySource(properties = "bank.base.url=jdbc:h2:mem:test-auth;DB_CLOSE_DELAY=-1")
@DisplayName("API — authentification")
class AuthentificationApiTest extends BaseTestApi {

    @Nested
    @DisplayName("Connexion réussie")
    class Succes {

        @Test
        @DisplayName("Des identifiants corrects rendent un jeton et l'identité du client")
        void identifiantsCorrectsRendentUnJeton() throws Exception {
            mockMvc.perform(postJson("/api/auth/login", new Connexion(MOUH_RIB, MOUH_MDP)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.typeToken").value("Bearer"))
                    .andExpect(jsonPath("$.rib").value(MOUH_RIB))
                    .andExpect(jsonPath("$.nom").value("Mouh"));
        }

        @Test
        @DisplayName("Le jeton annonce sa durée de validité, une heure")
        void jetonAnnonceSaValidite() throws Exception {
            mockMvc.perform(postJson("/api/auth/login", new Connexion(MOUH_RIB, MOUH_MDP)))
                    .andExpect(jsonPath("$.expireDansSecondes").value(3600));
        }

        @Test
        @DisplayName("Chaque client se connecte avec ses propres identifiants")
        void chaqueClientSeConnecteAvecLesSiens() throws Exception {
            mockMvc.perform(postJson("/api/auth/login", new Connexion(AMINE_RIB, AMINE_MDP)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rib").value(AMINE_RIB));
        }

        /**
         * Un JWT est SIGNÉ, pas chiffré : sa charge utile se lit en base64.
         * Ce test vérifie donc ce qui NE doit pas s'y trouver.
         */
        @Test
        @DisplayName("La réponse de connexion ne contient ni mot de passe ni haché")
        void reponseSansMotDePasse() throws Exception {
            MvcResult resultat = mockMvc.perform(
                            postJson("/api/auth/login", new Connexion(MOUH_RIB, MOUH_MDP)))
                    .andReturn();

            String corps = resultat.getResponse().getContentAsString();
            assertFalse(corps.contains(MOUH_MDP), corps);
            assertFalse(corps.contains("$2a$"), "Un haché BCrypt a fuité : " + corps);
        }
    }

    @Nested
    @DisplayName("Connexion refusée")
    class Echec {

        @Test
        @DisplayName("Un mot de passe faux est refusé en 401")
        void motDePasseFauxRefuse() throws Exception {
            mockMvc.perform(postJson("/api/auth/login", new Connexion(MOUH_RIB, "mauvais")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Un RIB inconnu est refusé en 401, pas en 404")
        void ribInconnuRefuseEn401() throws Exception {
            mockMvc.perform(postJson("/api/auth/login", new Connexion(999, "peu importe")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Le mot de passe d'un autre client ne donne pas accès au compte")
        void motDePasseDUnAutreClientRefuse() throws Exception {
            mockMvc.perform(postJson("/api/auth/login", new Connexion(MOUH_RIB, AMINE_MDP)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Le corps d'erreur est du JSON structuré, jamais une trace Java")
        void corpsDErreurStructure() throws Exception {
            mockMvc.perform(postJson("/api/auth/login", new Connexion(MOUH_RIB, "mauvais")))
                    .andExpect(jsonPath("$.erreur").isNotEmpty())
                    .andExpect(jsonPath("$.token").doesNotExist());
        }

        @Test
        @DisplayName("Un corps de requête absent est refusé en 400, pas en 500")
        void corpsAbsentRefuse() throws Exception {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erreur").isNotEmpty());
        }

        @Test
        @DisplayName("Un RIB absent du corps est refusé comme un identifiant faux")
        void ribAbsentRefuse() throws Exception {
            mockMvc.perform(postJson("/api/auth/login", new Connexion(null, MOUH_MDP)))
                    .andExpect(status().isUnauthorized());
        }
    }

    /**
     * LE TEST LE PLUS IMPORTANT DE CETTE CLASSE.
     *
     * Le coeur rend le même message pour un nom inconnu et un mot de passe
     * faux ; il suffirait que la couche web traduise l'un en 404 et l'autre en
     * 401 pour que l'API dise quels comptes existent. On compare donc les deux
     * réponses ENTIÈRES, code et corps.
     */
    @Nested
    @DisplayName("Non-divulgation")
    class NonDivulgation {

        @Test
        @DisplayName("RIB inconnu et mot de passe faux rendent exactement la même réponse")
        void reponsesIndiscernables() throws Exception {
            MvcResult inconnu = mockMvc.perform(
                    postJson("/api/auth/login", new Connexion(999, "mauvais"))).andReturn();
            MvcResult mauvaisMdp = mockMvc.perform(
                    postJson("/api/auth/login", new Connexion(MOUH_RIB, "mauvais"))).andReturn();

            assertEquals(inconnu.getResponse().getStatus(),
                    mauvaisMdp.getResponse().getStatus(),
                    "Le code HTTP distingue un RIB inconnu d'un mot de passe faux.");
            assertEquals(inconnu.getResponse().getContentAsString(),
                    mauvaisMdp.getResponse().getContentAsString(),
                    "Le corps de la réponse distingue un RIB inconnu d'un mot de passe faux.");
        }

        @Test
        @DisplayName("Le message de refus ne nomme ni le RIB ni le client")
        void messageSansIndice() throws Exception {
            MvcResult resultat = mockMvc.perform(
                    postJson("/api/auth/login", new Connexion(MOUH_RIB, "mauvais"))).andReturn();

            String erreur = lire(resultat).get("erreur").asText();
            assertFalse(erreur.contains(String.valueOf(MOUH_RIB)), erreur);
            assertFalse(erreur.contains("Mouh"), erreur);
            assertTrue(erreur.contains("incorrect"), erreur);
        }
    }
}
