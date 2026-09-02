package com.example.bank.core.service.securite;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Répertoire volatil, pour les tests et les montages sans base. */
public class RepertoireMotsDePasseEnMemoire implements RepertoireMotsDePasse {

    private final Map<Integer, String> hachesParRib = new HashMap<>();

    @Override
    public Optional<String> hachePour(int rib) {
        return Optional.ofNullable(hachesParRib.get(rib));
    }

    @Override
    public void enregistrer(int rib, String hache) {
        hachesParRib.put(rib, hache);
    }
}
