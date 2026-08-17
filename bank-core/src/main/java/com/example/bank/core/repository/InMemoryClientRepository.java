package com.example.bank.core.repository;

import com.example.bank.core.model.Client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stockage en mémoire, indexé par RIB.
 *
 * La Map remplace l'ancien tableau {@code Client[]} : la recherche par RIB est
 * en O(1) au lieu d'un parcours linéaire, l'unicité du RIB est garantie par la
 * structure elle-même, et le nombre de clients n'est plus figé à 2.
 *
 * Les données de démonstration créées ici sont les mêmes que celles qui
 * vivaient dans {@code Main} : seules les identités sont posées, les montants
 * sont ensuite appliqués par {@code BanqueService} — un repository stocke, il
 * n'applique pas de règles bancaires.
 */
public class InMemoryClientRepository implements ClientRepository {

    private final Map<Integer, Client> clientsParRib = new LinkedHashMap<>();

    public InMemoryClientRepository() {
        save(new Client("Mouh", "tata", 123));
        save(new Client("amine", "matoub", 456));
    }

    @Override
    public Optional<Client> findByRib(int rib) {
        return Optional.ofNullable(clientsParRib.get(rib));
    }

    @Override
    public Optional<Client> findByNom(String nom) {
        return clientsParRib.values().stream()
                .filter(c -> c.getNom().equals(nom))
                .findFirst();
    }

    @Override
    public void save(Client client) {
        clientsParRib.put(client.getRib(), client);
    }

    @Override
    public List<Client> findAll() {
        return new ArrayList<>(clientsParRib.values());
    }

    @Override
    public boolean existsByRib(int rib) {
        return clientsParRib.containsKey(rib);
    }
}
