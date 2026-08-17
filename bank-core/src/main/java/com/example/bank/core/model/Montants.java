package com.example.bank.core.model;

import com.example.bank.core.exception.MontantInvalideException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Règles communes à tout montant monétaire : deux décimales, arrondi bancaire,
 * et refus des montants nuls ou négatifs.
 *
 * Centralisé ici pour que le modèle et le service appliquent exactement la
 * même normalisation — sinon un service pourrait enregistrer une transaction
 * de 10,004 € alors que le compte a été débité de 10,00 €.
 */
public final class Montants {

    /** Nombre de décimales d'un montant monétaire. */
    public static final int ECHELLE = 2;
    /** Arrondi bancaire, obligatoire pour les opérations sur BigDecimal. */
    public static final RoundingMode ARRONDI = RoundingMode.HALF_EVEN;
    /** Montant nul normalisé, utilisé pour initialiser les soldes. */
    public static final BigDecimal ZERO = normaliser(BigDecimal.ZERO);

    private Montants() {
    }

    public static BigDecimal normaliser(BigDecimal montant) {
        return montant.setScale(ECHELLE, ARRONDI);
    }

    /** Normalise puis refuse tout montant nul ou négatif. */
    public static BigDecimal exigerPositif(BigDecimal montant) {
        if (montant == null) {
            throw new MontantInvalideException("Le montant est obligatoire.");
        }
        BigDecimal m = normaliser(montant);
        if (m.signum() <= 0) {
            throw new MontantInvalideException("Le montant doit être strictement positif.");
        }
        return m;
    }
}
