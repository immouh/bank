package com.example.bank.core.model.offre.carte.etat.concret;

import com.example.bank.core.exception.etat.PlafondDepasseException;
import com.example.bank.core.model.Montants;
import com.example.bank.core.model.offre.carte.CarteBancaire;
import com.example.bank.core.model.offre.carte.etat.EtatCarte;

import java.math.BigDecimal;

/** Carte en service : les paiements passent, dans la limite du plafond du tier. */
public class CarteActive implements EtatCarte {

    @Override
    public String libelle() {
        return "carte active";
    }

    @Override
    public boolean estActive() {
        return true;
    }

    /**
     * Le plafond n'est pas redéfini ici : il est lu sur la carte, donc celui
     * de son tier (500 € Jeune, 1 500 € Classique, 10 000 € Black).
     */
    @Override
    public void autoriserPaiement(CarteBancaire carte, BigDecimal montant) {
        BigDecimal m = Montants.exigerPositif(montant);
        if (m.compareTo(carte.getPlafond()) > 0) {
            throw new PlafondDepasseException(carte.getPlafond(), m);
        }
    }

    @Override
    public EtatCarte bloquer() {
        return new CarteBloquee();
    }

    /** Déjà active : réactiver ne fait rien plutôt que de lever une exception. */
    @Override
    public EtatCarte activer() {
        return this;
    }

    @Override
    public EtatCarte expirer() {
        return new CarteExpiree();
    }
}
