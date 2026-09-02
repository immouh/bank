package com.example.bank.core.repository;

import com.example.bank.core.model.Client;

import java.util.Arrays;
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

    /**
     * Sauvegarde plusieurs clients comme UN SEUL TOUT : ou bien tous sont
     * écrits, ou bien aucun ne l'est.
     *
     * C'est ce qu'exige un virement, qui débite l'un et crédite l'autre —
     * enchaîner deux {@link #save(Client)} laisserait, en cas de panne entre
     * les deux, un client débité et l'autre jamais crédité.
     *
     * L'implémentation par défaut se contente d'enchaîner les {@code save} :
     * elle convient au stockage en mémoire, où il n'y a rien à valider et
     * donc rien à annuler. Les implémentations sur base de données doivent la
     * redéfinir par une vraie transaction.
     */
    default void sauvegarderEnsemble(Client... clients) {
        Arrays.stream(clients).forEach(this::save);
    }
}
