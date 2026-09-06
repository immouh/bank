package com.example.bank.web.controller;

import com.example.bank.core.model.Client;
import com.example.bank.core.model.offre.compte.Compte;
import com.example.bank.core.repository.JdbcClientRepository;
import com.example.bank.core.service.BanqueService;
import com.example.bank.core.service.audit.EvenementAudit;
import com.example.bank.core.service.audit.JournalAudit;
import com.example.bank.core.service.commande.Commande;
import com.example.bank.core.service.commande.DeposerCommande;
import com.example.bank.core.service.commande.InvocateurCommande;
import com.example.bank.core.service.commande.RetirerCommande;
import com.example.bank.web.dto.CompteReponse;
import com.example.bank.web.dto.MontantRequete;
import com.example.bank.web.dto.OperationReponse;
import com.example.bank.web.dto.SituationReponse;
import com.example.bank.web.persistance.HistoriquePersistant;
import com.example.bank.web.securite.ClientAuthentifie;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consultation et opérations sur les comptes du client authentifié.
 *
 * AUCUN ENDPOINT NE PREND DE RIB. Le client concerné est toujours celui du
 * jeton, lu par {@link ClientAuthentifie}. C'est ce qui rend structurellement
 * impossible de lire ou de mouvementer le compte d'un autre : il n'y a aucun
 * paramètre à falsifier.
 *
 * LES OPÉRATIONS PASSENT PAR LES COMMANDES, comme côté Swing — c'est
 * {@code InvocateurCommande} qui journalise à l'audit, une fois, au même
 * endroit pour les deux frontaux.
 */
@RestController
@RequestMapping("/api")
public class CompteController {

    private final BanqueService banqueService;
    private final JdbcClientRepository repository;
    private final InvocateurCommande invocateur;
    private final JournalAudit audit;
    private final HistoriquePersistant historique;

    public CompteController(BanqueService banqueService,
                            JdbcClientRepository repository,
                            InvocateurCommande invocateur,
                            JournalAudit audit,
                            HistoriquePersistant historique) {
        this.banqueService = banqueService;
        this.repository = repository;
        this.invocateur = invocateur;
        this.audit = audit;
        this.historique = historique;
    }

    @GetMapping("/comptes/moi")
    public SituationReponse situation() {
        return situationDe(banqueService.rechercherParRib(ClientAuthentifie.rib()));
    }

    @PostMapping("/comptes/depot")
    public OperationReponse deposer(@RequestBody MontantRequete requete) {
        Client client = banqueService.rechercherParRib(ClientAuthentifie.rib());
        return executer(client, new DeposerCommande(banqueService, client, requete.montant()));
    }

    @PostMapping("/comptes/retrait")
    public OperationReponse retirer(@RequestBody MontantRequete requete) {
        Client client = banqueService.rechercherParRib(ClientAuthentifie.rib());
        return executer(client, new RetirerCommande(banqueService, client, requete.montant()));
    }

    /**
     * Ouverture du Livret A.
     *
     * PAS DE COMMANDE POUR CELLE-CI : le coeur n'en fournit pas (il n'y a ni
     * {@code OuvrirLivretACommande} côté Swing), et en inventer une ici la
     * mettrait dans le mauvais module. L'audit est donc posé à la main, comme
     * {@code AuthService} le fait pour les connexions.
     *
     * 201 Created : l'appel fait apparaître une ressource qui n'existait pas.
     */
    @PostMapping("/livret-a")
    @ResponseStatus(HttpStatus.CREATED)
    public SituationReponse ouvrirLivretA() {
        Client client = banqueService.rechercherParRib(ClientAuthentifie.rib());
        banqueService.creerLivretA(client);
        audit.enregistrer(client.getRib(), EvenementAudit.OUVERTURE_LIVRET_A,
                "Ouverture du Livret A");
        return situationDe(banqueService.rechercherParRib(client.getRib()));
    }

    /**
     * Exécute la commande puis persiste les lignes d'historique qu'elle a
     * produites.
     *
     * L'INDICE EST RELEVÉ AVANT — un client relu depuis la base revient avec
     * un historique en mémoire vide, mais s'en remettre à cette hypothèse
     * serait fragile. On écrit donc explicitement « tout ce qui est apparu
     * depuis », voir {@link HistoriquePersistant}.
     *
     * L'écriture vient APRÈS l'exécution : une commande refusée lève, et rien
     * n'est écrit — même règle que pour le journal d'audit.
     */
    private OperationReponse executer(Client client, Commande commande) {
        int avant = client.getHistorique().size();
        invocateur.executer(commande);
        historique.ajouter(client, avant);
        return new OperationReponse(commande.libelle(), client.getSoldeCompte());
    }

    /** Conversion vers le DTO : rien de ce qui n'est pas listé ici ne sort. */
    private SituationReponse situationDe(Client client) {
        List<CompteReponse> comptes = client.getComptes().stream()
                .map(CompteController::compteDe)
                .toList();
        return new SituationReponse(client.getRib(), client.getNom(),
                client.soldeTotal(), comptes);
    }

    private static CompteReponse compteDe(Compte compte) {
        return new CompteReponse(
                // Le nom de la classe concrète EST le type de produit
                // (CompteStandard, LivretA…), et c'est déjà ce que la base
                // stocke dans comptes.type : une même convention des deux côtés.
                compte.getClass().getSimpleName(),
                compte.estEpargne(),
                compte.getSolde(),
                compte.getDecouvertAutorise(),
                compte.getEtat().libelle());
    }
}
