package com.example.bank.core.model.offre.pret;

import com.example.bank.core.model.Montants;
import com.example.bank.core.model.offre.pret.etat.EtatPret;
import com.example.bank.core.model.offre.pret.etat.concret.PretEnAttente;

import java.math.BigDecimal;

/**
 * Socle commun aux prêts des trois tiers : il détient le montant emprunté et
 * l'état, et délègue les transitions à l'état courant. Les implémentations
 * concrètes ne portent plus que leur taux, leur durée et leur montant type.
 *
 * Tout prêt naît {@code EnAttente} : rien n'est accordé tant que la banque
 * n'a pas instruit la demande.
 */
public abstract class PretBase implements Pret {

    private final BigDecimal montantEmprunte;
    private EtatPret etat = new PretEnAttente();

    protected PretBase(BigDecimal montantEmprunte) {
        this.montantEmprunte = Montants.exigerPositif(montantEmprunte);
    }

    @Override
    public BigDecimal getMontantEmprunte() {
        return montantEmprunte;
    }

    @Override
    public EtatPret getEtat() {
        return etat;
    }

    @Override
    public BigDecimal mensualiteExigible() {
        return etat.mensualiteExigible(this);
    }

    @Override
    public void approuver() {
        etat = etat.approuver();
    }

    @Override
    public void demarrerRemboursement() {
        etat = etat.demarrerRemboursement();
    }

    @Override
    public void solder() {
        etat = etat.solder();
    }

    @Override
    public void declarerDefaut() {
        etat = etat.declarerDefaut();
    }
}
