package com.example.bank.core.service.securite;

import com.example.bank.core.exception.technique.ParametreInvalideException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le Strategy de hachage.
 *
 * COÛT RÉDUIT DANS CES TESTS — coût 4 au lieu de 12 : on vérifie le
 * comportement de l'algorithme, pas sa lenteur, et une vingtaine de hachages
 * à coût réel ajouterait plusieurs secondes à la suite.
 */
@DisplayName("Hachage BCrypt")
class BCryptHachageStrategyTest {

    private final HachageStrategy hachage = new BCryptHachageStrategy(4);

    @Test
    @DisplayName("Un mot de passe haché se vérifie contre son propre haché")
    void hacherPuisVerifier() {
        String hache = hachage.hacher("tata");

        assertTrue(hachage.verifier("tata", hache));
    }

    @Test
    @DisplayName("Un mauvais mot de passe ne correspond pas au haché")
    void mauvaisMotDePasseRejete() {
        String hache = hachage.hacher("tata");

        assertFalse(hachage.verifier("mauvais", hache));
    }

    @Test
    @DisplayName("La casse compte : « Tata » ne correspond pas au haché de « tata »")
    void casseSignificative() {
        assertFalse(hachage.verifier("Tata", hachage.hacher("tata")));
    }

    /**
     * Propriété normale de BCrypt : le sel est tiré au hasard et rangé dans
     * le haché. C'est ce qui rend les tables précalculées inutilisables — et
     * ce qui interdit de comparer deux hachés entre eux.
     */
    @Test
    @DisplayName("Hacher deux fois le même mot de passe donne deux hachés différents (sel aléatoire)")
    void selAleatoire() {
        assertNotEquals(hachage.hacher("tata"), hachage.hacher("tata"));
    }

    @Test
    @DisplayName("Les deux hachés d'un même mot de passe se vérifient l'un comme l'autre")
    void lesDeuxHachesRestentValables() {
        assertTrue(hachage.verifier("tata", hachage.hacher("tata")));
        assertTrue(hachage.verifier("tata", hachage.hacher("tata")));
    }

    @Test
    @DisplayName("Le haché ne contient pas le mot de passe en clair")
    void hacheNeContientPasLeClair() {
        assertFalse(hachage.hacher("tata").contains("tata"));
    }

    @Test
    @DisplayName("Un haché BCrypt est reconnaissable à son préfixe et tient dans 255 caractères")
    void formatDuHache() {
        String hache = hachage.hacher("tata");

        assertTrue(hache.startsWith("$2"), hache);
        assertTrue(hache.length() <= 255, "Longueur : " + hache.length());
    }

    @Test
    @DisplayName("Vérifier contre un haché absent rend faux au lieu de lever")
    void hacheAbsentRendFaux() {
        assertFalse(hachage.verifier("tata", null));
    }

    @Test
    @DisplayName("Vérifier une saisie absente rend faux au lieu de lever")
    void saisieAbsenteRendFausse() {
        assertFalse(hachage.verifier(null, hachage.hacher("tata")));
    }

    @Test
    @DisplayName("Un mot de passe stocké en clair par une version antérieure ne laisse entrer personne")
    void hacheIllisibleRendFaux() {
        assertFalse(hachage.verifier("tata", "tata"));
    }

    @Test
    @DisplayName("Hacher un mot de passe absent est refusé")
    void hacherNullRefuse() {
        assertThrows(ParametreInvalideException.class, () -> hachage.hacher(null));
    }

    @Test
    @DisplayName("Le haché factice ne correspond à aucune saisie")
    void hacheFacticeNeCorrespondAJamais() {
        String factice = hachage.hacheFactice();

        assertFalse(hachage.verifier("tata", factice));
        assertFalse(hachage.verifier("", factice));
    }

    @Test
    @DisplayName("Le haché factice est stable : il n'est pas recalculé à chaque appel")
    void hacheFacticeStable() {
        assertTrue(hachage.hacheFactice() == hachage.hacheFactice(),
                "Le haché factice devrait être une constante, pas un calcul.");
    }
}
