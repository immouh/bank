package com.example.bank.web.dto;

import java.math.BigDecimal;

/**
 * Ce que rend une opération qui a abouti : le libellé que la commande a
 * produit — donc exactement celui qui part au journal d'audit — et le nouveau
 * solde du compte courant, pour éviter au client un aller-retour de plus.
 */
public record OperationReponse(String libelle, BigDecimal soldeCompte) {
}
