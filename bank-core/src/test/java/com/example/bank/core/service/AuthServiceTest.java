package com.example.bank.core.service;

import com.example.bank.core.exception.BanqueException;
import com.example.bank.core.exception.existence.ClientIntrouvableException;
import com.example.bank.core.model.Client;
import com.example.bank.core.repository.ClientRepository;
import com.example.bank.core.repository.InMemoryClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * L'authentification.
 *
 * CE QUI COMPTE LE PLUS ICI — qu'un échec soit INDISTINGUABLE entre « ce nom
 * n'existe pas » et « ce mot de passe est faux ». Une différence, même dans le
 * type d'exception ou la ponctuation du message, transformerait la fenêtre de
 * connexion en énumérateur de comptes existants.
 */
@DisplayName("Service d'authentification")
class AuthServiceTest {

    /** Identifiants de démonstration chargés par le repository en mémoire. */
    private static final String NOM_CONNU = "Mouh";
    private static final String MOT_DE_PASSE_CORRECT = "tata";
    private static final String MOT_DE_PASSE_FAUX = "mauvais";
    private static final String NOM_INCONNU = "personne";

    private AuthService authService;

    @BeforeEach
    void preparerLAuthentification() {
        ClientRepository repository = new InMemoryClientRepository();
        authService = new AuthService(repository);
    }

    @Nested
    @DisplayName("Connexion réussie")
    class Succes {

        @Test
        @DisplayName("Des identifiants corrects rendent le client correspondant")
        void identifiantsCorrectsRendentLeClient() {
            Client connecte = authService.authentifier(NOM_CONNU, MOT_DE_PASSE_CORRECT);

            assertEquals(NOM_CONNU, connecte.getNom());
            assertEquals(123, connecte.getRib());
        }

        @Test
        @DisplayName("Chaque client se connecte avec ses propres identifiants")
        void chaqueClientSeConnecteAvecLesSiens() {
            assertEquals(456, authService.authentifier("amine", "matoub").getRib());
        }

        @Test
        @DisplayName("Le mot de passe d'un autre client ne donne pas accès au compte")
        void motDePasseDUnAutreClientRefuse() {
            assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier(NOM_CONNU, "matoub"));
        }
    }

    @Nested
    @DisplayName("Connexion refusée")
    class Echec {

        @Test
        @DisplayName("Un nom d'utilisateur inconnu est refusé")
        void nomInconnuRefuse() {
            assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier(NOM_INCONNU, MOT_DE_PASSE_CORRECT));
        }

        @Test
        @DisplayName("Un mot de passe faux sur un nom connu est refusé")
        void motDePasseFauxRefuse() {
            assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier(NOM_CONNU, MOT_DE_PASSE_FAUX));
        }

        @Test
        @DisplayName("La casse du nom compte : « mouh » n'est pas « Mouh »")
        void casseDuNomSignificative() {
            assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier("mouh", MOT_DE_PASSE_CORRECT));
        }

        @Test
        @DisplayName("Un mot de passe vide est refusé")
        void motDePasseVideRefuse() {
            assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier(NOM_CONNU, ""));
        }

        @Test
        @DisplayName("Un mot de passe absent est refusé sans erreur technique")
        void motDePasseNulRefuse() {
            assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier(NOM_CONNU, null));
        }

        @Test
        @DisplayName("Un nom absent est refusé sans erreur technique")
        void nomNulRefuse() {
            assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier(null, MOT_DE_PASSE_CORRECT));
        }

        @Test
        @DisplayName("L'échec est une exception métier, que l'IHM sait déjà attraper")
        void echecEstUneExceptionMetier() {
            BanqueException levee = assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier(NOM_INCONNU, MOT_DE_PASSE_CORRECT));

            assertInstanceOf(BanqueException.class, levee);
        }
    }

    @Nested
    @DisplayName("Non-divulgation des comptes existants")
    class NonDivulgation {

        @Test
        @DisplayName("Nom inconnu et mot de passe faux donnent EXACTEMENT le même message")
        void memeMessageDansLesDeuxCas() {
            ClientIntrouvableException nomInconnu = assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier(NOM_INCONNU, MOT_DE_PASSE_CORRECT));
            ClientIntrouvableException motDePasseFaux = assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier(NOM_CONNU, MOT_DE_PASSE_FAUX));

            assertEquals(nomInconnu.getMessage(), motDePasseFaux.getMessage());
        }

        @Test
        @DisplayName("Nom inconnu et mot de passe faux lèvent EXACTEMENT le même type d'exception")
        void memeTypeDExceptionDansLesDeuxCas() {
            ClientIntrouvableException nomInconnu = assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier(NOM_INCONNU, MOT_DE_PASSE_CORRECT));
            ClientIntrouvableException motDePasseFaux = assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier(NOM_CONNU, MOT_DE_PASSE_FAUX));

            assertEquals(nomInconnu.getClass(), motDePasseFaux.getClass());
        }

        @Test
        @DisplayName("Le message d'échec ne nomme pas le compte cherché : il ne dit pas s'il existe")
        void messageNeNommePasLeCompte() {
            ClientIntrouvableException levee = assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier(NOM_CONNU, MOT_DE_PASSE_FAUX));

            assertFalse(levee.getMessage().contains(NOM_CONNU), levee.getMessage());
        }

        @Test
        @DisplayName("Le message d'échec ne contient pas le mot de passe saisi")
        void messageNeContientPasLeMotDePasseSaisi() {
            ClientIntrouvableException levee = assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier(NOM_CONNU, MOT_DE_PASSE_FAUX));

            assertFalse(levee.getMessage().contains(MOT_DE_PASSE_FAUX), levee.getMessage());
        }

        @Test
        @DisplayName("Le message d'échec ne contient pas le mot de passe stocké")
        void messageNeContientPasLeMotDePasseStocke() {
            ClientIntrouvableException levee = assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier(NOM_CONNU, MOT_DE_PASSE_FAUX));

            assertFalse(levee.getMessage().contains(MOT_DE_PASSE_CORRECT), levee.getMessage());
        }

        @Test
        @DisplayName("Aucun mot de passe ne fuit dans la trace d'exception complète")
        void aucuneFuiteDansLaTraceComplete() {
            ClientIntrouvableException levee = assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier(NOM_CONNU, MOT_DE_PASSE_FAUX));

            String trace = levee.toString();
            assertFalse(trace.contains(MOT_DE_PASSE_CORRECT), trace);
            assertFalse(trace.contains(MOT_DE_PASSE_FAUX), trace);
        }

        @Test
        @DisplayName("L'échec ne porte aucune cause chaînée susceptible de fuiter du contexte")
        void aucuneCauseChainee() {
            ClientIntrouvableException levee = assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier(NOM_CONNU, MOT_DE_PASSE_FAUX));

            assertEquals(null, levee.getCause());
        }
    }
}
