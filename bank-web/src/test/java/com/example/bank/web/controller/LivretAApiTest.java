package com.example.bank.web.controller;

import com.example.bank.web.BaseTestApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * L'ouverture du Livret A.
 *
 * Les données de démonstration donnent un Livret A à Mouh et pas à amine :
 * les deux cas — ouverture possible, ouverture déjà faite — sont donc
 * couverts sans avoir à préparer quoi que ce soit.
 */
@TestPropertySource(properties = "bank.base.url=jdbc:h2:mem:test-livret;DB_CLOSE_DELAY=-1")
@DisplayName("API — Livret A")
class LivretAApiTest extends BaseTestApi {

    @Test
    @DisplayName("Un client sans Livret A peut l'ouvrir : 201 et le livret apparaît")
    void ouvertureDuLivret() throws Exception {
        mockMvc.perform(postJson("/api/livret-a", "")
                        .header(HttpHeaders.AUTHORIZATION, bearer(AMINE_RIB, AMINE_MDP)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comptes[?(@.epargne == true)]").exists())
                .andExpect(jsonPath("$.comptes[?(@.type == 'LivretA')]").exists());
    }

    /**
     * 409 et non 404 : le Livret A existe EN TROP. Répondre « introuvable » à
     * une ressource déjà présente serait le contresens le plus visible du
     * tableau de correspondance.
     */
    @Test
    @DisplayName("Un client qui a déjà un Livret A reçoit un 409, pas un 404")
    void secondeOuvertureRefusee() throws Exception {
        mockMvc.perform(postJson("/api/livret-a", "")
                        .header(HttpHeaders.AUTHORIZATION, bearer(MOUH_RIB, MOUH_MDP)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erreur").isNotEmpty());
    }

    @Test
    @DisplayName("Le Livret A ouvert est rémunéré et rendu à zéro, sans toucher au compte courant")
    void livretOuvertAZero() throws Exception {
        String autorisation = bearer(AMINE_RIB, AMINE_MDP);
        mockMvc.perform(postJson("/api/livret-a", "")
                .header(HttpHeaders.AUTHORIZATION, autorisation));

        mockMvc.perform(get("/api/comptes/moi")
                        .header(HttpHeaders.AUTHORIZATION, autorisation))
                .andExpect(jsonPath("$.comptes[?(@.epargne == true)].solde").value(
                        org.hamcrest.Matchers.hasItem(0.00)));
    }
}
