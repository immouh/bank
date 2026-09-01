package com.example.bank.core.model.offre.pret.concret;

import com.example.bank.core.model.offre.pret.Pret;
import com.example.bank.core.model.offre.pret.PretBase;

import java.math.BigDecimal;

/**
 * Prêt de l'offre Standard : prêt personnel à la consommation, durée courte.
 *
 * VALEURS D'EXEMPLE, à ajuster selon le barème réel.
 */
public class PretPersonnel extends PretBase {

    /** 4,50 % annuel. */
    private static final BigDecimal TAUX_ANNUEL = new BigDecimal("0.0450");
    private static final int DUREE_MOIS = 48;

    /** Montant type d'un prêt personnel. VALEUR D'EXEMPLE. */
    private static final BigDecimal MONTANT_PAR_DEFAUT = new BigDecimal("15000.00");

    /** Montant par défaut du tier, utilisé par {@code OffreFactory.creerPret()}. */
    public PretPersonnel() {
        this(MONTANT_PAR_DEFAUT);
    }

    public PretPersonnel(BigDecimal montantEmprunte) {
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
