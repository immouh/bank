package com.example.bank.core.model.offre;

import com.example.bank.core.model.offre.carte.CarteBancaire;
import com.example.bank.core.model.offre.compte.Compte;
import com.example.bank.core.model.offre.pret.Pret;
import com.example.bank.core.model.offre.taux.TauxInteretStrategy;

/**
 * Patron Abstract Factory : produit une famille cohérente de produits
 * bancaires (compte, carte, prêt, stratégie de taux) appartenant tous au même
 * tier commercial.
 *
 * L'intérêt est de rendre les combinaisons incohérentes impossibles : on ne
 * peut pas obtenir une CarteBlack avec un CompteEtudiant, parce qu'aucune
 * fabrique ne les produit ensemble.
 *
 * <p>Exemple d'utilisation :</p>
 * <pre>{@code
 * OffreFactory factory = new OffreEtudianteFactory();
 *
 * Compte compte = factory.creerCompte();                  // CompteEtudiant
 * CarteBancaire carte = factory.creerCarte();             // CarteJeune
 * Pret pret = factory.creerPret();                        // PretEtudiant
 * TauxInteretStrategy taux = factory.creerStrategieTaux(); // TauxLivretAEtudiant
 *
 * // Changer d'offre revient à changer la seule ligne de construction :
 * // OffreFactory factory = new OffrePremiumFactory();
 * // le reste du code, qui ne manipule que les interfaces, est inchangé.
 * }</pre>
 */
public interface OffreFactory {

    Compte creerCompte();

    CarteBancaire creerCarte();

    Pret creerPret();

    TauxInteretStrategy creerStrategieTaux();
}
