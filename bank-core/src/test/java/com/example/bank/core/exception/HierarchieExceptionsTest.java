package com.example.bank.core.exception;

import com.example.bank.core.exception.etat.CompteVerouilleException;
import com.example.bank.core.exception.etat.ErreurEtatException;
import com.example.bank.core.exception.etat.OperationInterditeException;
import com.example.bank.core.exception.etat.PlafondDepasseException;
import com.example.bank.core.exception.etat.SoldeInsuffisantException;
import com.example.bank.core.exception.existence.ClientIntrouvableException;
import com.example.bank.core.exception.existence.ErreurExistenceException;
import com.example.bank.core.exception.existence.LivretAAbsentException;
import com.example.bank.core.exception.existence.LivretADejaExistantException;
import com.example.bank.core.exception.technique.ErreurTechniqueException;
import com.example.bank.core.exception.technique.InvariantRompuException;
import com.example.bank.core.exception.technique.ParametreInvalideException;
import com.example.bank.core.exception.technique.PersistanceException;
import com.example.bank.core.exception.validation.DureeInvalideException;
import com.example.bank.core.exception.validation.ErreurValidationException;
import com.example.bank.core.exception.validation.MontantInvalideException;
import com.example.bank.core.exception.validation.VirementVersSoiMemeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Le contrat de la hiérarchie d'exceptions, testé de bout en bout.
 *
 * POURQUOI CE TEST EXISTE — les fenêtres Swing n'écrivent qu'un seul
 * {@code catch (BanqueException e)} et affichent {@code e.getMessage()}. Toute
 * exception métier qui sortirait de cette hiérarchie traverserait l'IHM sans
 * être attrapée, et l'utilisateur verrait une trace technique au lieu d'un
 * message. Le regroupement en quatre sous-packages ne doit rien changer à
 * cette garantie : c'est ce que vérifie {@link #catchBanqueExceptionAttrapeTout()}.
 *
 * INSTANCES RÉELLES, PAS DES CLASSES — on lève et on rattrape vraiment, plutôt
 * que de comparer des {@code Class}. C'est la sémantique du {@code catch} de
 * l'IHM qui nous intéresse, pas la parenté déclarée.
 */
@DisplayName("Hiérarchie des exceptions métier")
class HierarchieExceptionsTest {

    /**
     * Les treize exceptions CONCRÈTES du projet, une instance de chacune.
     *
     * À tenir à jour : ajouter une exception métier sans l'ajouter ici ferait
     * passer ce test à tort.
     */
    private static List<BanqueException> toutesLesExceptionsConcretes() {
        return List.of(
                // validation
                new MontantInvalideException("montant nul"),
                new VirementVersSoiMemeException(),
                new DureeInvalideException(),
                // etat
                new OperationInterditeException("compte fermé"),
                new CompteVerouilleException(5),
                new PlafondDepasseException(new BigDecimal("300.00"), new BigDecimal("500.00")),
                new SoldeInsuffisantException(new BigDecimal("10.00"), new BigDecimal("50.00")),
                // existence
                new ClientIntrouvableException("client inconnu"),
                new LivretAAbsentException(),
                new LivretADejaExistantException(),
                // technique
                new PersistanceException("base injoignable"),
                new ParametreInvalideException("paramètre absent"),
                new InvariantRompuException("invariant rompu"));
    }

    @Test
    @DisplayName("Les treize exceptions concrètes sont couvertes par ce test")
    void treizeExceptionsConcretes() {
        assertEquals(13, toutesLesExceptionsConcretes().size());
    }

    @Test
    @DisplayName("Un unique catch (BanqueException) attrape chacune des treize exceptions concrètes")
    void catchBanqueExceptionAttrapeTout() {
        for (BanqueException exception : toutesLesExceptionsConcretes()) {
            // Le bloc exact qu'écrivent les fenêtres Swing.
            try {
                throw exception;
            } catch (BanqueException attrapee) {
                assertTrue(attrapee.getMessage() != null && !attrapee.getMessage().isBlank(),
                        attrapee.getClass().getSimpleName() + " doit porter un message affichable");
            } catch (RuntimeException echappee) {
                fail(echappee.getClass().getSimpleName() + " a échappé au catch (BanqueException)");
            }
        }
    }

    @Nested
    @DisplayName("Appartenance à une catégorie")
    class Categories {

        @Test
        @DisplayName("Les erreurs de saisie descendent de ErreurValidationException")
        void categorieValidation() {
            assertAll(
                    () -> assertInstanceOf(ErreurValidationException.class,
                            new MontantInvalideException("montant nul")),
                    () -> assertInstanceOf(ErreurValidationException.class,
                            new VirementVersSoiMemeException()),
                    () -> assertInstanceOf(ErreurValidationException.class,
                            new DureeInvalideException()));
        }

        @Test
        @DisplayName("Les refus liés à l'état courant descendent de ErreurEtatException")
        void categorieEtat() {
            assertAll(
                    () -> assertInstanceOf(ErreurEtatException.class,
                            new OperationInterditeException("compte fermé")),
                    () -> assertInstanceOf(ErreurEtatException.class,
                            new CompteVerouilleException(5)),
                    () -> assertInstanceOf(ErreurEtatException.class,
                            new PlafondDepasseException(new BigDecimal("300.00"), new BigDecimal("500.00"))),
                    () -> assertInstanceOf(ErreurEtatException.class,
                            new SoldeInsuffisantException(new BigDecimal("10.00"), new BigDecimal("50.00"))));
        }

        @Test
        @DisplayName("Les entités absentes ou déjà présentes descendent de ErreurExistenceException")
        void categorieExistence() {
            assertAll(
                    () -> assertInstanceOf(ErreurExistenceException.class,
                            new ClientIntrouvableException("client inconnu")),
                    () -> assertInstanceOf(ErreurExistenceException.class,
                            new LivretAAbsentException()),
                    () -> assertInstanceOf(ErreurExistenceException.class,
                            new LivretADejaExistantException()));
        }

        @Test
        @DisplayName("Les pannes et bugs descendent de ErreurTechniqueException")
        void categorieTechnique() {
            assertAll(
                    () -> assertInstanceOf(ErreurTechniqueException.class,
                            new PersistanceException("base injoignable")),
                    () -> assertInstanceOf(ErreurTechniqueException.class,
                            new ParametreInvalideException("paramètre absent")),
                    () -> assertInstanceOf(ErreurTechniqueException.class,
                            new InvariantRompuException("invariant rompu")));
        }

        @Test
        @DisplayName("Les quatre catégories descendent elles-mêmes de BanqueException")
        void categoriesSousLaRacine() {
            assertAll(
                    () -> assertTrue(BanqueException.class.isAssignableFrom(ErreurValidationException.class)),
                    () -> assertTrue(BanqueException.class.isAssignableFrom(ErreurEtatException.class)),
                    () -> assertTrue(BanqueException.class.isAssignableFrom(ErreurExistenceException.class)),
                    () -> assertTrue(BanqueException.class.isAssignableFrom(ErreurTechniqueException.class)));
        }
    }

    @Nested
    @DisplayName("Les trois exceptions rapatriées dans la hiérarchie")
    class Rapatriees {

        @Test
        @DisplayName("La durée de prêt invalide n'est plus une IllegalArgumentException")
        void dureeInvalide() {
            DureeInvalideException exception = new DureeInvalideException();

            assertAll(
                    () -> assertInstanceOf(ErreurValidationException.class, exception),
                    () -> assertInstanceOf(BanqueException.class, exception),
                    () -> assertFalse(IllegalArgumentException.class.isInstance(exception)));
        }

        @Test
        @DisplayName("Le paramètre interne absent n'est plus une IllegalArgumentException")
        void parametreInvalide() {
            ParametreInvalideException exception = new ParametreInvalideException("mot de passe absent");

            assertAll(
                    () -> assertInstanceOf(ErreurTechniqueException.class, exception),
                    () -> assertInstanceOf(BanqueException.class, exception),
                    () -> assertFalse(IllegalArgumentException.class.isInstance(exception)));
        }

        @Test
        @DisplayName("L'invariant rompu n'est plus une IllegalStateException")
        void invariantRompu() {
            InvariantRompuException exception = new InvariantRompuException("client sans compte courant");

            assertAll(
                    () -> assertInstanceOf(ErreurTechniqueException.class, exception),
                    () -> assertInstanceOf(BanqueException.class, exception),
                    () -> assertFalse(IllegalStateException.class.isInstance(exception)));
        }
    }
}
