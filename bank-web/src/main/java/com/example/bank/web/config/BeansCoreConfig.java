package com.example.bank.web.config;

import com.example.bank.core.model.Client;
import com.example.bank.core.repository.BaseDeDonneesH2;
import com.example.bank.core.repository.JdbcClientRepository;
import com.example.bank.core.service.AuthService;
import com.example.bank.core.service.BanqueService;
import com.example.bank.core.service.audit.JdbcJournalAudit;
import com.example.bank.core.service.audit.JournalAudit;
import com.example.bank.core.service.commande.InvocateurCommande;
import com.example.bank.core.service.securite.BCryptHachageStrategy;
import com.example.bank.core.service.securite.HachageStrategy;
import com.example.bank.core.service.securite.JdbcRegistreTentatives;
import com.example.bank.core.service.securite.JdbcRepertoireMotsDePasse;
import com.example.bank.core.service.securite.RegistreTentatives;
import com.example.bank.core.service.securite.RepertoireMotsDePasse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.sql.Connection;

/**
 * LE PONT ENTRE SPRING ET LE COEUR MÉTIER.
 *
 * C'est le fichier qui explique pourquoi {@code bank-core} n'a pas eu à
 * changer d'une ligne. Spring sait injecter des objets qu'il a construits ;
 * il n'a jamais eu besoin que ces objets portent une annotation. Une méthode
 * {@code @Bean} n'est rien d'autre qu'un appel de constructeur dont Spring
 * retient le résultat — exactement les mêmes appels que ceux de
 * {@code bank-swing.Main}, recopiés ici sans rien y ajouter.
 *
 * Conséquence directe : pas un {@code @Component}, pas un {@code @Repository},
 * pas un {@code @Entity} dans le coeur. Il reste du Java pur, testable sans
 * Spring — ses 475 tests tournent toujours sans le moindre contexte Spring —
 * et {@code bank-swing} continue de le câbler à la main sans savoir que ce
 * module existe.
 *
 * PAS DE SPRING DATA JPA, décidé : {@code JdbcClientRepository} est injecté
 * tel quel. Le virement reste une transaction SQL portée par
 * {@code sauvegarderEnsemble}, pas par un {@code @Transactional}.
 */
@Configuration
public class BeansCoreConfig {

    /**
     * UNE SEULE CONNEXION, partagée — le montage repris tel quel du coeur.
     *
     * C'est la limite connue de cette phase : {@code JdbcClientRepository}
     * reçoit une {@code Connection}, pas une {@code DataSource}, et
     * {@code sauvegarderEnsemble} pose {@code setAutoCommit(false)} puis
     * valide sur CETTE connexion — deux requêtes web concurrentes
     * s'entremêleraient donc dans la même transaction. C'est acceptable pour
     * une application de démonstration mono-utilisateur, et {@code
     * BaseDeDonneesH2} annonce déjà que cette classe cédera la place à une
     * {@code DataSource}. Le passage à un pool demandera de changer la
     * signature du repository, donc de toucher au coeur : hors périmètre ici.
     */
    @Bean(destroyMethod = "close")
    public Connection connexion(@Value("${bank.base.url:jdbc:h2:./data/bank}") String url) {
        return BaseDeDonneesH2.ouvrir(url);
    }

    @Bean
    public JdbcClientRepository clientRepository(Connection connexion) {
        return new JdbcClientRepository(connexion);
    }

    @Bean
    public BanqueService banqueService(JdbcClientRepository repository) {
        return new BanqueService(repository);
    }

    /**
     * SÉCURITÉ — c'est ici, et nulle part ailleurs, qu'on choisit l'algorithme
     * de hachage, comme dans {@code Main}. En changer ne toucherait ni
     * {@code AuthService}, qui ne connaît que l'interface, ni Spring Security,
     * qui ne vérifie aucun mot de passe (voir {@code SecurityConfig}).
     */
    @Bean
    public HachageStrategy hachage() {
        return new BCryptHachageStrategy();
    }

    @Bean
    public RepertoireMotsDePasse repertoireMotsDePasse(Connection connexion) {
        return new JdbcRepertoireMotsDePasse(connexion);
    }

    @Bean
    public RegistreTentatives registreTentatives(Connection connexion) {
        return new JdbcRegistreTentatives(connexion);
    }

    @Bean
    public JournalAudit journalAudit(Connection connexion) {
        return new JdbcJournalAudit(connexion);
    }

    /**
     * La CHAÎNE d'authentification existante est montée par le constructeur
     * d'{@code AuthService} — existence, puis verrouillage, puis mot de passe.
     * Rien n'est réécrit ici : Spring Security ne vérifiera aucun identifiant,
     * il ne fera que porter le jeton une fois cette chaîne satisfaite.
     */
    @Bean
    public AuthService authService(JdbcClientRepository repository,
                                   HachageStrategy hachage,
                                   RepertoireMotsDePasse repertoire,
                                   RegistreTentatives registre,
                                   JournalAudit audit) {
        return new AuthService(repository, hachage, repertoire, registre, audit);
    }

    /**
     * Point de passage obligé des opérations, qui les consigne au journal
     * d'audit — exactement le rôle qu'il tient déjà côté Swing.
     */
    @Bean
    public InvocateurCommande invocateurCommande(JournalAudit audit) {
        return new InvocateurCommande(audit);
    }

    /**
     * Deux clients de démonstration, au tout premier lancement seulement.
     *
     * Repris de {@code Main} À L'IDENTIQUE mais SANS l'importer : les deux
     * frontaux sont indépendants, ils ne partagent que {@code bank-core}.
     *
     * LE CLAIR NE TOUCHE JAMAIS LA BASE : il est haché ici même, et c'est le
     * haché qui est inséré.
     */
    @Bean
    public DonneesDemonstration donneesDemonstration(JdbcClientRepository repository,
                                                     BanqueService banqueService,
                                                     HachageStrategy hachage) {
        return () -> {
            if (!repository.findAll().isEmpty()) {
                return;
            }
            Client mouh = new Client("Mouh", "tata", 123);
            Client amine = new Client("amine", "matoub", 456);
            repository.creer(mouh, hachage.hacher("tata"));
            repository.creer(amine, hachage.hacher("matoub"));

            banqueService.creerLivretA(mouh);
            banqueService.deposer(mouh, new BigDecimal("1000"));
            banqueService.virerVersLivretA(mouh, new BigDecimal("1000"));

            banqueService.deposer(mouh, new BigDecimal("500"));
            banqueService.retirer(mouh, new BigDecimal("200"));
            banqueService.virer(mouh, amine, new BigDecimal("300"));

            repository.enregistrerHistorique(mouh);
            repository.enregistrerHistorique(amine);
        };
    }

    /**
     * Contrat du chargement initial. Spring appelle {@code run()} au démarrage
     * parce que le type étend {@code CommandLineRunner} ; l'isoler derrière un
     * nom métier évite d'avoir à lire la signature de Spring pour comprendre
     * ce que fait ce bean.
     */
    @FunctionalInterface
    public interface DonneesDemonstration extends org.springframework.boot.CommandLineRunner {
        void charger();

        @Override
        default void run(String... args) {
            charger();
        }
    }
}
