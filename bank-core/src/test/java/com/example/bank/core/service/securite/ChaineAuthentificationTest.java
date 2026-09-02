package com.example.bank.core.service.securite;

import com.example.bank.core.exception.existence.ClientIntrouvableException;
import com.example.bank.core.exception.etat.CompteVerouilleException;
import com.example.bank.core.model.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La chaîne de contrôles de connexion, étape par étape puis en entier.
 *
 * Chaque étape est montée SEULE dans les blocs qui la concernent : c'est
 * précisément ce que le patron rend possible, et ce qu'une cascade de
 * {@code if} dans le service ne permettait pas.
 */
@DisplayName("Chaîne d'authentification")
class ChaineAuthentificationTest {

    private static final int RIB = 123;

    private HachageStrategy hachage;
    private RepertoireMotsDePasse repertoire;
    private RegistreTentativesEnMemoire registre;
    private Client client;

    @BeforeEach
    void preparer() {
        hachage = new BCryptHachageStrategy(4);
        repertoire = new RepertoireMotsDePasseEnMemoire();
        registre = new RegistreTentativesEnMemoire();
        client = new Client("Mouh", "peu importe", RIB);
        repertoire.enregistrer(RIB, hachage.hacher("tata"));
    }

    @Nested
    @DisplayName("Étape 1 — existence du client")
    class Existence {

        private final EtapeAuthentification etape =
                new VerificationClientExiste(new BCryptHachageStrategy(4));

        @Test
        @DisplayName("Un client connu passe l'étape")
        void clientConnuPasse() {
            assertDoesNotThrow(() -> etape.verifier(client, "tata"));
        }

        @Test
        @DisplayName("Un client inconnu est refusé par le message de non-divulgation")
        void clientInconnuRefuse() {
            ClientIntrouvableException levee = assertThrows(ClientIntrouvableException.class,
                    () -> etape.verifier(null, "tata"));

            assertEquals("Nom d'utilisateur ou mot de passe incorrect.", levee.getMessage());
        }

        @Test
        @DisplayName("Un client inconnu avec une saisie absente est refusé sans erreur technique")
        void saisieAbsenteSurClientInconnu() {
            assertThrows(ClientIntrouvableException.class, () -> etape.verifier(null, null));
        }

        /**
         * DÉFENSE ANTI-CHRONOMÈTRE — un nom inconnu doit coûter le même temps
         * qu'un mot de passe faux, sinon le chronomètre suffit à énumérer les
         * comptes existants et le message unique ne protège plus rien.
         *
         * Le temps lui-même ne se teste pas de façon fiable (machine chargée,
         * ramasse-miettes, JIT). On vérifie donc la CAUSE : que l'étape
         * effectue bien une vérification de hachage avant de refuser.
         */
        @Test
        @DisplayName("Un client inconnu déclenche quand même une vérification de hachage, pour ne pas répondre plus vite")
        void clientInconnuPaieLeMemeTempsDeCalcul() {
            HachageEspion espion = new HachageEspion();
            EtapeAuthentification surveillee = new VerificationClientExiste(espion);

            assertThrows(ClientIntrouvableException.class, () -> surveillee.verifier(null, "tata"));

            assertEquals(1, espion.verifications,
                    "Refuser un nom inconnu sans vérifier de haché rouvre l'oracle temporel.");
        }

        @Test
        @DisplayName("Un client connu ne paie pas cette vérification factice : la vraie viendra à l'étape 3")
        void clientConnuNePaiePasDeuxFois() {
            HachageEspion espion = new HachageEspion();
            EtapeAuthentification surveillee = new VerificationClientExiste(espion);

            surveillee.verifier(client, "tata");

            assertEquals(0, espion.verifications);
        }
    }

    /** Stratégie qui ne hache rien mais compte les vérifications demandées. */
    private static final class HachageEspion implements HachageStrategy {

        private int verifications;

        @Override
        public String hacher(String motDePasseClair) {
            return "haché(" + motDePasseClair + ")";
        }

        @Override
        public boolean verifier(String motDePasseClair, String hache) {
            verifications++;
            return hacher(motDePasseClair).equals(hache);
        }
    }

    @Nested
    @DisplayName("Étape 2 — verrouillage")
    class Verrouillage {

        private EtapeAuthentification etape;

        @BeforeEach
        void monter() {
            etape = new VerificationVerrouillage(registre);
        }

        @Test
        @DisplayName("Un compte sans échec passe l'étape")
        void compteLibrePasse() {
            assertDoesNotThrow(() -> etape.verifier(client, "tata"));
        }

        @Test
        @DisplayName("Deux échecs ne verrouillent pas encore")
        void deuxEchecsNeVerrouillentPas() {
            registre.enregistrerEchec(RIB);
            registre.enregistrerEchec(RIB);

            assertDoesNotThrow(() -> etape.verifier(client, "tata"));
        }

        @Test
        @DisplayName("Trois échecs verrouillent le compte")
        void troisEchecsVerrouillent() {
            for (int i = 0; i < 3; i++) {
                registre.enregistrerEchec(RIB);
            }

            assertThrows(CompteVerouilleException.class, () -> etape.verifier(client, "tata"));
        }

        @Test
        @DisplayName("Le message de verrouillage annonce le délai d'attente")
        void messageAnnonceLeDelai() {
            for (int i = 0; i < 3; i++) {
                registre.enregistrerEchec(RIB);
            }

            CompteVerouilleException levee = assertThrows(CompteVerouilleException.class,
                    () -> etape.verifier(client, "tata"));

            assertTrue(levee.getMessage().contains("15"), levee.getMessage());
        }
    }

    @Nested
    @DisplayName("Étape 3 — mot de passe")
    class MotDePasse {

        private EtapeAuthentification etape;

        @BeforeEach
        void monter() {
            etape = new VerificationMotDePasse(hachage, repertoire);
        }

        @Test
        @DisplayName("Le bon mot de passe passe l'étape")
        void bonMotDePassePasse() {
            assertDoesNotThrow(() -> etape.verifier(client, "tata"));
        }

        @Test
        @DisplayName("Un mauvais mot de passe est refusé par le message de non-divulgation")
        void mauvaisMotDePasseRefuse() {
            ClientIntrouvableException levee = assertThrows(ClientIntrouvableException.class,
                    () -> etape.verifier(client, "mauvais"));

            assertEquals("Nom d'utilisateur ou mot de passe incorrect.", levee.getMessage());
        }

        @Test
        @DisplayName("PONT DE MIGRATION — sans haché enregistré, la vérification retombe sur le modèle")
        void repliSurLeModele() {
            Client legacy = new Client("Legacy", "enclair", 999);

            assertDoesNotThrow(() -> etape.verifier(legacy, "enclair"));
            assertThrows(ClientIntrouvableException.class, () -> etape.verifier(legacy, "autre"));
        }

        @Test
        @DisplayName("Le repli échoue fermé : un haché stocké ne se compare pas à une saisie en clair")
        void repliEchoueFerme() {
            Client hacheDansLeModele = new Client("H", hachage.hacher("tata"), 888);

            assertThrows(ClientIntrouvableException.class,
                    () -> etape.verifier(hacheDansLeModele, "tata"));
        }
    }

    @Nested
    @DisplayName("Chaîne complète")
    class Complete {

        private EtapeAuthentification chaine;

        @BeforeEach
        void monterLaChaine() {
            EtapeAuthentification existence = new VerificationClientExiste(hachage);
            existence.puis(new VerificationVerrouillage(registre))
                    .puis(new VerificationMotDePasse(hachage, repertoire));
            chaine = existence;
        }

        @Test
        @DisplayName("Des identifiants valides traversent les trois étapes")
        void identifiantsValidesTraversent() {
            assertDoesNotThrow(() -> chaine.verifier(client, "tata"));
        }

        @Test
        @DisplayName("NON-DIVULGATION — client inconnu et mot de passe faux sont indiscernables")
        void clientInconnuEtMotDePasseFauxIndiscernables() {
            ClientIntrouvableException inconnu = assertThrows(ClientIntrouvableException.class,
                    () -> chaine.verifier(null, "tata"));
            ClientIntrouvableException motDePasseFaux = assertThrows(ClientIntrouvableException.class,
                    () -> chaine.verifier(client, "mauvais"));

            assertEquals(inconnu.getClass(), motDePasseFaux.getClass());
            assertEquals(inconnu.getMessage(), motDePasseFaux.getMessage());
        }

        /**
         * L'ORDRE EST LA PROPRIÉTÉ TESTÉE : le verrouillage passe AVANT le mot
         * de passe. Si l'ordre s'inversait un jour, un attaquant qui finit par
         * trouver le mot de passe entrerait malgré le verrouillage.
         */
        @Test
        @DisplayName("ORDRE — un compte verrouillé refuse même le bon mot de passe")
        void verrouillageAvantMotDePasse() {
            for (int i = 0; i < 3; i++) {
                registre.enregistrerEchec(RIB);
            }

            assertThrows(CompteVerouilleException.class, () -> chaine.verifier(client, "tata"));
        }

        @Test
        @DisplayName("ORDRE — un client inconnu est refusé avant même de regarder le verrouillage")
        void existenceAvantVerrouillage() {
            assertThrows(ClientIntrouvableException.class, () -> chaine.verifier(null, "tata"));
        }
    }
}
