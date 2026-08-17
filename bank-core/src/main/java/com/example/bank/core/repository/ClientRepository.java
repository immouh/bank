package com.example.bank.core.repository;

import com.example.bank.core.model.Client;

import java.util.List;
import java.util.Optional;

/**
 * Accès aux clients, indépendant du support de stockage.
 *
 * L'interface vit dans le coeur métier, les implémentations peuvent changer
 * (mémoire aujourd'hui, JDBC demain) sans qu'aucun service ni aucune fenêtre
 * ne soit modifié.
 */
public interface ClientRepository {

    Optional<Client> findByRib(int rib);

    Optional<Client> findByNom(String nom);

    void save(Client client);

    List<Client> findAll();

    boolean existsByRib(int rib);
}
