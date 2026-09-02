package com.example.bank.core.model.offre.pret;

import com.example.bank.core.exception.BanqueException;
import com.example.bank.core.exception.validation.DureeInvalideException;
import com.example.bank.core.exception.validation.ErreurValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * La garde de {@link Pret#mensualite(BigDecimal, BigDecimal, int)} sur la
 * durée.
 *
 * Une durée non positive ferait diviser le capital par zéro dans la branche à
 * taux nul, et élever {@code (1+t)} à une puissance négative dans l'autre. Le
 * refus est explicite, et depuis le regroupement des exceptions il descend de
 * {@link BanqueException} : une simulation lancée depuis l'IHM ne peut plus
 * remonter en trace technique.
 */
@DisplayName("Formule d'amortissement partagée par les prêts")
class PretTest {

    private static final BigDecimal CAPITAL = new BigDecimal("10000.00");
    private static final BigDecimal TAUX = new BigDecimal("0.0090");

    @Test
    @DisplayName("Une durée nulle est refusée")
    void dureeNulleRefusee() {
        assertThrows(DureeInvalideException.class, () -> Pret.mensualite(CAPITAL, TAUX, 0));
    }

    @Test
    @DisplayName("Une durée négative est refusée")
    void dureeNegativeRefusee() {
        assertThrows(DureeInvalideException.class, () -> Pret.mensualite(CAPITAL, TAUX, -12));
    }

    @Test
    @DisplayName("Le refus porte le message attendu")
    void messageDuRefus() {
        DureeInvalideException levee = assertThrows(DureeInvalideException.class,
                () -> Pret.mensualite(CAPITAL, TAUX, 0));

        assertEquals("La durée du prêt doit être strictement positive.", levee.getMessage());
    }

    @Test
    @DisplayName("Le refus est une erreur de validation, donc une BanqueException attrapable par l'IHM")
    void refusDansLaHierarchieMetier() {
        DureeInvalideException levee = assertThrows(DureeInvalideException.class,
                () -> Pret.mensualite(CAPITAL, TAUX, 0));

        assertInstanceOf(ErreurValidationException.class, levee);
        assertInstanceOf(BanqueException.class, levee);
    }

    @Test
    @DisplayName("Une durée strictement positive passe la garde")
    void dureePositiveAcceptee() {
        assertEquals(0, new BigDecimal("170.51").compareTo(Pret.mensualite(CAPITAL, TAUX, 60)));
    }
}
