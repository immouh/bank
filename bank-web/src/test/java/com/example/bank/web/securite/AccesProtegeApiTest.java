package com.example.bank.web.securite;

import com.example.bank.web.BaseTestApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Qui a le droit d'entrer, et jusqu'où.
 *
 * DEUX PROPRIÉTÉS Y SONT VÉRIFIÉES, et ce sont les deux qui comptent pour une
 * API : sans jeton valide on n'entre pas, et AVEC un jeton valide on n'atteint
 * que ses propres données. La seconde est la plus facile à rater : l'appelant
 * est authentifié, donc aucune vérification de mot de passe ne le protégera
 * d'un identifiant falsifié dans l'URL.
 */
@TestPropertySource(properties = "bank.base.url=jdbc:h2:mem:test-acces;DB_CLOSE_DELAY=-1")
@DisplayName("API — accès protégé et isolation entre clients")
class AccesProtegeApiTest extends BaseTestApi {

    private record Montant(BigDecimal montant) {
    }

    private record Virement(Integer ribDestinataire, BigDecimal montant) {
    }

    @Nested
    @DisplayName("Sans jeton valide, rien ne passe")
    class SansJeton {

        @Test
        @DisplayName("Une route protégée sans jeton est refusée en 401")
        void sansJetonRefuse() throws Exception {
            mockMvc.perform(get("/api/comptes/moi"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.erreur").isNotEmpty());
        }

        @Test
        @DisplayName("Un jeton qui n'est pas un JWT est refusé en 401")
        void jetonInvalideRefuse() throws Exception {
            mockMvc.perform(get("/api/comptes/moi")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer ceci-nest-pas-un-jeton"))
                    .andExpect(status().isUnauthorized());
        }

        /**
         * Le jeton est bien formé et non expiré, mais signé avec une AUTRE
         * clé : c'est la tentative de forge, et c'est la signature — pas la
         * date — qui doit l'arrêter.
         */
        @Test
        @DisplayName("Un jeton signé avec une autre clé est refusé en 401")
        void jetonMalSigneRefuse() throws Exception {
            String forge = new JwtService("une-tout-autre-cle-de-32-caracteres-au-moins")
                    .genererPour(MOUH_RIB, "Mouh");

            mockMvc.perform(get("/api/comptes/moi")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + forge))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Un en-tête Authorization sans le préfixe Bearer est ignoré : 401")
        void sansPrefixeBearerRefuse() throws Exception {
            mockMvc.perform(get("/api/comptes/moi")
                            .header(HttpHeaders.AUTHORIZATION, jetonDe(MOUH_RIB, MOUH_MDP)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Toutes les routes métier sont protégées, pas seulement la consultation")
        void toutesLesRoutesProtegees() throws Exception {
            mockMvc.perform(get("/api/historique")).andExpect(status().isUnauthorized());
            mockMvc.perform(postJson("/api/comptes/depot", new Montant(BigDecimal.ONE)))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(postJson("/api/comptes/retrait", new Montant(BigDecimal.ONE)))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(postJson("/api/virements", new Virement(AMINE_RIB, BigDecimal.ONE)))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(postJson("/api/livret-a", ""))
                    .andExpect(status().isUnauthorized());
        }
    }

    /**
     * LE TEST QUE CE MODULE EXISTE POUR NE PAS RATER.
     *
     * Aucun endpoint ne prend de RIB : le client concerné vient toujours du
     * jeton. Ces tests vérifient que c'est bien le cas, y compris quand
     * l'appelant essaie d'en imposer un.
     */
    @Nested
    @DisplayName("Un client ne voit que ses propres données")
    class Isolation {

        @Test
        @DisplayName("Le jeton de Mouh rend les comptes de Mouh, jamais ceux d'amine")
        void chacunVoitLesSiens() throws Exception {
            mockMvc.perform(get("/api/comptes/moi")
                            .header(HttpHeaders.AUTHORIZATION, bearer(MOUH_RIB, MOUH_MDP)))
                    .andExpect(jsonPath("$.rib").value(MOUH_RIB));

            mockMvc.perform(get("/api/comptes/moi")
                            .header(HttpHeaders.AUTHORIZATION, bearer(AMINE_RIB, AMINE_MDP)))
                    .andExpect(jsonPath("$.rib").value(AMINE_RIB));
        }

        @Test
        @DisplayName("Un RIB ajouté en paramètre de requête est ignoré : c'est le jeton qui décide")
        void parametreRibIgnore() throws Exception {
            mockMvc.perform(get("/api/comptes/moi")
                            .param("rib", String.valueOf(AMINE_RIB))
                            .header(HttpHeaders.AUTHORIZATION, bearer(MOUH_RIB, MOUH_MDP)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rib").value(MOUH_RIB))
                    .andExpect(jsonPath("$.nom").value("Mouh"));
        }

        @Test
        @DisplayName("Aucune route ne permet de désigner le compte d'un autre client")
        void aucuneRouteParRib() throws Exception {
            String autorisation = bearer(MOUH_RIB, MOUH_MDP);

            // Ces chemins n'existent pas, volontairement : les exposer serait
            // exactement la faille que l'absence de paramètre RIB évite.
            mockMvc.perform(get("/api/comptes/" + AMINE_RIB)
                            .header(HttpHeaders.AUTHORIZATION, autorisation))
                    .andExpect(status().isNotFound());
            mockMvc.perform(get("/api/historique/" + AMINE_RIB)
                            .header(HttpHeaders.AUTHORIZATION, autorisation))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Un dépôt crédite le porteur du jeton, même si un RIB est fourni en paramètre")
        void depotCrediteLePorteurDuJeton() throws Exception {
            String jetonAmine = bearer(AMINE_RIB, AMINE_MDP);
            String jetonMouh = bearer(MOUH_RIB, MOUH_MDP);
            BigDecimal soldeMouhAvant = soldeDe(jetonMouh);

            mockMvc.perform(postJson("/api/comptes/depot", new Montant(new BigDecimal("50.00")))
                            .param("rib", String.valueOf(MOUH_RIB))
                            .header(HttpHeaders.AUTHORIZATION, jetonAmine))
                    .andExpect(status().isOk());

            assertEquals(0, soldeMouhAvant.compareTo(soldeDe(jetonMouh)),
                    "Le dépôt d'amine a atterri sur le compte de Mouh.");
        }

        private BigDecimal soldeDe(String autorisation) throws Exception {
            MvcResult resultat = mockMvc.perform(get("/api/comptes/moi")
                    .header(HttpHeaders.AUTHORIZATION, autorisation)).andReturn();
            return new BigDecimal(lire(resultat).get("comptes").get(0).get("solde").asText());
        }
    }
}
