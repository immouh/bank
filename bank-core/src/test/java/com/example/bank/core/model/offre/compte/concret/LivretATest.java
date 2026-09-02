package com.example.bank.core.model.offre.compte.concret;

import com.example.bank.core.exception.validation.MontantInvalideException;
import com.example.bank.core.exception.etat.SoldeInsuffisantException;
import com.example.bank.core.model.offre.OffreFactory;
import com.example.bank.core.model.offre.compte.Compte;
import com.example.bank.core.model.offre.concret.OffreEtudianteFactory;
import com.example.bank.core.model.offre.concret.OffrePremiumFactory;
import com.example.bank.core.model.offre.concret.OffreStandardFactory;
import com.example.bank.core.model.offre.taux.concret.TauxLivretAStandard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le Livret A : compte d'épargne rémunéré, sans découvert.
 *
 * CE QUI EST VÉRIFIÉ ICI — que la {@code TauxInteretStrategy} injectée est
 * bien celle qui décide de la rémunération. C'est le premier endroit du projet
 * où le patron Strategy sert réellement à quelque chose.
 *
 * COMPARAISON DES BigDecimal — {@code compareTo} et jamais {@code equals},
 * qui compare aussi l'échelle ("0.0300" != "0.03").
 */
@DisplayName("Livret A")
class LivretATest {

    @Nested
    @DisplayName("Rémunération injectée")
    class Remuneration {

        @Test
        @DisplayName("TIER Étudiante — le livret est rémunéré à 2,00 %")
        void tauxDuTierEtudiant() {
            LivretA livret = livretDe(new OffreEtudianteFactory());

            assertEquals(0, new BigDecimal("0.0200").compareTo(livret.getTauxInteret()));
        }

        @Test
        @DisplayName("TIER Standard — le livret est rémunéré à 3,00 %")
        void tauxDuTierStandard() {
            LivretA livret = livretDe(new OffreStandardFactory());

            assertEquals(0, new BigDecimal("0.0300").compareTo(livret.getTauxInteret()));
        }

        @Test
        @DisplayName("TIER Premium — le livret est rémunéré à 4,50 %")
        void tauxDuTierPremium() {
            LivretA livret = livretDe(new OffrePremiumFactory());

            assertEquals(0, new BigDecimal("0.0450").compareTo(livret.getTauxInteret()));
        }

        @Test
        @DisplayName("HIÉRARCHIE — Étudiante < Standard < Premium, un même livret change de rendement selon l'offre")
        void hierarchieDesTaux() {
            BigDecimal etudiante = livretDe(new OffreEtudianteFactory()).getTauxInteret();
            BigDecimal standard = livretDe(new OffreStandardFactory()).getTauxInteret();
            BigDecimal premium = livretDe(new OffrePremiumFactory()).getTauxInteret();

            assertEquals(-1, etudiante.compareTo(standard));
            assertEquals(-1, standard.compareTo(premium));
        }

        @Test
        @DisplayName("La stratégie injectée reste consultable telle quelle")
        void strategieConsultable() {
            LivretA livret = new LivretA(new TauxLivretAStandard());

            assertTrue(livret.getStrategieTaux() instanceof TauxLivretAStandard);
        }

        @Test
        @DisplayName("Un Livret A sans stratégie de taux est refusé : un livret est toujours rémunéré")
        void strategieObligatoire() {
            assertThrows(NullPointerException.class, () -> new LivretA(null));
        }

        private LivretA livretDe(OffreFactory offre) {
            return new LivretA(offre.creerStrategieTaux());
        }
    }

    @Nested
    @DisplayName("Mouvements")
    class Mouvements {

        private final LivretA livret = new LivretA(new TauxLivretAStandard());

        @Test
        @DisplayName("Un livret neuf démarre à zéro")
        void livretNeufAZero() {
            assertEquals(0, new BigDecimal("0.00").compareTo(livret.getSolde()));
        }

        @Test
        @DisplayName("Un versement augmente le solde du montant exact")
        void versementAugmenteLeSolde() {
            livret.crediter(new BigDecimal("1000.00"));

            assertEquals(0, new BigDecimal("1000.00").compareTo(livret.getSolde()));
        }

        @Test
        @DisplayName("Un retrait dans la limite du solde est accepté")
        void retraitDansLaLimiteAccepte() {
            livret.crediter(new BigDecimal("1000.00"));

            livret.debiter(new BigDecimal("400.00"));

            assertEquals(0, new BigDecimal("600.00").compareTo(livret.getSolde()));
        }

        @Test
        @DisplayName("Aucun découvert n'est autorisé sur un livret d'épargne")
        void aucunDecouvertAutorise() {
            assertEquals(0, new BigDecimal("0.00").compareTo(livret.getDecouvertAutorise()));
        }

        @Test
        @DisplayName("Un retrait d'un centime au-delà du solde est refusé")
        void retraitAuDelaDuSoldeRefuse() {
            livret.crediter(new BigDecimal("100.00"));

            assertThrows(SoldeInsuffisantException.class,
                    () -> livret.debiter(new BigDecimal("100.01")));
        }

        @Test
        @DisplayName("Un versement nul est refusé")
        void versementNulRefuse() {
            assertThrows(MontantInvalideException.class, () -> livret.crediter(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Un versement négatif est refusé")
        void versementNegatifRefuse() {
            assertThrows(MontantInvalideException.class,
                    () -> livret.crediter(new BigDecimal("-100.00")));
        }
    }

    @Nested
    @DisplayName("Nature du compte")
    class Nature {

        @Test
        @DisplayName("Un Livret A est un compte d'épargne")
        void estUnCompteDEpargne() {
            assertTrue(new LivretA(new TauxLivretAStandard()).estEpargne());
        }

        @Test
        @DisplayName("Un compte courant n'est pas un compte d'épargne")
        void compteCourantNEstPasDeLEpargne() {
            assertFalse(new CompteStandard().estEpargne());
        }

        @Test
        @DisplayName("Un Livret A hérite du cycle de vie des comptes : il peut être bloqué")
        void heriteDuCycleDeVie() {
            Compte livret = new LivretA(new TauxLivretAStandard());
            livret.crediter(new BigDecimal("100.00"));

            livret.bloquer();

            assertFalse(livret.getEtat().autoriseOperations());
        }
    }
}
