package com.example.bank.core.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Une opération enregistrée dans l'historique d'un client.
 * La classe est immuable : une transaction passée ne se modifie jamais.
 */
public class Transaction {

    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final BigDecimal montant;
    private final TypeTransaction type;
    private final String description;
    private final LocalDateTime date;

    public Transaction(BigDecimal montant, TypeTransaction type, String description) {
        this.montant = montant;
        this.type = type;
        this.description = description;
        this.date = LocalDateTime.now(); // horodatage de l'opération
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public TypeTransaction getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "[" + date.format(FORMAT_DATE) + "] "
                + type + " - Montant: " + montant + " €"
                + " - " + description;
    }

    public enum TypeTransaction {
        DEPOT,
        RETRAIT,
        VIREMENT,
    }
}
