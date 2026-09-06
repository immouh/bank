package com.example.bank.web.controller;

import com.example.bank.core.repository.JdbcClientRepository;
import com.example.bank.web.dto.LigneHistoriqueReponse;
import com.example.bank.web.securite.ClientAuthentifie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Historique des opérations du client authentifié.
 *
 * LA LECTURE VIENT DE LA BASE, pas de l'objet {@code Client} : un client relu
 * revient avec un historique en mémoire vide, et la table {@code transactions}
 * est la seule à porter les VRAIS horodatages (voir {@code LigneHistorique}
 * dans le coeur, qui existe exactement pour cette raison).
 */
@RestController
@RequestMapping("/api/historique")
public class HistoriqueController {

    private final JdbcClientRepository repository;

    public HistoriqueController(JdbcClientRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<LigneHistoriqueReponse> historique() {
        return repository.historique(ClientAuthentifie.rib()).stream()
                .map(ligne -> new LigneHistoriqueReponse(
                        ligne.numeroOrdre(),
                        ligne.type().name(),
                        ligne.montant(),
                        ligne.description(),
                        ligne.horodatage()))
                .toList();
    }
}
