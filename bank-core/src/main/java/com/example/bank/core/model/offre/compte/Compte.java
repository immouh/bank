package com.example.bank.core.model.offre.compte;

import com.example.bank.core.exception.SoldeInsuffisantException;
import com.example.bank.core.model.Montants;

import java.math.BigDecimal;

/**
 * Compte bancaire d'une offre. Chaque tier a sa propre implémentation, qui se
 * distingue par son découvert autorisé et ses frais de tenue de compte.
 *
 * Les règles de calcul ne sont PAS réinventées ici : elles reprennent celles
 * déjà appliquées par {@code Client} — montant strictement positif normalisé à
 * deux décimales via {@link Montants#exigerPositif(BigDecimal)}, et
 * {@link SoldeInsuffisantException} quand le débit dépasse ce qui est
 * disponible. Les deux méthodes statiques ci-dessous les factorisent pour que
 * les trois implémentations ne puissent pas diverger.
 */
public interface Compte {

    BigDecimal getSolde();

    void crediter(BigDecimal montant);

    void debiter(BigDecimal montant);

    /**
     * Règle de crédit commune : montant strictement positif, normalisé.
     *
     * @return le nouveau solde
     */
    static BigDecimal soldeApresCredit(BigDecimal solde, BigDecimal montant) {
        return solde.add(Montants.exigerPositif(montant));
    }

    /**
     * Règle de débit commune : montant strictement positif, et débit plafonné
     * par le solde augmenté du découvert autorisé du tier.
     *
     * @param decouvertAutorise montant positif ou nul dont le solde peut descendre
     *                          sous zéro (0 = aucun découvert toléré)
     * @return le nouveau solde
     * @throws SoldeInsuffisantException si le débit dépasse le disponible
     */
    static BigDecimal soldeApresDebit(BigDecimal solde, BigDecimal montant,
                                      BigDecimal decouvertAutorise) {
        BigDecimal m = Montants.exigerPositif(montant);
        BigDecimal disponible = solde.add(decouvertAutorise);
        if (disponible.compareTo(m) < 0) {
            throw new SoldeInsuffisantException(disponible, m);
        }
        return solde.subtract(m);
    }
}
