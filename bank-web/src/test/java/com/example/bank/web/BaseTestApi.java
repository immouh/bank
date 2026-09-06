package com.example.bank.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Socle des tests d'intégration de l'API.
 *
 * CHAQUE CLASSE DE TEST A SA PROPRE BASE H2 EN MÉMOIRE — le nom est donné par
 * la propriété {@code bank.base.url} de la sous-classe. Deux valeurs
 * différentes donnent deux contextes Spring différents, donc deux bases
 * indépendantes : aucune classe ne travaille sur ce qu'une autre a laissé.
 *
 * LE COMPTEUR D'ÉCHECS EST REMIS À ZÉRO AVANT CHAQUE TEST. Sans ça, deux
 * tentatives ratées dans deux tests différents s'additionneraient et le
 * troisième test recevrait un 423 inattendu : les tests dépendraient de leur
 * ordre d'exécution.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseTestApi {

    protected static final String MOUH_MDP = "tata";
    protected static final int MOUH_RIB = 123;
    protected static final String AMINE_MDP = "matoub";
    protected static final int AMINE_RIB = 456;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected Connection connexion;

    @BeforeEach
    void effacerLesTentatives() throws SQLException {
        try (Statement statement = connexion.createStatement()) {
            statement.executeUpdate("DELETE FROM tentatives_connexion");
        }
    }

    /** Corps JSON d'un objet, pour le passer à MockMvc. */
    protected String corps(Object valeur) throws Exception {
        return json.writeValueAsString(valeur);
    }

    /** Requête POST avec un corps JSON. */
    protected MockHttpServletRequestBuilder postJson(String chemin, Object corps) throws Exception {
        return post(chemin).contentType(MediaType.APPLICATION_JSON).content(corps(corps));
    }

    /** Se connecte et rend le jeton — le point de départ de tout test protégé. */
    protected String jetonDe(int rib, String motDePasse) throws Exception {
        MvcResult resultat = mockMvc.perform(
                        postJson("/api/auth/login", new Connexion(rib, motDePasse)))
                .andExpect(status().isOk())
                .andReturn();
        return lire(resultat).get("token").asText();
    }

    /** En-tête d'autorisation prêt à poser sur une requête. */
    protected String bearer(int rib, String motDePasse) throws Exception {
        return "Bearer " + jetonDe(rib, motDePasse);
    }

    protected JsonNode lire(MvcResult resultat) throws Exception {
        return json.readTree(resultat.getResponse().getContentAsString());
    }

    /** Corps de connexion, écrit ici pour que les tests ne dépendent pas du DTO. */
    public record Connexion(Integer rib, String motDePasse) {
    }
}
