package com.example.bank.web.exception;

import com.example.bank.core.exception.etat.CompteVerouilleException;
import com.example.bank.core.exception.existence.ClientIntrouvableException;
import com.example.bank.web.controller.AuthController;
import com.example.bank.web.dto.ErreurReponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Les mêmes exceptions, mais en contexte de CONNEXION — où elles ne veulent
 * pas dire la même chose.
 *
 * POURQUOI UN SECOND GESTIONNAIRE. Partout ailleurs, une
 * {@code ClientIntrouvableException} signale une ressource absente : 404. À la
 * connexion, la MÊME exception signale « identifiants refusés » — et un 404
 * y annoncerait au monde que ce RIB n'existe pas, pendant qu'un mot de passe
 * faux rendrait autre chose. Le code HTTP redeviendrait l'oracle que le coeur
 * s'interdit depuis la phase A : sept tests y veillent côté métier, il aurait
 * suffi de la couche web pour tout perdre.
 *
 * {@code assignableTypes = AuthController.class} restreint ce gestionnaire au
 * seul contrôleur de connexion ; {@code @Order} le fait gagner sur le
 * gestionnaire global, qui garde ses codes pour tout le reste. La distinction
 * est donc portée par le CONTEXTE, pas par un {@code try/catch} dans le
 * contrôleur.
 */
@RestControllerAdvice(assignableTypes = AuthController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GestionnaireExceptionsAuthentification {

    /**
     * 401 pour un RIB inconnu COMME pour un mot de passe faux.
     *
     * Le message vient du coeur, où les deux étapes lèvent volontairement la
     * même phrase. Code identique, corps identique : la réponse ne permet pas
     * de distinguer les deux cas.
     */
    @ExceptionHandler(ClientIntrouvableException.class)
    public ResponseEntity<ErreurReponse> identifiantsRefuses(ClientIntrouvableException refus) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErreurReponse(refus.getMessage()));
    }

    /**
     * 423 LOCKED — et c'est un écart ASSUMÉ à la non-divulgation : après trois
     * échecs, la réponse cesse d'être indiscernable et annonce le
     * verrouillage, avec le délai restant.
     *
     * Le coeur fait déjà ce choix (il lève une exception distincte, avec son
     * propre message) et pour une bonne raison : un utilisateur bloqué doit
     * comprendre pourquoi ses bons identifiants sont refusés, sans quoi il
     * réessaie indéfiniment. Ce que ça révèle est marginal — il faut déjà
     * avoir provoqué trois échecs sur ce compte pour l'apprendre.
     */
    @ExceptionHandler(CompteVerouilleException.class)
    public ResponseEntity<ErreurReponse> compteVerrouille(CompteVerouilleException refus) {
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(new ErreurReponse(refus.getMessage()));
    }
}
