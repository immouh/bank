package com.example.bank.swing;

import com.example.bank.core.model.Client;
import com.example.bank.core.repository.BaseDeDonneesH2;
import com.example.bank.core.repository.JdbcClientRepository;
import com.example.bank.core.service.AuthService;
import com.example.bank.core.service.BanqueService;
import com.example.bank.core.service.audit.JdbcJournalAudit;
import com.example.bank.core.service.audit.JournalAudit;
import com.example.bank.core.service.commande.InvocateurCommande;
import com.example.bank.core.service.securite.BCryptHachageStrategy;
import com.example.bank.core.service.securite.HachageStrategy;
import com.example.bank.core.service.securite.JdbcRegistreTentatives;
import com.example.bank.core.service.securite.JdbcRepertoireMotsDePasse;
import com.example.bank.core.service.securite.RegistreTentatives;
import com.example.bank.core.service.securite.RepertoireMotsDePasse;
import com.example.bank.swing.controller.CompteController;
import com.example.bank.swing.controller.LoginController;
import com.example.bank.swing.controller.VirementController;
import com.example.bank.swing.ui.GestionnaireInterfaceGraphique;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Point d'assemblage de l'application : c'est le seul endroit qui sait quelle
 * implémentation de repository est utilisée et qui câble services et
 * controllers entre eux.
 *
 * Depuis la phase C, le stockage est une base H2 en mode fichier : les
 * comptes survivent à la fermeture. {@code InMemoryClientRepository} reste
 * dans le code — c'est lui qu'utilisent les tests qui n'ont pas besoin d'une
 * vraie base.
 *
 * Depuis la phase D, les mots de passe sont stockés HACHÉS. Une base créée
 * avant cette phase contient encore des mots de passe en clair, que la
 * vérification BCrypt rejettera : supprimer le dossier {@code data/} pour
 * repartir d'une base propre.
 */
public class Main {

    private static final Path DOSSIER_DONNEES = Path.of("data");

    public static void main(String[] args) throws IOException {
        // H2 crée le fichier de base, pas le dossier qui le contient.
        Files.createDirectories(DOSSIER_DONNEES);

        // Câblage des couches, du plus bas au plus haut.
        Connection connexion = BaseDeDonneesH2.ouvrir(BaseDeDonneesH2.URL_FICHIER);
        fermerALaSortie(connexion);

        JdbcClientRepository repository = new JdbcClientRepository(connexion);
        BanqueService banqueService = new BanqueService(repository);

        // SÉCURITÉ — c'est ici, et nulle part ailleurs, qu'on choisit
        // l'algorithme de hachage. En changer un jour ne touchera pas
        // AuthService, qui ne connaît que l'interface.
        HachageStrategy hachage = new BCryptHachageStrategy();
        RepertoireMotsDePasse repertoire = new JdbcRepertoireMotsDePasse(connexion);
        RegistreTentatives registre = new JdbcRegistreTentatives(connexion);
        JournalAudit audit = new JdbcJournalAudit(connexion);

        AuthService authService =
                new AuthService(repository, hachage, repertoire, registre, audit);

        // Uniquement au tout premier lancement : sur une base déjà peuplée,
        // rejouer les mouvements de démonstration doublerait les soldes à
        // chaque démarrage.
        if (repository.findAll().isEmpty()) {
            creerDonneesDeDemonstration(repository, banqueService, hachage);
        }

        InvocateurCommande invocateur = new InvocateurCommande(audit);

        LoginController loginController = new LoginController(authService);
        CompteController compteController = new CompteController(banqueService, invocateur);
        VirementController virementController =
                new VirementController(banqueService, repository, invocateur);

        // Swing doit être construit et manipulé sur l'Event Dispatch Thread.
        SwingUtilities.invokeLater(() ->
                new GestionnaireInterfaceGraphique(loginController, compteController, virementController));
    }

    /**
     * Crée les deux clients de démonstration et rejoue les mouvements
     * d'origine. État final identique à celui des versions précédentes :
     * Mouh compte 0,00 € / Livret A 1000,00 €, amine compte 300,00 €.
     *
     * La création passe par {@code creer(client, motDePasse)} : le mot de
     * passe n'est pas lisible depuis un {@code Client}, il doit donc être
     * fourni au moment de l'insertion.
     *
     * LE CLAIR NE TOUCHE JAMAIS LA BASE : il est haché ici même, et c'est le
     * haché qui est inséré.
     */
    private static void creerDonneesDeDemonstration(JdbcClientRepository repository,
                                                    BanqueService banqueService,
                                                    HachageStrategy hachage) {
        Client mouh = new Client("Mouh", "tata", 123);
        Client amine = new Client("amine", "matoub", 456);
        repository.creer(mouh, hachage.hacher("tata"));
        repository.creer(amine, hachage.hacher("matoub"));

        banqueService.creerLivretA(mouh);
        banqueService.deposer(mouh, new BigDecimal("1000"));
        banqueService.virerVersLivretA(mouh, new BigDecimal("1000"));

        banqueService.deposer(mouh, new BigDecimal("500"));
        banqueService.retirer(mouh, new BigDecimal("200"));
        banqueService.virer(mouh, amine, new BigDecimal("300"));

        // L'historique s'écrit à part : voir JdbcClientRepository.
        repository.enregistrerHistorique(mouh);
        repository.enregistrerHistorique(amine);
    }

    /** Ferme la base proprement quand la JVM s'arrête. */
    private static void fermerALaSortie(Connection connexion) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                connexion.close();
            } catch (SQLException ignore) {
                // La JVM s'arrête : plus personne à qui signaler l'échec.
            }
        }));
    }
}
