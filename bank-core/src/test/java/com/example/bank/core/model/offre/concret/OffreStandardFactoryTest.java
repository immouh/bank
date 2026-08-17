package com.example.bank.core.model.offre.concret;

import com.example.bank.core.model.offre.OffreFactory;
import com.example.bank.core.model.offre.carte.concret.CarteClassique;
import com.example.bank.core.model.offre.compte.Compte;
import com.example.bank.core.model.offre.compte.concret.CompteStandard;
import com.example.bank.core.model.offre.pret.concret.PretPersonnel;
import com.example.bank.core.model.offre.taux.concret.TauxLivretAStandard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

@DisplayName("Fabrique de l'offre Standard")
class OffreStandardFactoryTest {

    private final OffreFactory factory = new OffreStandardFactory();

    @Test
    @DisplayName("creerCompte() renvoie un CompteStandard")
    void creerCompteRenvoieLeTypeDuTier() {
        assertInstanceOf(CompteStandard.class, factory.creerCompte());
    }

    @Test
    @DisplayName("creerCarte() renvoie une CarteClassique")
    void creerCarteRenvoieLeTypeDuTier() {
        assertInstanceOf(CarteClassique.class, factory.creerCarte());
    }

    @Test
    @DisplayName("creerPret() renvoie un PretPersonnel")
    void creerPretRenvoieLeTypeDuTier() {
        assertInstanceOf(PretPersonnel.class, factory.creerPret());
    }

    @Test
    @DisplayName("creerStrategieTaux() renvoie un TauxLivretAStandard")
    void creerStrategieTauxRenvoieLeTypeDuTier() {
        assertInstanceOf(TauxLivretAStandard.class, factory.creerStrategieTaux());
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
