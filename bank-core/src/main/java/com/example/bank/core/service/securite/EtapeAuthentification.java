package com.example.bank.core.service.securite;

import com.example.bank.core.exception.BanqueException;
import com.example.bank.core.model.Client;

/**
 * Patron Chain of Responsibility : une étape du contrôle de connexion.
 *
 * CE QUE LA CHAÎNE APPORTE ICI — l'ordre des contrôles devient une donnée du
 * code au lieu d'un accident. Avec une cascade de {@code if}, rien ne dit
 * qu'il est INTENTIONNEL de vérifier le verrouillage avant le mot de passe ;
 * on le déduit de l'ordre des lignes, et le premier qui réorganise la méthode
 * casse la propriété sans s'en rendre compte. C'est exactement le défaut
 * relevé au point 4 de l'audit de phase A à propos de {@code virer}.
 *
 * Ici l'ordre est déclaré au montage de la chaîne, chaque étape est testable
 * seule, et en insérer une nouvelle (contrôle d'IP, second facteur) ne
 * demande de toucher à aucune étape existante.
 *
 * {@link #verifier} est {@code final} : une étape décide de ce qu'elle
 * contrôle, jamais de qui vient après elle.
 */
public abstract class EtapeAuthentification {

    private EtapeAuthentification suivante;

    /** Chaîne une étape après celle-ci et rend cette dernière, pour enchaîner les appels. */
    public EtapeAuthentification puis(EtapeAuthentification suivante) {
        this.suivante = suivante;
        return suivante;
    }

    /**
     * Exécute ce contrôle puis passe la main.
     *
     * @param client         le client trouvé, ou {@code null} si le nom saisi
     *                       ne correspond à personne
     * @throws BanqueException au premier contrôle qui refuse
     */
    public final void verifier(Client client, String motDePasseSaisi) {
        executer(client, motDePasseSaisi);
        if (suivante != null) {
            suivante.verifier(client, motDePasseSaisi);
        }
    }

    /** Le contrôle propre à cette étape. Laisse passer en ne levant rien. */
    protected abstract void executer(Client client, String motDePasseSaisi);
}
