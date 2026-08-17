package com.example.bank.core.model.offre.concret;

import com.example.bank.core.model.offre.OffreFactory;
import com.example.bank.core.model.offre.carte.concret.CarteBlack;
import com.example.bank.core.model.offre.compte.Compte;
import com.example.bank.core.model.offre.compte.concret.ComptePremium;
import com.example.bank.core.model.offre.pret.concret.PretImmobilier;
import com.example.bank.core.model.offre.taux.concret.TauxLivretAPremium;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

@DisplayName("Fabrique de l'offre Premium")
class OffrePremiumFactoryTest {

    private final OffreFactory factory = new OffrePremiumFactory();

    @Test
    @DisplayName("creerCompte() renvoie un ComptePremium")
    void creerCompteRenvoieLeTypeDuTier() {
        assertInstanceOf(ComptePremium.class, factory.creerCompte());
    }

    @Test
    @DisplayName("creerCarte() renvoie une CarteBlack")
    void creerCarteRenvoieLeTypeDuTier() {
        assertInstanceOf(CarteBlack.class, factory.creerCarte());
    }

    @Test
    @DisplayName("creerPret() renvoie un PretImmobilier")
    void creerPretRenvoieLeTypeDuTier() {
        assertInstanceOf(PretImmobilier.class, factory.creerPret());
    }

    @Test
    @DisplayName("creerStrategieTaux() renvoie un TauxLivretAPremium")
    void creerStrategieTauxRenvoieLeTypeDuTier() {
        assertInstanceOf(TauxLivretAPremium.class, factory.creerStrategieTaux());
    }

    @Test
    @DisplayName("Deux appels à creerCompte() renvoient deux instances distinctes")
    void chaqueAppelCreeUnNouveauCompte() {
        // COMPORTEMENT ATTENDU : une nouvelle instance à chaque appel, PAS un
        // singleton partagé. Un Compte porte un solde mutable : deux clients
        // qui partageraient la même instance partageraient leur argent.
        Compte premier = factory.creerCompte();
        Compte second = factory.creerCompte();

        assertNotSame(premier, second);
    }
}
