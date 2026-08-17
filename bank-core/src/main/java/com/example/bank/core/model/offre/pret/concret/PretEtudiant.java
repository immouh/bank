package com.example.bank.core.model.offre.pret.concret;

import com.example.bank.core.model.Montants;
import com.example.bank.core.model.offre.pret.Pret;

import java.math.BigDecimal;

/**
 * Prêt de l'offre Étudiante : taux bonifié, durée longue.
 *
 * VALEURS D'EXEMPLE, à ajuster selon le barème réel.
 */
public class PretEtudiant implements Pret {

    /** 0,90 % annuel, taux bonifié réservé aux étudiants. */
    private static final BigDecimal TAUX_ANNUEL = new BigDecimal("0.0090");
    private static final int DUREE_MOIS = 60;

    /** Montant type d'un prêt étudiant. VALEUR D'EXEMPLE. */
    private static final BigDecimal MONTANT_PAR_DEFAUT = new BigDecimal("10000.00");

    private final BigDecimal montantEmprunte;

    /** Montant par défaut du tier, utilisé par {@code OffreFactory.creerPret()}. */
    public PretEtudiant() {
        this(MONTANT_PAR_DEFAUT);
    }

    public PretEtudiant(BigDecimal montantEmprunte) {
        this.montantEmprunte = Montants.exigerPositif(montantEmprunte);
    }

    @Override
    public BigDecimal getTaux() {
        return TAUX_ANNUEL;
    }

    @Override
    public BigDecimal getMontantEmprunte() {
        return montantEmprunte;
    }

    @Override
    public BigDecimal calculerMensualite() {
        return Pret.mensualite(montantEmprunte, TAUX_ANNUEL, DUREE_MOIS);
    }

    public int getDureeMois() {
        return DUREE_MOIS;
    }
}
