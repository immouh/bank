package com.example.bank;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class transaction {
    private float montant ;
    private TypeTransaction type;
    private String descriptions;

    public transaction(float montant, TypeTransaction type, String descriptions) {
        this.montant = montant;
        LocalDate localDateTime;
   // Obtient la date et l'heure actuelles
        this.type = type;
        this.descriptions = descriptions;
    }
    @Override
    public String toString() {
        return "Transaction - Montant: " + montant + ", Type: " + type +  "  Description: " + descriptions+ "";
    }
    public enum TypeTransaction{
        DEPOT,
        RETRAIT,
        VIREMENT,
    }
}
