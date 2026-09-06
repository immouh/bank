package com.example.bank.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Point d'entrée du frontal REST — l'équivalent de {@code Main} côté Swing,
 * en beaucoup plus court : c'est Spring qui assemble, à partir des {@code @Bean}
 * déclarés dans {@code config/}.
 *
 * L'AUTO-CONFIGURATION DE LA SOURCE DE DONNÉES EST DÉSACTIVÉE. Spring Boot,
 * voyant H2 sur le classpath, monterait sinon sa propre {@code DataSource} et
 * sa propre gestion du schéma. Or la base de ce projet est ouverte par
 * {@code BaseDeDonneesH2}, qui rejoue {@code schema.sql} lui-même : laisser
 * les deux mécanismes coexister donnerait deux vérités sur « qui crée la
 * base ». Une seule reste, celle du coeur.
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class BankWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankWebApplication.class, args);
    }
}
