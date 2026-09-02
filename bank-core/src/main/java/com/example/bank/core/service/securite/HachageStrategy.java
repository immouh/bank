package com.example.bank.core.service.securite;

/**
 * Patron Strategy : encapsule l'algorithme de hachage du mot de passe, pour
 * qu'il puisse changer sans que l'authentification ne bouge.
 *
 * POURQUOI UNE STRATÉGIE ICI — les recommandations en matière de hachage se
 * périment. BCrypt aujourd'hui, Argon2 demain ; le jour venu, il y aura une
 * classe à écrire et une ligne de câblage à changer dans {@code Main}, pas
 * une ligne dans {@code AuthService}. C'est exactement le rôle que
 * {@code TauxInteretStrategy} joue déjà pour la rémunération du Livret A.
 *
 * CONTRAT — {@link #hacher} doit produire un résultat DIFFÉRENT à chaque
 * appel pour un même mot de passe (sel aléatoire), et {@link #verifier} doit
 * savoir retrouver la correspondance malgré ça. Comparer deux hachés entre
 * eux n'a donc aucun sens : seule la vérification fait autorité.
 */
public interface HachageStrategy {

    /** Hache un mot de passe en clair, prêt à être stocké. */
    String hacher(String motDePasseClair);

    /**
     * Vérifie qu'un mot de passe en clair correspond à un haché stocké.
     * Ne lève jamais : une saisie absurde ou un haché illisible rend
     * simplement {@code false}.
     */
    boolean verifier(String motDePasseClair, String hache);

    /**
     * Un haché de référence qui ne correspond à AUCUN mot de passe connu,
     * mais dont la vérification coûte le même temps qu'une vraie.
     *
     * POURQUOI L'ALGORITHME DOIT LE FOURNIR — il sert à répondre à un nom
     * d'utilisateur inconnu en prenant exactement le temps d'un refus de mot
     * de passe (voir {@link VerificationClientExiste}). Seul l'algorithme
     * sait ce que « coûter le même temps » veut dire chez lui, et sait en
     * produire un sans le recalculer à chaque tentative.
     */
    default String hacheFactice() {
        return hacher("aucun mot de passe ne correspond à ce haché");
    }
}
