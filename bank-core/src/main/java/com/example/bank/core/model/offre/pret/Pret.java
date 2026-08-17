package com.example.bank.core.model.offre.pret;

import com.example.bank.core.model.Montants;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Prêt proposé par une offre. Chaque tier a sa propre implémentation, qui se
 * distingue par son taux nominal annuel et sa durée de remboursement.
 */
public interface Pret {

    /**
     * Taux nominal ANNUEL, exprimé en fraction décimale :
     * {@code 0.0450} signifie 4,50 %.
     */
    BigDecimal getTaux();

    /** Capital emprunté, en euros. */
    BigDecimal getMontantEmprunte();

    /** Mensualité constante, arrondie au centime. */
    BigDecimal calculerMensualite();

    /**
     * Formule d'amortissement constant, factorisée pour que les trois prêts
     * la partagent au lieu de la recopier :
     *
     * <pre>M = C x t x (1+t)^n / ((1+t)^n - 1)</pre>
     *
     * où {@code t} est le taux mensuel et {@code n} le nombre de mensualités.
     * Un taux nul dégénère en simple division du capital par la durée.
     *
     * @param capital     capital emprunté
     * @param tauxAnnuel  taux nominal annuel en fraction décimale
     * @param dureeMois   nombre de mensualités, strictement positif
     */
    static BigDecimal mensualite(BigDecimal capital, BigDecimal tauxAnnuel, int dureeMois) {
        if (dureeMois <= 0) {
            throw new IllegalArgumentException("La durée du prêt doit être strictement positive.");
        }
        BigDecimal c = Montants.normaliser(capital);

        if (tauxAnnuel.signum() == 0) {
            return c.divide(BigDecimal.valueOf(dureeMois), Montants.ECHELLE, Montants.ARRONDI);
        }

        // Précision intermédiaire large : l'arrondi au centime n'intervient
        // qu'une fois, sur le résultat final.
        MathContext mc = new MathContext(16, RoundingMode.HALF_EVEN);
        BigDecimal tauxMensuel = tauxAnnuel.divide(BigDecimal.valueOf(12), mc);
        BigDecimal facteur = BigDecimal.ONE.add(tauxMensuel).pow(dureeMois, mc);

        BigDecimal mensualite = c.multiply(tauxMensuel).multiply(facteur)
                .divide(facteur.subtract(BigDecimal.ONE), mc);
        return Montants.normaliser(mensualite);
    }
}
