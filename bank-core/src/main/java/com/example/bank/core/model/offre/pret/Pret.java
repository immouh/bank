package com.example.bank.core.model.offre.pret;

import com.example.bank.core.model.Montants;
import com.example.bank.core.model.offre.pret.etat.EtatPret;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Prêt proposé par une offre. Chaque tier a sa propre implémentation, qui se
 * distingue par son taux nominal annuel et sa durée de remboursement.
 *
 * CYCLE DE VIE — le prêt délègue à son {@link EtatPret} courant (patron
 * State) : en attente, approuvé, en remboursement, puis soldé ou en défaut.
 * Les implémentations passent par {@link PretBase}, qui tient le montant
 * emprunté et l'état.
 */
public interface Pret {

    /**
     * Taux nominal ANNUEL, exprimé en fraction décimale :
     * {@code 0.0450} signifie 4,50 %.
     */
    BigDecimal getTaux();

    /** Capital emprunté, en euros. */
    BigDecimal getMontantEmprunte();

    /**
     * Mensualité constante selon le BARÈME du tier, arrondie au centime.
     *
     * Disponible dans tous les états, y compris {@code EnAttente} : c'est une
     * simulation, et simuler avant d'accorder est précisément ce que fait une
     * demande de prêt. Pour la mensualité réellement DUE, voir
     * {@link #mensualiteExigible()}, que l'état filtre.
     */
    BigDecimal calculerMensualite();

    /** État courant du prêt. */
    EtatPret getEtat();

    /**
     * Mensualité exigible de l'emprunteur : le barème, mais seulement à
     * partir de {@code Approuvé} et jusqu'à la fin du remboursement.
     */
    BigDecimal mensualiteExigible();

    /** Transition : la banque accorde le prêt. */
    void approuver();

    /** Transition : les fonds sont débloqués, l'échéancier démarre. */
    void demarrerRemboursement();

    /** Transition : dernière échéance payée. État terminal. */
    void solder();

    /** Transition : constat d'impayé, déclenché manuellement. État terminal. */
    void declarerDefaut();

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
