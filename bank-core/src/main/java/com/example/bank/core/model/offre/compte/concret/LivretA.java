package com.example.bank.core.model.offre.compte.concret;

import com.example.bank.core.model.offre.compte.CompteBase;
import com.example.bank.core.model.offre.taux.TauxInteretStrategy;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Livret A : compte d'épargne rémunéré, sans découvert.
 *
 * C'est le compte qui donne enfin un point d'ancrage au patron Strategy — la
 * {@link TauxInteretStrategy} du tier lui est INJECTÉE à la construction, si
 * bien qu'un Livret A ouvert dans l'offre Étudiante est rémunéré à 2 %, le
 * même livret dans l'offre Premium à 4,50 %, sans qu'une seule ligne de cette
 * classe ne change.
 *
 * AUCUN DÉCOUVERT — un livret d'épargne ne se met pas à découvert : on ne peut
 * pas retirer plus que ce qu'on y a placé. Le plafond de 0 € est repris de la
 * même mécanique que {@code CompteEtudiant}.
 *
 * Le versement des intérêts n'est PAS implémenté ici : le taux est exposé,
 * son application périodique (et le plafond réglementaire de 22 950 €)
 * relèvent d'un chantier ultérieur.
 */
public class LivretA extends CompteBase {

    /** Un livret d'épargne ne peut jamais passer sous zéro. */
    private static final BigDecimal DECOUVERT_AUTORISE = new BigDecimal("0.00");

    private final TauxInteretStrategy tauxInteret;

    public LivretA(TauxInteretStrategy tauxInteret) {
        this.tauxInteret = Objects.requireNonNull(tauxInteret,
                "La stratégie de taux est obligatoire : un Livret A est toujours rémunéré.");
    }

    @Override
    public BigDecimal getDecouvertAutorise() {
        return DECOUVERT_AUTORISE;
    }

    /** Un Livret A est de l'épargne : c'est ce qui le distingue du compte courant. */
    @Override
    public boolean estEpargne() {
        return true;
    }

    /** Taux ANNUEL de rémunération, en fraction décimale ({@code 0.0300} = 3,00 %). */
    public BigDecimal getTauxInteret() {
        return tauxInteret.getTaux();
    }

    /** La stratégie injectée, pour qui veut la relire ou la comparer. */
    public TauxInteretStrategy getStrategieTaux() {
        return tauxInteret;
    }
}
