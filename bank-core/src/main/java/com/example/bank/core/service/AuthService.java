package com.example.bank.core.service;

import com.example.bank.core.exception.BanqueException;
import com.example.bank.core.exception.existence.ClientIntrouvableException;
import com.example.bank.core.exception.etat.CompteVerouilleException;
import com.example.bank.core.model.Client;
import com.example.bank.core.repository.ClientRepository;
import com.example.bank.core.service.audit.EvenementAudit;
import com.example.bank.core.service.audit.JournalAudit;
import com.example.bank.core.service.audit.JournalAuditEnMemoire;
import com.example.bank.core.service.securite.BCryptHachageStrategy;
import com.example.bank.core.service.securite.EtapeAuthentification;
import com.example.bank.core.service.securite.HachageStrategy;
import com.example.bank.core.service.securite.RegistreTentatives;
import com.example.bank.core.service.securite.RegistreTentativesEnMemoire;
import com.example.bank.core.service.securite.RepertoireMotsDePasse;
import com.example.bank.core.service.securite.RepertoireMotsDePasseEnMemoire;
import com.example.bank.core.service.securite.VerificationClientExiste;
import com.example.bank.core.service.securite.VerificationMotDePasse;
import com.example.bank.core.service.securite.VerificationVerrouillage;

/**
 * Authentification.
 *
 * RÈGLE DE PLACEMENT — la RECHERCHE du client par son nom demande le
 * repository : c'est au service de la faire. Ce qu'il ne fait plus lui-même,
 * c'est décider si la connexion est valable : cette décision est déléguée à
 * une CHAÎNE de contrôles (existence, verrouillage, mot de passe), montée
 * une fois pour toutes dans le constructeur.
 *
 * Le service ne garde donc que ce qu'aucune étape ne peut porter : les
 * CONSÉQUENCES d'un résultat — incrémenter les échecs, remettre le compteur à
 * zéro, consigner au journal d'audit. Une étape contrôle, elle ne décide de
 * rien d'autre.
 *
 * <h2>Le hachage n'est pas écrit ici</h2>
 * L'algorithme arrive par injection ({@link HachageStrategy}), comme le taux
 * du Livret A arrive dans {@code LivretA}. Passer de BCrypt à autre chose est
 * une ligne dans {@code Main}, pas une ligne dans ce fichier.
 */
public class AuthService {

    private final ClientRepository repository;
    private final RegistreTentatives registre;
    private final JournalAudit journal;
    private final EtapeAuthentification premiereEtape;

    /**
     * Montage minimal, sans stockage : hachage BCrypt, répertoire, registre et
     * journal en mémoire.
     *
     * Utilisé par les tests et par tout appelant qui n'a pas de base à
     * fournir. Aucun mot de passe n'étant enregistré dans le répertoire, la
     * vérification retombe sur le pont de migration décrit dans
     * {@link VerificationMotDePasse} — c'est ce qui laisse fonctionner le
     * stockage en mémoire, encore peuplé de mots de passe en clair.
     */
    public AuthService(ClientRepository repository) {
        this(repository,
                new BCryptHachageStrategy(),
                new RepertoireMotsDePasseEnMemoire(),
                new RegistreTentativesEnMemoire(),
                new JournalAuditEnMemoire());
    }

    public AuthService(ClientRepository repository,
                       HachageStrategy hachage,
                       RepertoireMotsDePasse repertoire,
                       RegistreTentatives registre,
                       JournalAudit journal) {
        this.repository = repository;
        this.registre = registre;
        this.journal = journal;

        // L'ORDRE EST DÉCLARÉ ICI, une bonne fois : existence, puis
        // verrouillage, puis mot de passe. Le lire dans le montage plutôt que
        // dans l'ordre des lignes d'une méthode, c'est tout l'intérêt.
        EtapeAuthentification existence = new VerificationClientExiste(hachage);
        existence.puis(new VerificationVerrouillage(registre))
                .puis(new VerificationMotDePasse(hachage, repertoire));
        this.premiereEtape = existence;
    }

    /**
     * @return le client authentifié
     * @throws ClientIntrouvableException si le nom est inconnu OU le mot de
     *         passe faux — un message unique dans les deux cas, pour ne pas
     *         révéler quels noms d'utilisateur existent.
     * @throws CompteVerouilleException si le compte est suspendu après trop
     *         de tentatives échouées
     */
    public Client authentifier(String nom, String motDePasse) {
        Client client = repository.findByNom(nom).orElse(null);
        try {
            premiereEtape.verifier(client, motDePasse);
        } catch (BanqueException refus) {
            consignerLEchec(client, refus);
            throw refus;
        }

        // Une connexion réussie efface l'ardoise : les trois essais comptent
        // consécutivement, pas sur la vie entière du compte.
        registre.reinitialiser(client.getRib());
        journal.enregistrer(client.getRib(), EvenementAudit.CONNEXION_REUSSIE);
        return client;
    }

    /**
     * Un nom inconnu ne compte aucun échec : il n'y a pas de compte à
     * verrouiller, et rien à consigner qui ne soit du bruit.
     *
     * Un refus pour VERROUILLAGE n'incrémente pas non plus : sans quoi
     * l'utilisateur bloqué qui réessaie repousserait sa propre échéance
     * indéfiniment.
     */
    private void consignerLEchec(Client client, BanqueException refus) {
        if (client == null || refus instanceof CompteVerouilleException) {
            return;
        }
        int rib = client.getRib();
        registre.enregistrerEchec(rib);
        journal.enregistrer(rib, EvenementAudit.CONNEXION_ECHOUEE,
                "échec " + registre.echecs(rib) + "/" + RegistreTentatives.ECHECS_AVANT_VERROUILLAGE);
        if (registre.estVerrouille(rib)) {
            journal.enregistrer(rib, EvenementAudit.COMPTE_VEROUILLE,
                    RegistreTentatives.MINUTES_DE_VERROUILLAGE + " minutes");
        }
    }
}
