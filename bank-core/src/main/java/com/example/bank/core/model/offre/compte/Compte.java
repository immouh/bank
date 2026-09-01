package com.example.bank.core.model.offre.compte;

import com.example.bank.core.exception.SoldeInsuffisantException;
import com.example.bank.core.model.Montants;
import com.example.bank.core.model.offre.compte.etat.EtatCompte;

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
 *
 * CYCLE DE VIE — le compte délègue à son {@link EtatCompte} courant (patron
 * State) : c'est l'état qui accepte ou refuse crédit, débit et transitions.
 * Les implémentations passent par {@link CompteBase}, qui tient le solde et
 * l'état ; elles n'ont plus à porter que les constantes de leur tier.
 */
public interface Compte {

    BigDecimal getSolde();

    /**
     * Montant POSITIF dont le solde peut descendre sous zéro pour ce tier
     * (0 = aucun découvert toléré). Exposé sur l'interface parce que l'état
     * {@code EnDécouvert} en a besoin pour arbitrer un débit sans connaître
     * le tier concret.
     */
    BigDecimal getDecouvertAutorise();

    /** État courant du compte : actif, en découvert, bloqué ou fermé. */
    EtatCompte getEtat();

    void crediter(BigDecimal montant);

    void debiter(BigDecimal montant);

    /**
     * Crédit de régularisation à l'initiative de la banque : le seul mouvement
     * qu'un compte bloqué accepte. Sur un compte actif ou en découvert, c'est
     * un crédit ordinaire.
     */
    void crediterRegularisation(BigDecimal montant);

    /** Met le compte en opposition. Toute opération du client est alors refusée. */
    void bloquer();

    /** Lève le blocage. Le compte repart actif ou en découvert selon son solde. */
    void debloquer();

    /** Clôture définitive : état terminal, aucune opération ni retour possible. */
    void fermer();

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
