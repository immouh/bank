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
        // PONT DE MIGRATION, TOUJOURS NÉCESSAIRE — aucun haché enregistré
        // pour ce client : on retombe sur la comparaison portée par le
        // modèle, celle d'avant le hachage.
        //
        // LE CAS QUI SUBSISTE, un seul : le montage EN MÉMOIRE, celui du
        // constructeur à un argument d'{@code AuthService}. Il associe un
        // {@code InMemoryClientRepository}, dont les clients portent encore
        // leur mot de passe en clair, à un {@code RepertoireMotsDePasseEnMemoire}
        // vide — personne ne peut le peupler, car {@code Client} n'expose
        // aucun accesseur de mot de passe (un test de la phase A l'interdit) :
        // le clair est illisible depuis l'extérieur, donc impossible à hacher
        // après coup. Sans ce repli, ce montage ne laisserait entrer personne.
        //
        // LA BASE H2 NE PASSE PLUS ICI : Main hache à la création et
        // JdbcRepertoireMotsDePasse rend toujours la colonne d'un client
        // existant. Il n'y a pas non plus de migration de données en attente —
        // une base d'avant la phase D se jette, Main le dit.
        //
        // Ce repli échoue FERMÉ : là où le champ du modèle contiendrait un
        // haché, le comparer à une saisie en clair rend toujours faux. Une
        // panne du répertoire ne laisse donc entrer personne.
        //
        // CONDITION DE SUPPRESSION — le jour où plus aucun montage ne laisse
        // le répertoire vide face à des clients en clair : soit
        // {@code InMemoryClientRepository} cesse de porter des mots de passe,
        // soit le constructeur à un argument d'{@code AuthService} disparaît
        // au profit d'un montage qui remplit lui-même le répertoire. Ce jour-là
        // ces trois lignes tombent, et {@code correspond} se réduit à
        // {@code repertoire.hachePour(...).filter(...)}.
        return client.verifierMotDePasse(saisie);
    }
}
