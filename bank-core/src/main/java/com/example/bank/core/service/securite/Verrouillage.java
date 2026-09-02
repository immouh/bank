package com.example.bank.core.service.securite;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Arithmétique du verrouillage, partagée par les deux registres.
 *
 * Écrite une fois pour que la version en mémoire et la version en base ne
 * puissent pas diverger sur « quand le verrouillage tombe ».
 */
final class Verrouillage {

    private Verrouillage() {
    }

    static boolean expire(LocalDateTime derniereTentative, Clock horloge) {
        return !finDuVerrouillage(derniereTentative).isAfter(LocalDateTime.now(horloge));
    }

    static int minutesRestantes(LocalDateTime derniereTentative, Clock horloge) {
        Duration restant = Duration.between(LocalDateTime.now(horloge),
                finDuVerrouillage(derniereTentative));
        // Arrondi au supérieur : « réessayez dans 0 minute » n'aide personne.
        long minutes = (restant.toSeconds() + 59) / 60;
        return (int) Math.max(0, minutes);
    }

    private static LocalDateTime finDuVerrouillage(LocalDateTime derniereTentative) {
        return derniereTentative.plusMinutes(RegistreTentatives.MINUTES_DE_VERROUILLAGE);
    }
}
