package com.example.bank.web.controller;

import com.example.bank.web.BaseTestApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Consultation et opérations sur le compte du client authentifié.
 *
 * LES SOLDES SONT VÉRIFIÉS EN DELTA, jamais en valeur absolue : les tests
 * d'une même classe partagent une base, et exiger « 500,00 € au départ »
 * rendrait chaque test dépendant de ceux qui l'ont précédé.
 */
@TestPropertySource(properties = "bank.base.url=jdbc:h2:mem:test-compte;DB_CLOSE_DELAY=-1")
@DisplayName("API — comptes")
class CompteApiTest extends BaseTestApi {

    private record Montant(BigDecimal montant) {
    }

    private BigDecimal soldeCourant(String autorisation) throws Exception {
        MvcResult resultat = mockMvc.perform(get("/api/comptes/moi")
                .header(HttpHeaders.AUTHORIZATION, autorisation)).andReturn();
        return new BigDecimal(lire(resultat).get("comptes").get(0).get("solde").asText());
    }

    @Nested
    @DisplayName("Consultation")
    class Consultation {

        @Test
        @DisplayName("Un client authentifié lit sa situation : son identité et ses comptes")
        void situationDuClient() throws Exception {
            mockMvc.perform(get("/api/comptes/moi")
                            .header(HttpHeaders.AUTHORIZATION, bearer(MOUH_RIB, MOUH_MDP)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rib").value(MOUH_RIB))
                    .andExpect(jsonPath("$.nom").value("Mouh"))
                    .andExpect(jsonPath("$.comptes").isArray())
                    .andExpect(jsonPath("$.soldeTotal").isNotEmpty());
        }

        @Test
        @DisplayName("Chaque compte annonce son type, son état et son découvert autorisé")
        void detailDesComptes() throws Exception {
            mockMvc.perform(get("/api/comptes/moi")
                            .header(HttpHeaders.AUTHORIZATION, bearer(MOUH_RIB, MOUH_MDP)))
                    .andExpect(jsonPath("$.comptes[0].type").value("CompteStandard"))
                    .andExpect(jsonPath("$.comptes[0].epargne").value(false))
                    .andExpect(jsonPath("$.comptes[0].etat").isNotEmpty())
                    .andExpect(jsonPath("$.comptes[0].decouvertAutorise").value(300.00));
        }

        /**
         * Le DTO est construit champ par champ, mais rien ne remplace une
         * vérification sur le JSON RÉELLEMENT émis : c'est lui qui part sur le
         * réseau.
         */
        @Test
        @DisplayName("La situation ne laisse fuir ni mot de passe ni haché")
        void situationSansMotDePasse() throws Exception {
            MvcResult resultat = mockMvc.perform(get("/api/comptes/moi")
                    .header(HttpHeaders.AUTHORIZATION, bearer(MOUH_RIB, MOUH_MDP))).andReturn();

            String corps = resultat.getResponse().getContentAsString();
            assertFalse(corps.contains(MOUH_MDP), corps);
            assertFalse(corps.contains("$2a$"), "Un haché BCrypt a fuité : " + corps);
            assertFalse(corps.toLowerCase().contains("motdepasse"), corps);
        }
    }

    @Nested
    @DisplayName("Dépôt et retrait")
    class Operations {

        @Test
        @DisplayName("Un dépôt augmente le solde du montant exact")
        void depotAugmenteLeSolde() throws Exception {
            String autorisation = bearer(MOUH_RIB, MOUH_MDP);
            BigDecimal avant = soldeCourant(autorisation);

            mockMvc.perform(postJson("/api/comptes/depot", new Montant(new BigDecimal("150.00")))
                            .header(HttpHeaders.AUTHORIZATION, autorisation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.libelle").value(
                            org.hamcrest.Matchers.startsWith("Dépôt")));

            assertEquals(0, avant.add(new BigDecimal("150.00")).compareTo(soldeCourant(autorisation)));
        }

        @Test
        @DisplayName("Un retrait diminue le solde du montant exact")
        void retraitDiminueLeSolde() throws Exception {
            String autorisation = bearer(MOUH_RIB, MOUH_MDP);
            mockMvc.perform(postJson("/api/comptes/depot", new Montant(new BigDecimal("400.00")))
                    .header(HttpHeaders.AUTHORIZATION, autorisation));
            BigDecimal avant = soldeCourant(autorisation);

            mockMvc.perform(postJson("/api/comptes/retrait", new Montant(new BigDecimal("100.00")))
                            .header(HttpHeaders.AUTHORIZATION, autorisation))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.libelle").value(
                            org.hamcrest.Matchers.startsWith("Retrait")));

            assertEquals(0, avant.subtract(new BigDecimal("100.00"))
                    .compareTo(soldeCourant(autorisation)));
        }

        @Test
        @DisplayName("Le solde rendu par l'opération est celui d'après l'opération")
        void operationRendLeNouveauSolde() throws Exception {
            String autorisation = bearer(MOUH_RIB, MOUH_MDP);
            BigDecimal avant = soldeCourant(autorisation);

            MvcResult resultat = mockMvc.perform(
                            postJson("/api/comptes/depot", new Montant(new BigDecimal("10.00")))
                                    .header(HttpHeaders.AUTHORIZATION, autorisation))
                    .andReturn();

            assertEquals(0, avant.add(new BigDecimal("10.00")).compareTo(
                    new BigDecimal(lire(resultat).get("soldeCompte").asText())));
        }
    }

    @Nested
    @DisplayName("Refus métier traduits en HTTP")
    class Refus {

        @Test
        @DisplayName("Un montant négatif est refusé en 400")
        void montantNegatifRefuse() throws Exception {
            mockMvc.perform(postJson("/api/comptes/depot", new Montant(new BigDecimal("-10.00")))
                            .header(HttpHeaders.AUTHORIZATION, bearer(MOUH_RIB, MOUH_MDP)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erreur").isNotEmpty());
        }

        @Test
        @DisplayName("Un montant nul est refusé en 400")
        void montantNulRefuse() throws Exception {
            mockMvc.perform(postJson("/api/comptes/depot", new Montant(BigDecimal.ZERO))
                            .header(HttpHeaders.AUTHORIZATION, bearer(MOUH_RIB, MOUH_MDP)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Un montant absent est refusé en 400, pas en 500")
        void montantAbsentRefuse() throws Exception {
            mockMvc.perform(postJson("/api/comptes/depot", new Montant(null))
                            .header(HttpHeaders.AUTHORIZATION, bearer(MOUH_RIB, MOUH_MDP)))
                    .andExpect(status().isBadRequest());
        }

        /**
         * 409 et non 400 : la requête est bien formée, c'est l'état du compte
         * qui la refuse. La même requête passerait sur un compte approvisionné.
         */
        @Test
        @DisplayName("Un retrait au-delà du solde et du découvert est refusé en 409")
        void retraitTropGrandRefuse() throws Exception {
            mockMvc.perform(postJson("/api/comptes/retrait", new Montant(new BigDecimal("999999.00")))
                            .header(HttpHeaders.AUTHORIZATION, bearer(MOUH_RIB, MOUH_MDP)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.erreur").value(
                            org.hamcrest.Matchers.containsString("insuffisant")));
        }

        @Test
        @DisplayName("Un retrait refusé ne modifie pas le solde")
        void retraitRefuseLaisseLeSoldeIntact() throws Exception {
            String autorisation = bearer(MOUH_RIB, MOUH_MDP);
            BigDecimal avant = soldeCourant(autorisation);

            mockMvc.perform(postJson("/api/comptes/retrait", new Montant(new BigDecimal("999999.00")))
                            .header(HttpHeaders.AUTHORIZATION, autorisation))
                    .andExpect(status().isConflict());

            assertEquals(0, avant.compareTo(soldeCourant(autorisation)));
        }
    }
}
