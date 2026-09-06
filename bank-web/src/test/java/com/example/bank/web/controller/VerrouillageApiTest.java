package com.example.bank.web.controller;

import com.example.bank.web.BaseTestApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le verrouillage après trois échecs, vu depuis HTTP.
 *
 * LA MÉCANIQUE N'EST PAS RETESTÉE ICI — seuil, délai et remise à zéro
 * appartiennent au coeur, que {@code VerrouillageTest} couvre déjà. Ce qui se
 * vérifie ici, c'est que la couche web LAISSE PASSER cette protection au lieu
 * de la court-circuiter, et qu'elle la traduit dans un code HTTP qui a du sens.
 *
 * CLASSE SÉPARÉE, avec sa propre base : verrouiller un compte est justement
 * ce que les autres classes de test doivent éviter.
 */
@TestPropertySource(properties = "bank.base.url=jdbc:h2:mem:test-verrouillage;DB_CLOSE_DELAY=-1")
@DisplayName("API — verrouillage du compte")
class VerrouillageApiTest extends BaseTestApi {

    private void echouer(int fois) throws Exception {
        for (int i = 0; i < fois; i++) {
            mockMvc.perform(postJson("/api/auth/login", new Connexion(MOUH_RIB, "mauvais")))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("Deux échecs ne verrouillent pas encore : le bon mot de passe passe toujours")
    void deuxEchecsNeVerrouillentPas() throws Exception {
        echouer(2);

        mockMvc.perform(postJson("/api/auth/login", new Connexion(MOUH_RIB, MOUH_MDP)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Trois échecs verrouillent le compte : le bon mot de passe est refusé en 423")
    void troisEchecsVerrouillent() throws Exception {
        echouer(3);

        mockMvc.perform(postJson("/api/auth/login", new Connexion(MOUH_RIB, MOUH_MDP)))
                .andExpect(status().isLocked());
    }

    @Test
    @DisplayName("Le refus pour verrouillage annonce le délai d'attente")
    void refusAnnonceLeDelai() throws Exception {
        echouer(3);

        mockMvc.perform(postJson("/api/auth/login", new Connexion(MOUH_RIB, MOUH_MDP)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.erreur").value(
                        org.hamcrest.Matchers.containsString("verrouillé")));
    }

    @Test
    @DisplayName("Le verrouillage d'un client n'empêche pas un autre de se connecter")
    void verrouillageIsole() throws Exception {
        echouer(3);

        mockMvc.perform(postJson("/api/auth/login", new Connexion(AMINE_RIB, AMINE_MDP)))
                .andExpect(status().isOk());
    }

    /**
     * Un compte verrouillé ne délivre plus de jeton — mais un jeton DÉJÀ
     * délivré reste valable jusqu'à son expiration, l'API étant stateless.
     * Ce test fige ce comportement pour qu'il soit un choix, pas une surprise
     * (voir PHASE_G_EXPLICATIONS.md, « ce qui a été laissé de côté »).
     */
    @Test
    @DisplayName("Un jeton obtenu avant le verrouillage reste utilisable : il n'y a pas de révocation")
    void jetonAnterieurResteValable() throws Exception {
        String autorisation = bearer(MOUH_RIB, MOUH_MDP);

        echouer(3);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/comptes/moi")
                        .header("Authorization", autorisation))
                .andExpect(status().isOk());
    }
}
