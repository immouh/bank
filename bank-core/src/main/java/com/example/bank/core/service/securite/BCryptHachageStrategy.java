package com.example.bank.core.service.securite;

import com.example.bank.core.exception.technique.ParametreInvalideException;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Hachage BCrypt : sel aléatoire intégré au haché, et coût de calcul
 * volontairement élevé.
 *
 * LE COÛT EST LA FONCTIONNALITÉ — BCrypt est lent exprès. Un hachage rapide
 * (SHA-256 et consorts) permet de tester des milliards de mots de passe par
 * seconde sur du matériel grand public ; ici chaque essai coûte quelques
 * dizaines de millisecondes, ce qui rend l'attaque par force brute
 * inintéressante. Le facteur de coût est donc un réglage de sécurité, pas un
 * problème de performance à optimiser.
 *
 * Le sel étant tiré au hasard et rangé DANS le haché, deux hachages du même
 * mot de passe donnent deux chaînes différentes — c'est ce qui rend les
 * tables précalculées inutilisables.
 */
public class BCryptHachageStrategy implements HachageStrategy {

    /**
     * Facteur de coût : 2^12 itérations, de l'ordre de 100 ms par
     * vérification sur une machine de bureau actuelle. Au-delà, la connexion
     * devient perceptiblement lente ; en deçà, l'attaque redevient abordable.
     */
    private static final int COUT = 12;

    /**
     * Haché de référence pour les noms d'utilisateur inconnus, calculé une
     * fois pour toutes au coût par défaut et figé ici.
     *
     * Le recalculer au démarrage coûterait une centaine de millisecondes à
     * chaque construction du service — mesuré : la suite de tests de
     * l'authentification passait de quelques centièmes de seconde à onze
     * secondes. C'est le haché d'un identifiant aléatoire que personne n'a
     * jamais connu : aucune saisie ne peut lui correspondre.
     */
    private static final String HACHE_FACTICE =
            "$2a$12$QA1gXW2JUPCf9.FNDsRmCuacPfFIXAjhOQM4Iz5nfvd22hxqKTxU2";

    private final int cout;

    public BCryptHachageStrategy() {
        this(COUT);
    }

    /** Coût réglable, notamment pour les tests, qui n'ont pas à payer 100 ms par cas. */
    public BCryptHachageStrategy(int cout) {
        this.cout = cout;
    }

    @Override
    public String hacher(String motDePasseClair) {
        if (motDePasseClair == null) {
            throw new ParametreInvalideException("Le mot de passe à hacher est obligatoire.");
        }
        return BCrypt.hashpw(motDePasseClair, BCrypt.gensalt(cout));
    }

    /**
     * Figé au coût par défaut : une stratégie réglée sur un autre coût
     * verra donc un temps de refus légèrement différent du sien. C'est sans
     * conséquence, le réglage n'existant que pour accélérer les tests.
     */
    @Override
    public String hacheFactice() {
        return HACHE_FACTICE;
    }

    @Override
    public boolean verifier(String motDePasseClair, String hache) {
        if (motDePasseClair == null || hache == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(motDePasseClair, hache);
        } catch (IllegalArgumentException hacheIllisible) {
            // Haché absent, tronqué, ou stocké en clair par une version
            // antérieure : on refuse, on ne devine pas.
            return false;
        }
    }
}
