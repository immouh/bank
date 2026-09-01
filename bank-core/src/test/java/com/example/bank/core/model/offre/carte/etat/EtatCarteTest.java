package com.example.bank.core.model.offre.carte.etat;

import com.example.bank.core.exception.MontantInvalideException;
import com.example.bank.core.exception.OperationInterditeException;
import com.example.bank.core.exception.PlafondDepasseException;
import com.example.bank.core.model.offre.carte.CarteBancaire;
import com.example.bank.core.model.offre.carte.concret.CarteBlack;
import com.example.bank.core.model.offre.carte.concret.CarteJeune;
import com.example.bank.core.model.offre.carte.etat.concret.CarteActive;
import com.example.bank.core.model.offre.carte.etat.concret.CarteBloquee;
import com.example.bank.core.model.offre.carte.etat.concret.CarteExpiree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Machine à états de la carte : Active / Bloquée / Expirée.
 *
 * La CarteJeune sert de support (plafond 500 €) : c'est le plus bas des trois
 * tiers, donc le plus commode pour observer un refus de plafond.
 */
@DisplayName("État d'une carte")
class EtatCarteTest {

    private CarteBancaire carte;

    @BeforeEach
    void creerCarte() {
        carte = new CarteJeune();
    }

    @Nested
    @DisplayName("Active")
    class Active {

        @Test
        @DisplayName("Une carte neuve est active")
        void carteNeuveEstActive() {
            assertInstanceOf(CarteActive.class, carte.getEtat());
            assertTrue(carte.estActive());
        }

        @Test
        @DisplayName("Un paiement sous le plafond est autorisé")
        void paiementSousLePlafondAutorise() {
            assertDoesNotThrow(() -> carte.autoriserPaiement(new BigDecimal("499.99")));
        }

        @Test
        @DisplayName("Un paiement égal au plafond est autorisé")
        void paiementEgalAuPlafondAutorise() {
            assertDoesNotThrow(() -> carte.autoriserPaiement(new BigDecimal("500.00")));
        }

        @Test
        @DisplayName("TIER — un paiement au-dessus du plafond de 500 € est refusé")
        void paiementAuDessusDuPlafondRefuse() {
            assertThrows(PlafondDepasseException.class,
                    () -> carte.autoriserPaiement(new BigDecimal("500.01")));
        }

        @Test
        @DisplayName("TIER — le même montant passe sur une carte Black, dont le plafond est de 10000 €")
        void memeMontantAcceptEParLeTierPremium() {
            assertDoesNotThrow(() -> new CarteBlack().autoriserPaiement(new BigDecimal("500.01")));
        }

        @Test
        @DisplayName("Un paiement d'un montant nul est refusé")
        void paiementNulRefuse() {
            assertThrows(MontantInvalideException.class,
                    () -> carte.autoriserPaiement(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Un paiement d'un montant négatif est refusé")
        void paiementNegatifRefuse() {
            assertThrows(MontantInvalideException.class,
                    () -> carte.autoriserPaiement(new BigDecimal("-10.00")));
        }

        @Test
        @DisplayName("Réactiver une carte déjà active ne change rien")
        void reactiverUneCarteActiveNeChangeRien() {
            carte.activer();

            assertInstanceOf(CarteActive.class, carte.getEtat());
        }
    }

    @Nested
    @DisplayName("Bloquée")
    class Bloquee {

        @BeforeEach
        void bloquerLaCarte() {
            carte.bloquer();
        }

        @Test
        @DisplayName("Une carte bloquée n'est plus active")
        void carteBloqueeNEstPlusActive() {
            assertInstanceOf(CarteBloquee.class, carte.getEtat());
            assertFalse(carte.estActive());
        }

        @Test
        @DisplayName("Une carte bloquée refuse un paiement, même très inférieur au plafond")
        void carteBloqueeRefuseLePaiement() {
            assertThrows(OperationInterditeException.class,
                    () -> carte.autoriserPaiement(new BigDecimal("1.00")));
        }

        @Test
        @DisplayName("Le blocage est réversible : l'activation remet la carte en service")
        void blocageReversible() {
            carte.activer();

            assertTrue(carte.estActive());
            assertDoesNotThrow(() -> carte.autoriserPaiement(new BigDecimal("100.00")));
        }

        @Test
        @DisplayName("Une carte bloquée ne peut pas être bloquée une seconde fois")
        void doubleBlocageRefuse() {
            assertThrows(OperationInterditeException.class, () -> carte.bloquer());
        }

        @Test
        @DisplayName("Une carte bloquée peut arriver à expiration")
        void carteBloqueePeutExpirer() {
            carte.expirer();

            assertInstanceOf(CarteExpiree.class, carte.getEtat());
        }
    }

    @Nested
    @DisplayName("Expirée — état terminal")
    class Expiree {

        @BeforeEach
        void expirerLaCarte() {
            carte.expirer();
        }

        @Test
        @DisplayName("Une carte expirée n'est plus active")
        void carteExpireeNEstPlusActive() {
            assertInstanceOf(CarteExpiree.class, carte.getEtat());
            assertFalse(carte.estActive());
        }

        @Test
        @DisplayName("Une carte expirée refuse tout paiement")
        void carteExpireeRefuseLePaiement() {
            assertThrows(OperationInterditeException.class,
                    () -> carte.autoriserPaiement(new BigDecimal("10.00")));
        }

        @Test
        @DisplayName("Pas de retour en arrière : une carte expirée ne se réactive pas")
        void carteExpireeNeSeReactivePas() {
            assertThrows(OperationInterditeException.class, () -> carte.activer());
        }

        @Test
        @DisplayName("Une carte expirée ne se bloque pas : il n'y a plus rien à bloquer")
        void carteExpireeNeSeBloquePas() {
            assertThrows(OperationInterditeException.class, () -> carte.bloquer());
        }

        @Test
        @DisplayName("Une carte expirée ne peut pas expirer une seconde fois")
        void doubleExpirationRefusee() {
            assertThrows(OperationInterditeException.class, () -> carte.expirer());
        }
    }
}
