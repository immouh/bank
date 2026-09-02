package com.example.bank.core.exception.existence;

import com.example.bank.core.exception.BanqueException;

/**
 * Ouverture d'un second Livret A pour un client qui en a déjà un.
 *
 * Un client n'a droit qu'à un seul Livret A — c'est une règle MÉTIER, pas une
 * erreur de programmation. Elle appartient donc à la hiérarchie
 * {@link BanqueException}, que l'IHM attrape déjà pour afficher un message à
 * l'utilisateur. Avant, {@code Client.ouvrirLivretA} levait une
 * {@code IllegalStateException} : l'unique {@code catch (BanqueException)} des
 * fenêtres la laissait passer, et l'utilisateur voyait une erreur technique.
 */
public class LivretADejaExistantException extends ErreurExistenceException {

    public LivretADejaExistantException() {
        super("Vous possédez déjà un Livret A.");
    }
}
