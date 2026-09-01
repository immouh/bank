package com.example.bank.core.model.offre.compte.etat.concret;

import com.example.bank.core.model.offre.compte.Compte;
import com.example.bank.core.model.offre.compte.etat.EtatCompte;

import java.math.BigDecimal;

/**
 * Compte au solde négatif, mais dans la limite du découvert autorisé de son
 * tier ({@code CompteEtudiant} 0 €, {@code CompteStandard} 300 €,
 * {@code ComptePremium} 2 000 €).
 *
 * Le plafond n'est PAS redéfini ici : il est lu sur le compte via
 * {@link Compte#getDecouvertAutorise()}, et la règle « un débit ne peut pas
 * aggraver le découvert au-delà du plafond » est déjà celle de
 * {@link Compte#soldeApresDebit} — inutile de l'écrire une seconde fois, il
 * suffit de l'appeler.
 */
public class CompteEnDecouvert implements EtatCompte {

    @Override
    public String libelle() {
        return "compte en découvert";
    }

    @Override
    public boolean autoriseOperations() {
        return true;
    }

    @Override
    public BigDecimal crediter(Compte compte, BigDecimal montant) {
        return Compte.soldeApresCredit(compte.getSolde(), montant);
    }

    @Override
    public BigDecimal debiter(Compte compte, BigDecimal montant) {
        return Compte.soldeApresDebit(compte.getSolde(), montant, compte.getDecouvertAutorise());
    }

    @Override
    public BigDecimal crediterRegularisation(Compte compte, BigDecimal montant) {
        return crediter(compte, montant);
    }

    /** Le retour à un solde positif ou nul sort automatiquement du découvert. */
    @Override
    public EtatCompte apresVariationDeSolde(BigDecimal nouveauSolde) {
        return nouveauSolde.signum() >= 0 ? new CompteActif() : this;
    }

    @Override
    public EtatCompte bloquer() {
        return new CompteBloque();
    }

    @Override
    public EtatCompte fermer() {
        return new CompteFerme();
    }
}
