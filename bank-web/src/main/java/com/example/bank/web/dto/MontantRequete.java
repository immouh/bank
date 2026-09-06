package com.example.bank.web.dto;

import java.math.BigDecimal;

/**
 * Corps d'un dépôt ou d'un retrait.
 *
 * LE MONTANT N'EST PAS VALIDÉ ICI — pas d'annotation {@code @Positive}. La
 * règle « strictement positif, deux décimales, HALF_EVEN » appartient à
 * {@code Montants} et le coeur l'applique déjà ; la redoubler en annotation
 * donnerait deux règles à maintenir et deux messages d'erreur différents pour
 * le même refus. Le DTO ne fait que transporter.
 */
public record MontantRequete(BigDecimal montant) {
}
