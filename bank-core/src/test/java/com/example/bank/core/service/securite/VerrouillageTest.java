package com.example.bank.core.service.securite;

import com.example.bank.core.exception.existence.ClientIntrouvableException;
import com.example.bank.core.exception.etat.CompteVerouilleException;
import com.example.bank.core.model.Client;
import com.example.bank.core.repository.ClientRepository;
import com.example.bank.core.repository.InMemoryClientRepository;
import com.example.bank.core.service.AuthService;
import com.example.bank.core.service.audit.EvenementAudit;
import com.example.bank.core.service.audit.JournalAuditEnMemoire;
import com.example.bank.core.service.audit.LigneAudit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verrouillage après trois tentatives, vu depuis {@code AuthService}.
 *
 * L'HORLOGE EST INJECTÉE — tester l'expiration du verrouillage sans ça
 * demanderait d'attendre quinze minutes pour de vrai.
 */
@DisplayName("Verrouillage après tentatives échouées")
class VerrouillageTest {

    private static final int RIB_MOUH = 123;
    private static final String NOM = "Mouh";
    private static final String BON = "tata";
    private static final String MAUVAIS = "mauvais";

    private Instant maintenant;
    private RegistreTentativesEnMemoire registre;
    private JournalAuditEnMemoire audit;
    private AuthService authService;

    @BeforeEach
    void preparer() {
        maintenant = Instant.parse("2026-09-02T10:00:00Z");
        Clock horloge = new HorlogeReglable();
        registre = new RegistreTentativesEnMemoire(horloge);
        audit = new JournalAuditEnMemoire(horloge);

        ClientRepository repository = new InMemoryClientRepository();
        authService = new AuthService(repository,
                new BCryptHachageStrategy(4),
                new RepertoireMotsDePasseEnMemoire(),
                registre,
                audit);
    }

    /** Horloge qu'on avance à la main, pour franchir la fenêtre de verrouillage. */
    private class HorlogeReglable extends Clock {
        @Override public ZoneId getZone() { return ZoneId.systemDefault(); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return maintenant; }
    }

    private void echouer(int fois) {
        for (int i = 0; i < fois; i++) {
            assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier(NOM, MAUVAIS));
        }
    }

    @Nested
    @DisplayName("Comptage des échecs")
    class Comptage {

        @Test
        @DisplayName("Un compte neuf n'a aucun échec au compteur")
        void compteurNeuf() {
            assertEquals(0, registre.echecs(RIB_MOUH));
            assertFalse(registre.estVerrouille(RIB_MOUH));
        }

        @Test
        @DisplayName("Deux échecs ne verrouillent pas encore")
        void deuxEchecsNeVerrouillentPas() {
            echouer(2);

            assertEquals(2, registre.echecs(RIB_MOUH));
            assertFalse(registre.estVerrouille(RIB_MOUH));
        }

        @Test
        @DisplayName("Trois échecs consécutifs verrouillent le compte")
        void troisEchecsVerrouillent() {
            echouer(3);

            assertTrue(registre.estVerrouille(RIB_MOUH));
        }

        @Test
        @DisplayName("Une connexion réussie remet le compteur à zéro")
        void succesReinitialiseLeCompteur() {
            echouer(2);

            authService.authentifier(NOM, BON);

            assertEquals(0, registre.echecs(RIB_MOUH));
        }

        @Test
        @DisplayName("Un succès entre deux séries d'échecs empêche le verrouillage : les échecs doivent être consécutifs")
        void echecsNonConsecutifsNeVerrouillentPas() {
            echouer(2);
            authService.authentifier(NOM, BON);
            echouer(2);

            assertFalse(registre.estVerrouille(RIB_MOUH));
        }

        @Test
        @DisplayName("Un nom d'utilisateur inconnu ne fait grimper aucun compteur")
        void nomInconnuNeCompteRien() {
            assertThrows(ClientIntrouvableException.class,
                    () -> authService.authentifier("personne", MAUVAIS));

            assertEquals(0, registre.echecs(RIB_MOUH));
        }
    }

    @Nested
    @DisplayName("Compte verrouillé")
    class Verrouille {

        @BeforeEach
        void verrouiller() {
            echouer(3);
        }

        @Test
        @DisplayName("Le bon mot de passe est refusé lui aussi : c'est tout l'intérêt du verrouillage")
        void bonMotDePasseRefuseAussi() {
            assertThrows(CompteVerouilleException.class,
                    () -> authService.authentifier(NOM, BON));
        }

        @Test
        @DisplayName("Le refus indique le délai d'attente plutôt que le message de non-divulgation")
        void messageDeVerrouillage() {
            CompteVerouilleException levee = assertThrows(CompteVerouilleException.class,
                    () -> authService.authentifier(NOM, BON));

            assertTrue(levee.getMessage().contains("verrouillé"), levee.getMessage());
        }

        @Test
        @DisplayName("Réessayer pendant le verrouillage ne repousse pas l'échéance")
        void reessayerNeProlongePasLeVerrouillage() {
            assertThrows(CompteVerouilleException.class,
                    () -> authService.authentifier(NOM, BON));
            assertThrows(CompteVerouilleException.class,
                    () -> authService.authentifier(NOM, MAUVAIS));

            assertEquals(3, registre.echecs(RIB_MOUH),
                    "Un refus pour verrouillage ne doit pas compter comme un échec de plus.");
        }

        @Test
        @DisplayName("Le verrouillage tient encore à quatorze minutes")
        void tientAQuatorzeMinutes() {
            maintenant = maintenant.plus(Duration.ofMinutes(14));

            assertTrue(registre.estVerrouille(RIB_MOUH));
        }

        @Test
        @DisplayName("Le verrouillage tombe de lui-même après quinze minutes")
        void expireApresQuinzeMinutes() {
            maintenant = maintenant.plus(Duration.ofMinutes(15));

            assertFalse(registre.estVerrouille(RIB_MOUH));
        }

        @Test
        @DisplayName("Une fois le délai passé, le bon mot de passe rouvre le compte")
        void connexionPossibleApresExpiration() {
            maintenant = maintenant.plus(Duration.ofMinutes(16));

            assertEquals(RIB_MOUH, authService.authentifier(NOM, BON).getRib());
        }

        @Test
        @DisplayName("L'expiration efface aussi le compteur d'échecs")
        void expirationEffaceLeCompteur() {
            maintenant = maintenant.plus(Duration.ofMinutes(16));
            registre.estVerrouille(RIB_MOUH);

            assertEquals(0, registre.echecs(RIB_MOUH));
        }
    }

    @Nested
    @DisplayName("Traces laissées au journal")
    class Traces {

        @Test
        @DisplayName("Une connexion réussie est consignée")
        void connexionReussieConsignee() {
            authService.authentifier(NOM, BON);

            assertEquals(List.of(EvenementAudit.CONNEXION_REUSSIE), evenements());
        }

        @Test
        @DisplayName("Chaque échec est consigné")
        void echecsConsignes() {
            echouer(2);

            assertEquals(List.of(EvenementAudit.CONNEXION_ECHOUEE, EvenementAudit.CONNEXION_ECHOUEE),
                    evenements());
        }

        @Test
        @DisplayName("Le verrouillage produit sa propre ligne, en plus du troisième échec")
        void verrouillageConsigne() {
            echouer(3);

            assertTrue(evenements().contains(EvenementAudit.COMPTE_VEROUILLE));
            assertEquals(EvenementAudit.COMPTE_VEROUILLE, evenements().get(evenements().size() - 1));
        }

        @Test
        @DisplayName("Le journal ne contient jamais le mot de passe saisi")
        void journalSansMotDePasse() {
            echouer(2);
            authService.authentifier(NOM, BON);
            echouer(3);

            for (LigneAudit ligne : audit.tout()) {
                String detail = ligne.detail() == null ? "" : ligne.detail();
                assertFalse(detail.contains(MAUVAIS), detail);
                assertFalse(detail.contains(BON), detail);
            }
        }

        private List<EvenementAudit> evenements() {
            return audit.pour(RIB_MOUH).stream().map(LigneAudit::evenement).toList();
        }
    }
}
