package com.example.bank.core.service.securite;

import com.example.bank.core.model.Client;

import java.util.Optional;

/**
 * Troisième étape : la saisie correspond-elle au mot de passe enregistré ?
 *
 * En cas d'échec, lève EXACTEMENT la même exception, avec le même message,
 * que {@link VerificationClientExiste} — c'est la propriété de
 * non-divulgation que sept tests de la phase A verrouillent.
 */
public class VerificationMotDePasse extends EtapeAuthentification {

    private final HachageStrategy hachage;
    private final RepertoireMotsDePasse repertoire;

    public VerificationMotDePasse(HachageStrategy hachage, RepertoireMotsDePasse repertoire) {
        this.hachage = hachage;
        this.repertoire = repertoire;
    }

    @Override
    protected void executer(Client client, String motDePasseSaisi) {
        if (!correspond(client, motDePasseSaisi)) {
            throw VerificationClientExiste.identifiantsInvalides();
        }
    }

    private boolean correspond(Client client, String saisie) {
        Optional<String> hache = repertoire.hachePour(client.getRib());
        if (hache.isPresent()) {
            return hachage.verifier(saisie, hache.get());
        }
        // PONT DE MIGRATION — aucun haché enregistré pour ce client : on
        // retombe sur la comparaison portée par le modèle, celle d'avant le
        // hachage. C'est ce qui permet au stockage en mémoire, encore peuplé
        // de mots de passe en clair, de continuer à fonctionner tel quel.
        //
        // Ce repli échoue FERMÉ : sur la base H2, le champ du modèle contient
        // le haché, et le comparer à une saisie en clair rend toujours faux.
        // Une panne du répertoire ne laisse donc entrer personne.
        //
        // À SUPPRIMER quand tous les magasins seront hachés.
        return client.verifierMotDePasse(saisie);
    }
}
