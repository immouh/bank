package com.example.bank.core.model.offre.concret;

import com.example.bank.core.model.offre.OffreFactory;
import com.example.bank.core.model.offre.carte.concret.CarteJeune;
import com.example.bank.core.model.offre.compte.Compte;
import com.example.bank.core.model.offre.compte.concret.CompteEtudiant;
import com.example.bank.core.model.offre.pret.concret.PretEtudiant;
import com.example.bank.core.model.offre.taux.concret.TauxLivretAEtudiant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

@DisplayName("Fabrique de l'offre Étudiante")
class OffreEtudianteFactoryTest {

    private final OffreFactory factory = new OffreEtudianteFactory();

    @Test
    @DisplayName("creerCompte() renvoie un CompteEtudiant")
    void creerCompteRenvoieLeTypeDuTier() {
        assertInstanceOf(CompteEtudiant.class, factory.creerCompte());
    }

    @Test
    @DisplayName("creerCarte() renvoie une CarteJeune")
    void creerCarteRenvoieLeTypeDuTier() {
        assertInstanceOf(CarteJeune.class, factory.creerCarte());
    }

    @Test
    @DisplayName("creerPret() renvoie un PretEtudiant")
    void creerPretRenvoieLeTypeDuTier() {
        assertInstanceOf(PretEtudiant.class, factory.creerPret());
    }

    @Test
    @DisplayName("creerStrategieTaux() renvoie un TauxLivretAEtudiant")
    void creerStrategieTauxRenvoieLeTypeDuTier() {
        assertInstanceOf(TauxLivretAEtudiant.class, factory.creerStrategieTaux());
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
