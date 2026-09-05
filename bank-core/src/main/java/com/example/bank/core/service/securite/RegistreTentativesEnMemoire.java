package com.example.bank.core.service.securite;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Registre volatil : les compteurs disparaissent à l'arrêt de l'application.
 *
 * L'HORLOGE EST INJECTÉE — sans ça, tester l'expiration d'un verrouillage
 * demanderait d'attendre quinze minutes pour de vrai. Avec une
 * {@code Clock.fixed}, le test avance le temps à la main.
 */
public class RegistreTentativesEnMemoire implements RegistreTentatives {

    private final Map<Integer, Compteur> compteurs = new HashMap<>();
    private final Clock horloge;

    public RegistreTentativesEnMemoire() {
        this(Clock.systemDefaultZone());
    }

    public RegistreTentativesEnMemoire(Clock horloge) {
        this.horloge = horloge;
    }

    @Override
    public void enregistrerEchec(int rib) {
        Compteur actuel = compteurs.get(rib);
        int echecs = actuel == null ? 1 : actuel.echecs() + 1;
        compteurs.put(rib, new Compteur(echecs, LocalDateTime.now(horloge)));
    }

    @Override
    public void reinitialiser(int rib) {
        compteurs.remove(rib);
    }

    @Override
    public boolean estVerrouille(int rib) {
        Compteur compteur = compteurs.get(rib);
        if (compteur == null || compteur.echecs() < ECHECS_AVANT_VERROUILLAGE) {
            return false;
        }
        if (Verrouillage.expire(compteur.derniereTentative(), horloge)) {
            // Le verrouillage a fait son temps : l'ardoise s'efface d'elle-même.
            compteurs.remove(rib);
            return false;
        }
        return true;
    }

    @Override
    public int echecs(int rib) {
        Compteur compteur = compteurs.get(rib);
        return compteur == null ? 0 : compteur.echecs();
    }

    @Override
    public int minutesRestantes(int rib) {
        if (!estVerrouille(rib)) {
            return 0;
        }
        return Verrouillage.minutesRestantes(compteurs.get(rib).derniereTentative(), horloge);
    }

    private record Compteur(int echecs, LocalDateTime derniereTentative) {
    }
}
