package com.example.bank.web.exception;

import com.example.bank.core.exception.BanqueException;
import com.example.bank.core.exception.etat.CompteVerouilleException;
import com.example.bank.core.exception.etat.ErreurEtatException;
import com.example.bank.core.exception.existence.ErreurExistenceException;
import com.example.bank.core.exception.existence.LivretADejaExistantException;
import com.example.bank.core.exception.technique.ErreurTechniqueException;
import com.example.bank.core.exception.validation.ErreurValidationException;
import com.example.bank.web.dto.ErreurReponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les exceptions du coeur en réponses HTTP.
 *
 * UN {@code @RestControllerAdvice} est un intercepteur d'exceptions valable
 * pour TOUS les contrôleurs : au lieu d'un {@code try/catch} recopié dans
 * chaque méthode, la traduction est écrite une fois. C'est le pendant web du
 * {@code catch (BanqueException e)} unique que l'IHM Swing peut se permettre
 * grâce à la hiérarchie sous {@code BanqueException}.
 *
 * ON MAPPE LES CATÉGORIES, PAS LES CLASSES FEUILLES. Les quatre familles
 * ({@code validation}, {@code etat}, {@code existence}, {@code technique})
 * suffisent : une nouvelle exception métier héritant de la bonne catégorie
 * reçoit automatiquement le bon code HTTP, sans qu'on ait à revenir ici. Les
 * trois exceptions traitées à part le sont pour une raison précise, expliquée
 * sur chaque méthode.
 *
 * PRÉCÉDENCE LA PLUS BASSE : la connexion a son propre gestionnaire, plus
 * spécifique, qui doit gagner (voir
 * {@link GestionnaireExceptionsAuthentification}).
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GestionnaireExceptionsGlobal {

    private static final Logger JOURNAL =
            LoggerFactory.getLogger(GestionnaireExceptionsGlobal.class);

    /** Montant invalide, virement vers soi-même, durée invalide. */
    @ExceptionHandler(ErreurValidationException.class)
    public ResponseEntity<ErreurReponse> validation(ErreurValidationException refus) {
        return reponse(HttpStatus.BAD_REQUEST, refus);
    }

    /**
     * 409 CONFLICT — la demande est bien FORMÉE, c'est l'état du compte qui
     * l'empêche : solde insuffisant, compte bloqué, plafond atteint. Un 400
     * dirait « ta requête est mauvaise », ce qui est faux : la même requête
     * passerait demain sur un compte approvisionné.
     */
    @ExceptionHandler(ErreurEtatException.class)
    public ResponseEntity<ErreurReponse> etat(ErreurEtatException refus) {
        return reponse(HttpStatus.CONFLICT, refus);
    }

    /**
     * 423 LOCKED — plus précis que le 409 de sa catégorie : le code dit
     * lui-même que la ressource est verrouillée temporairement, et le message
     * du coeur porte déjà le nombre de minutes restantes.
     *
     * Ce cas ne se produit ici que si un compte se verrouille APRÈS la
     * connexion ; le verrouillage à la connexion, lui, passe par
     * {@link GestionnaireExceptionsAuthentification}.
     */
    @ExceptionHandler(CompteVerouilleException.class)
    public ResponseEntity<ErreurReponse> verrouille(CompteVerouilleException refus) {
        return reponse(HttpStatus.LOCKED, refus);
    }

    /** Client, destinataire ou Livret A introuvable. */
    @ExceptionHandler(ErreurExistenceException.class)
    public ResponseEntity<ErreurReponse> existence(ErreurExistenceException refus) {
        return reponse(HttpStatus.NOT_FOUND, refus);
    }

    /**
     * 409 CONFLICT plutôt que le 404 de sa catégorie : « le Livret A existe
     * déjà » est l'inverse exact d'un « introuvable ». Répondre 404 à une
     * ressource qui existe en trop serait le contresens le plus visible de
     * tout ce tableau.
     */
    @ExceptionHandler(LivretADejaExistantException.class)
    public ResponseEntity<ErreurReponse> livretDejaLa(LivretADejaExistantException refus) {
        return reponse(HttpStatus.CONFLICT, refus);
    }

    /**
     * 500 — et le message du coeur ne sort PAS.
     *
     * Une {@code PersistanceException} porte le détail de la panne SQL ; le
     * renvoyer au client décrirait le schéma de la base à qui sonde l'API.
     * Le détail est journalisé côté serveur, le client reçoit une phrase
     * neutre.
     */
    @ExceptionHandler(ErreurTechniqueException.class)
    public ResponseEntity<ErreurReponse> technique(ErreurTechniqueException panne) {
        JOURNAL.error("Erreur technique traitée en 500", panne);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErreurReponse("Erreur interne du serveur."));
    }

    /**
     * Filet pour une {@code BanqueException} qui n'entrerait dans aucune des
     * quatre familles. Ne devrait pas arriver — mais le jour où une cinquième
     * catégorie apparaît, mieux vaut un 400 lisible qu'une trace Java.
     */
    @ExceptionHandler(BanqueException.class)
    public ResponseEntity<ErreurReponse> metierNonClasse(BanqueException refus) {
        JOURNAL.warn("Exception métier hors des quatre catégories : {}",
                refus.getClass().getName());
        return reponse(HttpStatus.BAD_REQUEST, refus);
    }

    /** Corps JSON absent ou illisible : c'est la requête qui est mauvaise. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErreurReponse> corpsIllisible(HttpMessageNotReadableException refus) {
        return ResponseEntity.badRequest()
                .body(new ErreurReponse("Corps de requête absent ou mal formé."));
    }

    /**
     * Dernier filet : rien ne doit sortir en trace Java.
     *
     * LES ERREURS QUE SPRING A DÉJÀ CLASSÉES GARDENT LEUR CODE. Chemin
     * inconnu, méthode HTTP non supportée, type de contenu refusé : Spring
     * lève des exceptions qui portent elles-mêmes leur statut
     * ({@code ErrorResponse}). Les faire tomber dans le 500 de ce filet
     * transformerait un banal 404 en « panne serveur » — c'est exactement ce
     * qu'un test d'isolation a attrapé : {@code GET /api/comptes/456}, une
     * route qui n'existe pas, répondait 500.
     *
     * Le message reste neutre : celui de Spring est en anglais et récite le
     * chemin demandé.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErreurReponse> imprevu(Exception panne) {
        if (panne instanceof ErrorResponse erreurHttp) {
            HttpStatusCode code = erreurHttp.getStatusCode();
            return ResponseEntity.status(code).body(new ErreurReponse(
                    code.value() == HttpStatus.NOT_FOUND.value()
                            ? "Ressource introuvable."
                            : "Requête refusée par le serveur."));
        }
        JOURNAL.error("Exception inattendue traitée en 500", panne);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErreurReponse("Erreur interne du serveur."));
    }

    private static ResponseEntity<ErreurReponse> reponse(HttpStatus code, BanqueException refus) {
        return ResponseEntity.status(code).body(new ErreurReponse(refus.getMessage()));
    }
}
