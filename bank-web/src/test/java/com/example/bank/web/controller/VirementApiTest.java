package com.example.bank.web.controller;

import com.example.bank.web.BaseTestApi;
import org.junit.jupiter.api.DisplayName;
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
 * Le virement, vu depuis HTTP.
 *
 * L'ATOMICITÉ N'EST PAS RETESTÉE ICI — elle appartient à
 * {@code sauvegarderEnsemble} et {@code JdbcClientRepositoryTest} la couvre
 * sur une vraie base. Ce qui se vérifie ici : que l'émetteur vient du jeton,
 * que les deux côtés bougent, et que chaque refus métier tombe sur le bon code.
 */
@TestPropertySource(properties = "bank.base.url=jdbc:h2:mem:test-virement;DB_CLOSE_DELAY=-1")
@DisplayName("API — virements")
class VirementApiTest extends BaseTestApi {

    private record Virement(Integer ribDestinataire, BigDecimal montant) {
    }

    private record Montant(BigDecimal montant) {
    }

    private BigDecimal solde(String autorisation) throws Exception {
        MvcResult resultat = mockMvc.perform(get("/api/comptes/moi")
                .header(HttpHeaders.AUTHORIZATION, autorisation)).andReturn();
        return new BigDecimal(lire(resultat).get("comptes").get(0).get("solde").asText());
    }

    @Test
    @DisplayName("Un virement débite l'émetteur et crédite le destinataire du même montant")
    void virementDeplaceLArgent() throws Exception {
        String jetonMouh = bearer(MOUH_RIB, MOUH_MDP);
        String jetonAmine = bearer(AMINE_RIB, AMINE_MDP);
        // De quoi virer sans dépendre du solde laissé par un autre test.
        mockMvc.perform(postJson("/api/comptes/depot", new Montant(new BigDecimal("200.00")))
                .header(HttpHeaders.AUTHORIZATION, jetonMouh));

        BigDecimal mouhAvant = solde(jetonMouh);
        BigDecimal amineAvant = solde(jetonAmine);

        mockMvc.perform(postJson("/api/virements", new Virement(AMINE_RIB, new BigDecimal("120.00")))
                        .header(HttpHeaders.AUTHORIZATION, jetonMouh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libelle").value(
                        org.hamcrest.Matchers.containsString("amine")));

        assertEquals(0, mouhAvant.subtract(new BigDecimal("120.00")).compareTo(solde(jetonMouh)));
        assertEquals(0, amineAvant.add(new BigDecimal("120.00")).compareTo(solde(jetonAmine)));
    }

    @Test
    @DisplayName("Un destinataire inconnu est refusé en 404")
    void destinataireInconnuRefuse() throws Exception {
        mockMvc.perform(postJson("/api/virements", new Virement(999, new BigDecimal("10.00")))
                        .header(HttpHeaders.AUTHORIZATION, bearer(MOUH_RIB, MOUH_MDP)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erreur").value(
                        org.hamcrest.Matchers.containsString("Destinataire")));
    }

    @Test
    @DisplayName("Un destinataire absent du corps est refusé en 404, pas en 500")
    void destinataireAbsentRefuse() throws Exception {
        mockMvc.perform(postJson("/api/virements", new Virement(null, new BigDecimal("10.00")))
                        .header(HttpHeaders.AUTHORIZATION, bearer(MOUH_RIB, MOUH_MDP)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un virement vers soi-même est refusé en 400")
    void virementVersSoiMemeRefuse() throws Exception {
        mockMvc.perform(postJson("/api/virements", new Virement(MOUH_RIB, new BigDecimal("10.00")))
                        .header(HttpHeaders.AUTHORIZATION, bearer(MOUH_RIB, MOUH_MDP)))
                .andExpect(status().isBadRequest());
    }

    /**
     * REPREND LE TODO #4 REFERMÉ CÔTÉ COEUR — le montant est validé avant la
     * règle « vers soi-même ». La couche web doit remonter le 400 du montant,
     * pas masquer l'ordre décidé par le service.
     */
    @Test
    @DisplayName("Vers soi-même ET avec un montant invalide, c'est le montant qui est signalé")
    void ordreDeValidationRespecte() throws Exception {
        MvcResult resultat = mockMvc.perform(
                        postJson("/api/virements", new Virement(MOUH_RIB, new BigDecimal("-10.00")))
                                .header(HttpHeaders.AUTHORIZATION, bearer(MOUH_RIB, MOUH_MDP)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertEquals(true, lire(resultat).get("erreur").asText().contains("montant"),
                lire(resultat).get("erreur").asText());
    }

    @Test
    @DisplayName("Un virement au-delà du solde est refusé en 409")
    void virementTropGrandRefuse() throws Exception {
        mockMvc.perform(postJson("/api/virements",
                        new Virement(AMINE_RIB, new BigDecimal("999999.00")))
                        .header(HttpHeaders.AUTHORIZATION, bearer(MOUH_RIB, MOUH_MDP)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Un virement refusé ne déplace aucun argent, des deux côtés")
    void virementRefuseNeDeplaceRien() throws Exception {
        String jetonMouh = bearer(MOUH_RIB, MOUH_MDP);
        String jetonAmine = bearer(AMINE_RIB, AMINE_MDP);
        BigDecimal mouhAvant = solde(jetonMouh);
        BigDecimal amineAvant = solde(jetonAmine);

        mockMvc.perform(postJson("/api/virements",
                        new Virement(AMINE_RIB, new BigDecimal("999999.00")))
                        .header(HttpHeaders.AUTHORIZATION, jetonMouh))
                .andExpect(status().isConflict());

        assertEquals(0, mouhAvant.compareTo(solde(jetonMouh)));
        assertEquals(0, amineAvant.compareTo(solde(jetonAmine)));
    }
}
