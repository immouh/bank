package com.example.bank.core.service.commande;

import com.example.bank.core.exception.SoldeInsuffisantException;
import com.example.bank.core.model.Client;
import com.example.bank.core.repository.ClientRepository;
import com.example.bank.core.repository.InMemoryClientRepository;
import com.example.bank.core.service.BanqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L'invocateur : exécution des commandes et journal.
 *
 * Le journal est la porte laissée ouverte à un futur historique de session ou
 * à un undo. Ce qui compte donc ici : qu'il ne contienne QUE des opérations
 * réellement passées, sinon un rejeu rejouerait des échecs.
 */
@DisplayName("Invocateur de commandes")
class InvocateurCommandeTest {

    private InvocateurCommande invocateur;
    private BanqueService banqueService;
    private Client mouh;
    private Client amine;

    @BeforeEach
    void preparerLaBanque() {
        ClientRepository repository = new InMemoryClientRepository();
        banqueService = new BanqueService(repository);
        mouh = banqueService.rechercherParRib(123);
        amine = banqueService.rechercherParRib(456);
        invocateur = new InvocateurCommande();
    }

    @Test
    @DisplayName("Un invocateur neuf a un journal vide")
    void journalVideAuDepart() {
        assertEquals(0, invocateur.nombreExecutees());
        assertTrue(invocateur.getJournal().isEmpty());
    }

    @Test
    @DisplayName("L'invocateur exécute réellement la commande qu'on lui confie")
    void invocateurExecuteLaCommande() {
        invocateur.executer(new DeposerCommande(banqueService, mouh, new BigDecimal("500.00")));

        assertEquals(0, new BigDecimal("500.00").compareTo(mouh.getSoldeCompte()));
    }

    @Test
    @DisplayName("Une commande aboutie est journalisée")
    void commandeAboutieEstJournalisee() {
        invocateur.executer(new DeposerCommande(banqueService, mouh, new BigDecimal("500.00")));

        assertEquals(1, invocateur.nombreExecutees());
    }

    @Test
    @DisplayName("Le journal conserve l'ordre d'exécution")
    void journalConserveLOrdre() {
        invocateur.executer(new DeposerCommande(banqueService, mouh, new BigDecimal("500.00")));
        invocateur.executer(new RetirerCommande(banqueService, mouh, new BigDecimal("200.00")));
        invocateur.executer(new VirerCommande(banqueService, mouh, amine, new BigDecimal("100.00")));

        List<Commande> journal = invocateur.getJournal();
        assertEquals(3, journal.size());
        assertTrue(journal.get(0).libelle().startsWith("Dépôt"), journal.get(0).libelle());
        assertTrue(journal.get(1).libelle().startsWith("Retrait"), journal.get(1).libelle());
        assertTrue(journal.get(2).libelle().startsWith("Virement"), journal.get(2).libelle());
    }

    @Test
    @DisplayName("Une commande qui échoue n'est PAS journalisée : elle n'a rien changé")
    void commandeEnEchecNEstPasJournalisee() {
        Commande impossible = new RetirerCommande(banqueService, mouh, new BigDecimal("99999.00"));

        assertThrows(SoldeInsuffisantException.class, () -> invocateur.executer(impossible));
        assertEquals(0, invocateur.nombreExecutees());
    }

    @Test
    @DisplayName("L'exception métier remonte intacte jusqu'à l'appelant, qui l'affichera")
    void exceptionMetierRemonteIntacte() {
        Commande impossible = new RetirerCommande(banqueService, mouh, new BigDecimal("99999.00"));

        SoldeInsuffisantException levee = assertThrows(SoldeInsuffisantException.class,
                () -> invocateur.executer(impossible));
        assertTrue(levee.getMessage().contains("Solde insuffisant"), levee.getMessage());
    }

    @Test
    @DisplayName("Un échec n'empêche pas les commandes suivantes d'être journalisées")
    void echecNInterromptPasLaSuite() {
        assertThrows(SoldeInsuffisantException.class, () -> invocateur.executer(
                new RetirerCommande(banqueService, mouh, new BigDecimal("99999.00"))));
        invocateur.executer(new DeposerCommande(banqueService, mouh, new BigDecimal("500.00")));

        assertEquals(1, invocateur.nombreExecutees());
    }

    @Test
    @DisplayName("Le journal exposé n'est pas modifiable de l'extérieur")
    void journalNonModifiable() {
        invocateur.executer(new DeposerCommande(banqueService, mouh, new BigDecimal("500.00")));
        List<Commande> journal = invocateur.getJournal();

        assertThrows(UnsupportedOperationException.class, () -> journal.clear());
    }

    @Test
    @DisplayName("Vider le journal n'annule rien : les opérations restent passées")
    void viderLeJournalNAnnulePas() {
        invocateur.executer(new DeposerCommande(banqueService, mouh, new BigDecimal("500.00")));

        invocateur.viderJournal();

        assertEquals(0, invocateur.nombreExecutees());
        assertEquals(0, new BigDecimal("500.00").compareTo(mouh.getSoldeCompte()));
    }

    @Test
    @DisplayName("Une commande nulle est refusée")
    void commandeNulleRefusee() {
        assertThrows(NullPointerException.class, () -> invocateur.executer(null));
    }
}
