package com.example.bank.core.service.securite;

import com.example.bank.core.exception.etat.CompteVerouilleException;
import com.example.bank.core.model.Client;

/**
 * Deuxième étape : le compte est-il suspendu pour tentatives répétées ?
 *
 * PLACÉE AVANT LE MOT DE PASSE, VOLONTAIREMENT. Un compte verrouillé refuse
 * même une saisie correcte — c'est tout l'intérêt : si le bon mot de passe
 * ouvrait quand même, un attaquant qui finit par le trouver entrerait malgré
 * le verrouillage, et les trois essais n'auraient servi à rien. Cet ordre
 * évite aussi de brûler un BCrypt pour un compte de toute façon fermé.
 */
public class VerificationVerrouillage extends EtapeAuthentification {

    private final RegistreTentatives registre;

    public VerificationVerrouillage(RegistreTentatives registre) {
        this.registre = registre;
    }

    @Override
    protected void executer(Client client, String motDePasseSaisi) {
        int rib = client.getRib();
        if (registre.estVerrouille(rib)) {
            throw new CompteVerouilleException(registre.minutesRestantes(rib));
        }
    }
}
