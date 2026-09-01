package com.example.bank.core.model.offre.pret.etat;

import com.example.bank.core.exception.OperationInterditeException;
import com.example.bank.core.model.offre.pret.Pret;

import java.math.BigDecimal;

/**
 * État d'un prêt : {@code EnAttente} -> {@code Approuvé} ->
 * {@code EnRemboursement} -> {@code Soldé} ou {@code EnDéfaut}.
 *
 * PATRON STATE — même principe que {@code EtatCompte} et {@code EtatCarte} :
 * chaque méthode par défaut refuse, un état n'autorise que ce qu'il
 * redéfinit. Le chemin nominal se lit donc dans les redéfinitions.
 */
public interface EtatPret {

    /** Libellé lisible, utilisé dans les messages d'erreur et par l'IHM. */
    String libelle();

    /**
     * Mensualité EXIGIBLE, c'est-à-dire réellement due par l'emprunteur.
     *
     * À distinguer de {@code Pret.calculerMensualite()}, qui applique le
     * barème et reste disponible à tout moment : on doit pouvoir simuler la
     * mensualité d'un prêt AVANT de l'accorder, c'est même à ça que sert une
     * demande de prêt. Ici, seuls {@code Approuvé} et {@code EnRemboursement}
     * répondent : un prêt en attente n'engage à rien, un prêt soldé ou en
     * défaut ne suit plus l'échéancier.
     */
    default BigDecimal mensualiteExigible(Pret pret) {
        throw OperationInterditeException.operation("mensualité exigible", libelle());
    }

    /** Transition : accord de la banque sur la demande. */
    default EtatPret approuver() {
        throw OperationInterditeException.transition("approbation", libelle());
    }

    /** Transition : déblocage des fonds, l'échéancier démarre. */
    default EtatPret demarrerRemboursement() {
        throw OperationInterditeException.transition("mise en remboursement", libelle());
    }

    /** Transition : dernière mensualité payée. État terminal. */
    default EtatPret solder() {
        throw OperationInterditeException.transition("solde", libelle());
    }

    /** Transition : constat d'impayé, déclenché manuellement. État terminal. */
    default EtatPret declarerDefaut() {
        throw OperationInterditeException.transition("mise en défaut", libelle());
    }
}
