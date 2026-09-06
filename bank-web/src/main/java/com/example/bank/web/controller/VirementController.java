package com.example.bank.web.controller;

import com.example.bank.core.exception.existence.ClientIntrouvableException;
import com.example.bank.core.model.Client;
import com.example.bank.core.repository.JdbcClientRepository;
import com.example.bank.core.service.BanqueService;
import com.example.bank.core.service.commande.InvocateurCommande;
import com.example.bank.core.service.commande.VirerCommande;
import com.example.bank.web.dto.OperationReponse;
import com.example.bank.web.dto.VirementRequete;
import com.example.bank.web.persistance.HistoriquePersistant;
import com.example.bank.web.securite.ClientAuthentifie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Virement du compte courant de l'appelant vers celui d'un autre client.
 *
 * L'ÉMETTEUR VIENT DU JETON, jamais du corps de la requête : le seul RIB que
 * le client fournit est celui du DESTINATAIRE. Débiter quelqu'un d'autre est
 * donc impossible, pas seulement interdit.
 *
 * L'ATOMICITÉ RESTE CELLE DU COEUR — {@code BanqueService.virer} appelle
 * {@code sauvegarderEnsemble}, qui pose une vraie transaction SQL. Aucun
 * {@code @Transactional} n'est ajouté ici : il en donnerait une seconde,
 * au-dessus de la première, sans rien garantir de plus.
 */
@RestController
@RequestMapping("/api/virements")
public class VirementController {

    private final BanqueService banqueService;
    private final JdbcClientRepository repository;
    private final InvocateurCommande invocateur;
    private final HistoriquePersistant historique;

    public VirementController(BanqueService banqueService,
                              JdbcClientRepository repository,
                              InvocateurCommande invocateur,
                              HistoriquePersistant historique) {
        this.banqueService = banqueService;
        this.repository = repository;
        this.invocateur = invocateur;
        this.historique = historique;
    }

    @PostMapping
    public OperationReponse virer(@RequestBody VirementRequete requete) {
        Client emetteur = banqueService.rechercherParRib(ClientAuthentifie.rib());
        Client destinataire = destinataire(requete);

        int avantEmetteur = emetteur.getHistorique().size();
        int avantDestinataire = destinataire.getHistorique().size();

        invocateur.executer(
                new VirerCommande(banqueService, emetteur, destinataire, requete.montant()));

        // Un virement produit une ligne DE CHAQUE CÔTÉ : les deux sont
        // écrites, sans quoi le destinataire verrait son solde bouger sans
        // trace de l'opération qui l'a crédité.
        historique.ajouter(emetteur, avantEmetteur);
        historique.ajouter(destinataire, avantDestinataire);

        return new OperationReponse("Virement de " + requete.montant() + " € vers "
                + destinataire.getNom(), emetteur.getSoldeCompte());
    }

    /**
     * Le destinataire est cherché AVANT l'exécution, et son absence remonte
     * le même message que côté Swing. Un RIB destinataire nul est traité
     * comme un destinataire introuvable plutôt que de laisser passer un
     * {@code NullPointerException} jusqu'au coeur.
     */
    private Client destinataire(VirementRequete requete) {
        if (requete.ribDestinataire() == null) {
            throw new ClientIntrouvableException("Destinataire introuvable.");
        }
        return repository.findByRib(requete.ribDestinataire())
                .orElseThrow(() -> new ClientIntrouvableException("Destinataire introuvable."));
    }
}
