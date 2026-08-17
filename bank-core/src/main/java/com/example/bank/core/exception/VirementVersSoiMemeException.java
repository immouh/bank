package com.example.bank.core.exception;

/**
 * Virement dont l'émetteur et le destinataire sont le même compte courant.
 * Interdit : cela contournerait la règle « le Livret A s'alimente depuis
 * le compte courant, jamais l'inverse ».
 */
public class VirementVersSoiMemeException extends BanqueException {

    public VirementVersSoiMemeException() {
        super("Impossible de faire un virement vers votre propre compte courant.\n"
                + "Utilisez l'option « Vers mon Livret A » pour alimenter votre épargne.");
    }
}
