package com.example.bank.core.model.offre.pret.concret;

import com.example.bank.core.model.offre.pret.Pret;
import com.example.bank.core.model.offre.pret.PretBase;

import java.math.BigDecimal;

/**
 * Prêt de l'offre Premium : prêt immobilier, montant élevé sur longue durée.
 *
 * VALEURS D'EXEMPLE, à ajuster selon le barème réel.
 */
public class PretImmobilier extends PretBase {

    /** 3,20 % annuel. */
    private static final BigDecimal TAUX_ANNUEL = new BigDecimal("0.0320");
    private static final int DUREE_MOIS = 240; // 20 ans

    /** Montant type d'un prêt immobilier. VALEUR D'EXEMPLE. */
    private static final BigDecimal MONTANT_PAR_DEFAUT = new BigDecimal("200000.00");

    /** Montant par défaut du tier, utilisé par {@code OffreFactory.creerPret()}. */
    public PretImmobilier() {
        this(MONTANT_PAR_DEFAUT);
    }

    public PretImmobilier(BigDecimal montantEmprunte) {
        super(montantEmprunte);
    }

    @Override
    public BigDecimal getTaux() {
        return TAUX_ANNUEL;
    }

    @Override
    public BigDecimal calculerMensualite() {
        return Pret.mensualite(getMontantEmprunte(), TAUX_ANNUEL, DUREE_MOIS);
    }

    public int getDureeMois() {
        return DUREE_MOIS;
    }
}
