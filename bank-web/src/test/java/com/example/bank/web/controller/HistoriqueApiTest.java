package com.example.bank.web.controller;

import com.example.bank.web.BaseTestApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * L'historique, vu depuis HTTP.
 *
 * CE QUI SE JOUE ICI — que les opérations passées par l'API laissent bien une
 * trace en base, et qu'elles s'AJOUTENT au passé au lieu de l'effacer. C'est
 * le point que {@code HistoriquePersistant} existe pour tenir : appeler
 * {@code enregistrerHistorique} depuis une requête web aurait remplacé tout
 * l'historique du client par la seule opération en cours.
 */
@TestPropertySource(properties = "bank.base.url=jdbc:h2:mem:test-historique;DB_CLOSE_DELAY=-1")
@DisplayName("API — historique")
class HistoriqueApiTest extends BaseTestApi {

    private record Montant(BigDecimal montant) {
    }

    private int nombreDeLignes(String autorisation) throws Exception {
        MvcResult resultat = mockMvc.perform(get("/api/historique")
                .header(HttpHeaders.AUTHORIZATION, autorisation)).andReturn();
        return lire(resultat).size();
    }

    @Test
    @DisplayName("L'historique du client est rendu avec ses lignes datées")
    void historiqueRendu() throws Exception {
        mockMvc.perform(get("/api/historique")
                        .header(HttpHeaders.AUTHORIZATION, bearer(MOUH_RIB, MOUH_MDP)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].type").isNotEmpty())
                .andExpect(jsonPath("$[0].montant").isNotEmpty())
                .andExpect(jsonPath("$[0].horodatage").isNotEmpty());
    }

    @Test
    @DisplayName("Un dépôt ajoute exactement une ligne à l'historique")
    void depotAjouteUneLigne() throws Exception {
        String autorisation = bearer(MOUH_RIB, MOUH_MDP);
        int avant = nombreDeLignes(autorisation);

        mockMvc.perform(postJson("/api/comptes/depot", new Montant(new BigDecimal("25.00")))
                        .header(HttpHeaders.AUTHORIZATION, autorisation))
                .andExpect(status().isOk());

        assertEquals(avant + 1, nombreDeLignes(autorisation));
    }

    /**
     * LE TEST QUI JUSTIFIE {@code HistoriquePersistant}. Avec
     * {@code enregistrerHistorique}, ce test verrait l'historique retomber à
     * une seule ligne après le premier dépôt.
     */
    @Test
    @DisplayName("Les opérations successives s'ajoutent, elles n'effacent pas le passé")
    void operationsSAjoutent() throws Exception {
        String autorisation = bearer(MOUH_RIB, MOUH_MDP);
        int avant = nombreDeLignes(autorisation);

        mockMvc.perform(postJson("/api/comptes/depot", new Montant(new BigDecimal("10.00")))
                .header(HttpHeaders.AUTHORIZATION, autorisation));
        mockMvc.perform(postJson("/api/comptes/depot", new Montant(new BigDecimal("20.00")))
                .header(HttpHeaders.AUTHORIZATION, autorisation));
        mockMvc.perform(postJson("/api/comptes/retrait", new Montant(new BigDecimal("5.00")))
                .header(HttpHeaders.AUTHORIZATION, autorisation));

        assertEquals(avant + 3, nombreDeLignes(autorisation));
    }

    @Test
    @DisplayName("Les lignes sont numérotées dans l'ordre d'exécution, sans trou ni doublon")
    void lignesNumeroteesDansLOrdre() throws Exception {
        String autorisation = bearer(MOUH_RIB, MOUH_MDP);
        mockMvc.perform(postJson("/api/comptes/depot", new Montant(new BigDecimal("30.00")))
                .header(HttpHeaders.AUTHORIZATION, autorisation));

        MvcResult resultat = mockMvc.perform(get("/api/historique")
                .header(HttpHeaders.AUTHORIZATION, autorisation)).andReturn();

        var lignes = lire(resultat);
        for (int i = 0; i < lignes.size(); i++) {
            assertEquals(i, lignes.get(i).get("numeroOrdre").asInt(),
                    "Numérotation cassée à l'indice " + i);
        }
    }

    @Test
    @DisplayName("Un virement laisse une trace chez l'émetteur ET chez le destinataire")
    void virementTraceDesDeuxCotes() throws Exception {
        String jetonMouh = bearer(MOUH_RIB, MOUH_MDP);
        String jetonAmine = bearer(AMINE_RIB, AMINE_MDP);
        mockMvc.perform(postJson("/api/comptes/depot", new Montant(new BigDecimal("100.00")))
                .header(HttpHeaders.AUTHORIZATION, jetonMouh));
        int mouhAvant = nombreDeLignes(jetonMouh);
        int amineAvant = nombreDeLignes(jetonAmine);

        mockMvc.perform(postJson("/api/virements",
                        new java.util.LinkedHashMap<>(java.util.Map.of(
                                "ribDestinataire", AMINE_RIB, "montant", new BigDecimal("40.00"))))
                        .header(HttpHeaders.AUTHORIZATION, jetonMouh))
                .andExpect(status().isOk());

        assertEquals(mouhAvant + 1, nombreDeLignes(jetonMouh));
        assertEquals(amineAvant + 1, nombreDeLignes(jetonAmine));
    }

    @Test
    @DisplayName("Chaque client ne lit que son propre historique")
    void historiqueIsole() throws Exception {
        String jetonMouh = bearer(MOUH_RIB, MOUH_MDP);
        mockMvc.perform(postJson("/api/comptes/depot", new Montant(new BigDecimal("77.00")))
                .header(HttpHeaders.AUTHORIZATION, jetonMouh));

        MvcResult amine = mockMvc.perform(get("/api/historique")
                .header(HttpHeaders.AUTHORIZATION, bearer(AMINE_RIB, AMINE_MDP))).andReturn();

        assertTrue(amine.getResponse().getContentAsString().indexOf("77.00") < 0,
                "L'historique d'amine contient une opération de Mouh.");
    }
}
