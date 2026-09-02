package com.example.bank.core.exception.technique;

/**
 * Un invariant du modèle est violé : l'objet est dans un état que le code
 * était censé rendre impossible.
 *
 * Exemple : un {@code Client} sans compte courant, alors que le constructeur
 * en crée toujours un et qu'aucune méthode ne le retire. Si cela arrive,
 * c'est un BUG, pas une situation métier — mais il doit tout de même
 * atteindre l'utilisateur par le chemin normal des erreurs plutôt que par une
 * {@code IllegalStateException} que l'IHM laisserait filer.
 */
public class InvariantRompuException extends ErreurTechniqueException {

    public InvariantRompuException(String message) {
        super(message);
    }
}
