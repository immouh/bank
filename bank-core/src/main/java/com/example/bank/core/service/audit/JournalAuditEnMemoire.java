package com.example.bank.core.service.audit;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Journal volatil : pour les tests, et pour les montages sans base — un
 * service d'authentification doit pouvoir tourner sans qu'on lui impose un
 * stockage.
 */
public class JournalAuditEnMemoire implements JournalAudit {

    private final List<LigneAudit> lignes = new ArrayList<>();
    private final Clock horloge;

    public JournalAuditEnMemoire() {
        this(Clock.systemDefaultZone());
    }

    public JournalAuditEnMemoire(Clock horloge) {
        this.horloge = horloge;
    }

    @Override
    public void enregistrer(int rib, EvenementAudit evenement, String detail) {
        lignes.add(new LigneAudit(rib, evenement, LocalDateTime.now(horloge), detail));
    }

    @Override
    public List<LigneAudit> pour(int rib) {
        return lignes.stream().filter(ligne -> ligne.rib() == rib).toList();
    }

    /** Tout le journal, tous clients confondus. */
    public List<LigneAudit> tout() {
        return List.copyOf(lignes);
    }
}
