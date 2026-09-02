package com.example.bank.core.exception.technique;

/**
 * Un paramètre interne attendu par un service est absent ou inexploitable.
 *
 * Catégorie TECHNIQUE et non validation : ce n'est pas l'utilisateur qui a
 * mal rempli un champ, c'est un appelant du cœur métier qui n'a pas respecté
 * le contrat d'une méthode — un mot de passe {@code null} envoyé au hachage,
 * par exemple. Le remonter sous {@link ErreurTechniqueException} évite qu'une
 * {@code IllegalArgumentException} traverse l'unique
 * {@code catch (BanqueException)} des fenêtres et remonte en trace nue.
 */
public class ParametreInvalideException extends ErreurTechniqueException {

    public ParametreInvalideException(String message) {
        super(message);
    }
}
