# Phase G — le module `bank-web`, expliqué fichier par fichier

**Pour qui** : quelqu'un qui connaît déjà le projet `bank` (le modèle, les services, les patrons) mais qui n'a jamais fait de Spring Boot.

**Ce que ce document n'est pas** : une documentation d'API. Il n'y a pas de Swagger ici. C'est un document d'apprentissage, à relire avant d'attaquer la phase H.

---

## Sommaire

1. [L'idée en une page](#1-lidée-en-une-page)
2. [Les concepts Spring introduits](#2-les-concepts-spring-introduits)
3. [Le flux complet d'une requête authentifiée](#3-le-flux-complet-dune-requête-authentifiée)
4. [Pourquoi `bank-core` n'a pas eu besoin de changer](#4-pourquoi-bank-core-na-pas-eu-besoin-de-changer)
5. [Chaque fichier, un par un](#5-chaque-fichier-un-par-un)
6. [Le tableau exception → code HTTP](#6-le-tableau-exception--code-http)
7. [Ce qui a été volontairement laissé de côté](#7-ce-qui-a-été-volontairement-laissé-de-côté)

---

## 1. L'idée en une page

`bank-swing` est un frontal : il prend les entrées de l'utilisateur, appelle `bank-core`, affiche le résultat.

`bank-web` est **un deuxième frontal, du même rang**. Il fait exactement la même chose, mais l'utilisateur est au bout d'un câble réseau au lieu d'être devant une `JFrame`. Là où Swing lit un `JTextField`, l'API lit un corps JSON. Là où Swing affiche un `JOptionPane`, l'API renvoie un code HTTP.

```
                    ┌──────────────┐
                    │  bank-core   │   métier pur, Java, zéro framework
                    └──────┬───────┘
                  ┌────────┴────────┐
                  │                 │
          ┌───────▼──────┐   ┌──────▼───────┐
          │  bank-swing  │   │   bank-web   │
          │   (JFrame)   │   │  (HTTP/JSON) │
          └──────────────┘   └──────────────┘
```

Les deux frontaux **ne se connaissent pas**. `bank-web` n'importe pas une seule classe de `bank-swing` : quand il fallait la même logique (les données de démonstration), elle a été réécrite, pas importée.

La seule chose vraiment nouvelle est l'authentification. En Swing, une fois la fenêtre de connexion passée, l'application *sait* qui est là : elle garde l'objet `Client` en mémoire. En HTTP, chaque requête arrive nue, sans mémoire de la précédente. Il faut donc un moyen pour que la requête dise elle-même qui l'envoie : c'est le **jeton JWT**.

---

## 2. Les concepts Spring introduits

Six notions suffisent à lire tout le module.

### `@Configuration` et `@Bean`

Un **bean** est simplement un objet que Spring construit et garde pour le donner à qui en a besoin. Une classe `@Configuration` est une liste de recettes ; chaque méthode `@Bean` est une recette.

```java
@Bean
public BanqueService banqueService(JdbcClientRepository repository) {
    return new BanqueService(repository);
}
```

Ça se lit : « pour fabriquer un `BanqueService`, il me faut un `JdbcClientRepository` ; débrouille-toi pour me le donner, et voilà comment j'assemble ». C'est **exactement** ce que fait `Main` dans `bank-swing`, à ceci près que `Main` écrit les appels dans le bon ordre à la main, alors que Spring déduit l'ordre des paramètres.

C'est le point clé de toute la phase : un `@Bean` n'exige **rien** de l'objet fabriqué. `BanqueService` n'a pas besoin d'annotation pour être un bean.

### `@RestController`

Une classe dont les méthodes répondent à des URLs. `@GetMapping("/api/comptes/moi")` veut dire « quand une requête `GET` arrive sur ce chemin, appelle cette méthode ». La valeur rendue est automatiquement convertie en JSON par Jackson.

C'est le pendant du `ActionListener` d'un bouton Swing : le déclencheur n'est plus un clic, c'est une requête HTTP.

### Un filtre

Du code qui s'exécute **avant** que la requête n'atteigne le moindre contrôleur, et qui peut la laisser passer, la modifier ou l'arrêter. Spring Security n'est rien d'autre qu'une **chaîne de filtres** — la même idée que la chaîne `EtapeAuthentification` du cœur, appliquée à HTTP.

`FiltreAuthentificationJwt` s'insère dans cette chaîne pour lire le jeton.

### Le contexte de sécurité (`SecurityContextHolder`)

Un porte-documents attaché au **thread** qui traite la requête. Le filtre y dépose « c'est le client 123 » ; n'importe quel code exécuté plus loin dans la même requête peut le relire.

C'est ce qui remplace le champ `private Client clientConnecte` d'une fenêtre Swing : on ne peut pas garder l'information entre deux requêtes, mais on peut la porter **pendant** une requête.

### `@RestControllerAdvice`

Un intercepteur d'exceptions valable pour tous les contrôleurs à la fois. Au lieu d'un `try/catch` recopié dans chaque méthode, la traduction « exception métier → réponse HTTP » est écrite une seule fois.

C'est le pendant web du `catch (BanqueException e)` unique que l'IHM Swing peut se permettre grâce à la hiérarchie sous `BanqueException`. La même décision d'architecture paie une deuxième fois.

### Stateless

« Sans état » : le serveur ne retient rien entre deux requêtes. Pas de session, pas de cookie, pas de « qui est connecté » en mémoire. Chaque requête se justifie seule, par son jeton.

---

## 3. Le flux complet d'une requête authentifiée

### Étape 0 — obtenir un jeton (une seule fois)

```
POST /api/auth/login          { "rib": 123, "motDePasse": "tata" }
   │
   │  route déclarée PUBLIQUE dans SecurityConfig : le filtre JWT
   │  ne trouve pas de jeton, et c'est normal
   ▼
AuthController.connexion()
   │
   ├─► repository.findByRib(123) ────────► "Mouh"   (pont RIB → nom)
   │
   ├─► authService.authentifier("Mouh", "tata")
   │        │
   │        │   LA CHAÎNE DU CŒUR, inchangée :
   │        ├──► VerificationClientExiste     le nom existe-t-il ?
   │        ├──► VerificationVerrouillage     3 échecs récents ?
   │        └──► VerificationMotDePasse       BCrypt contre le haché
   │        │
   │        └──► registre.reinitialiser() + journal d'audit
   │
   └─► jwtService.genererPour(123, "Mouh")
              │
              ▼
   200  { "token": "eyJhbGciOi...", "typeToken": "Bearer",
          "expireDansSecondes": 3600, "rib": 123, "nom": "Mouh" }
```

### Étapes 1 à 6 — chaque requête protégée

```
GET /api/comptes/moi
Authorization: Bearer eyJhbGciOi...
   │
   │  ① FiltreAuthentificationJwt
   │     lit l'en-tête, vérifie la SIGNATURE et l'EXPIRATION,
   │     extrait le RIB (123) et le pose dans le contexte de sécurité.
   │     Jeton absent / faux / expiré → il ne pose RIEN et laisse passer.
   ▼
   │  ② SecurityConfig.authorizeHttpRequests
   │     « cette route exige d'être authentifié ». Personne dans le
   │     contexte ? → AuthenticationEntryPoint → 401 { "erreur": ... }
   │     et le contrôleur n'est jamais atteint.
   ▼
   │  ③ CompteController.situation()
   │     ClientAuthentifie.rib() relit le contexte → 123
   │     Aucun RIB ne vient de l'URL ni du corps : rien à falsifier.
   ▼
   │  ④ bank-core
   │     banqueService.rechercherParRib(123) → JdbcClientRepository → H2
   ▼
   │  ⑤ conversion en DTO
   │     Client + List<Compte>  →  SituationReponse + List<CompteReponse>
   │     Ce qui n'est pas listé dans le record ne sort pas.
   ▼
   │  ⑥ Jackson sérialise le record en JSON
   ▼
   200  { "rib": 123, "nom": "Mouh", "soldeTotal": 1000.00,
          "comptes": [ { "type": "CompteStandard", ... } ] }
```

Et si le métier refuse — solde insuffisant, montant négatif, destinataire inconnu — l'exception traverse le contrôleur sans être capturée, et `GestionnaireExceptionsGlobal` la transforme en code HTTP + `{ "erreur": ... }`.

---

## 4. Pourquoi `bank-core` n'a pas eu besoin de changer

C'est le résultat le plus important de cette phase, et il n'est pas un hasard : c'est le chantier 3 (l'architecture en couches) qui est en train d'être encaissé.

### Ce qu'on aurait pu croire nécessaire

Un réflexe courant est d'annoter le cœur :

```java
// CE QUI N'A PAS ÉTÉ FAIT
@Service                        // ← dans bank-core
public class BanqueService { ... }

@Repository                     // ← dans bank-core
public class JdbcClientRepository { ... }

@Entity                         // ← dans bank-core
public class Client { ... }
```

Ça marcherait. Et ça coûterait cher : `bank-core` dépendrait de Spring **à la compilation**. Ses 475 tests auraient besoin d'un contexte Spring pour tourner. `bank-swing`, qui n'a que faire de Spring, embarquerait quand même tout le framework. Et `@Entity` obligerait à donner un constructeur vide et des mutateurs à `Client` — exactement l'encapsulation que la phase A a défendue par un test.

### Ce qui a été fait à la place

Spring sait injecter des objets **qu'il a construits lui-même**. Il n'a jamais exigé qu'ils portent une annotation. `BeansCoreConfig` se contente donc d'appeler les constructeurs existants :

```java
// bank-web/config/BeansCoreConfig.java
@Bean
public AuthService authService(JdbcClientRepository repository,
                               HachageStrategy hachage,
                               RepertoireMotsDePasse repertoire,
                               RegistreTentatives registre,
                               JournalAudit audit) {
    return new AuthService(repository, hachage, repertoire, registre, audit);
}
```

À comparer avec `bank-swing/Main.java`, ligne pour ligne :

```java
// bank-swing/Main.java — le MÊME appel, écrit à la main
AuthService authService =
        new AuthService(repository, hachage, repertoire, registre, audit);
```

C'est le même code. La seule différence est qui décide de l'ordre d'assemblage : `Main` l'écrit, Spring le déduit des types.

### Ce que ça achète

| | |
|---|---|
| `bank-core` | zéro import Spring, zéro annotation. Ses tests tournent sans contexte. |
| `bank-swing` | ne sait pas que `bank-web` existe. N'a pas été recompilé différemment. |
| `bank-web` | seul module à dépendre de Spring. |
| Le virement | reste atomique par `sauvegarderEnsemble` (vraie transaction SQL), pas par un `@Transactional`. |
| La non-divulgation | reste garantie par le cœur ; la couche web n'a eu qu'à ne pas la casser. |

**La leçon** : « le cœur est réutilisable tel quel par un futur frontal REST », écrit dans le README depuis le chantier 3, était une hypothèse. Cette phase l'a vérifiée. Le `git diff --stat` sur `bank-core/` et `bank-swing/` est vide.

---

## 5. Chaque fichier, un par un

### `pom.xml` (parent, modifié) et `bank-web/pom.xml`

**Rôle** — déclarer le troisième module et ses dépendances.

**Pourquoi cette forme** — le POM parent du projet reste `bank-parent`, pas `spring-boot-starter-parent`. Un projet Maven ne peut avoir qu'un seul parent, et c'est le nôtre qui porte le JDK 21 et l'épinglage de surefire 3.2.5 (le piège documenté depuis le chantier 5 : sans lui, Maven 3.6 ignorerait tous les tests JUnit 5 en silence). On récupère quand même la gestion de versions de Spring en **important** son BOM :

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-dependencies</artifactId>
    <version>${spring-boot.version}</version>
    <type>pom</type>
    <scope>import</scope>     <!-- importé, pas hérité -->
</dependency>
```

**Articulation** — `bank-web` dépend de `bank-core`, jamais l'inverse, et n'a aucune dépendance vers `bank-swing`. Le driver H2 est en scope `runtime`, comme dans `bank-swing` : c'est le point d'assemblage qui choisit la base, le cœur ne compile contre aucun driver.

---

### `BankWebApplication.java`

**Rôle** — le point d'entrée, l'équivalent du `main()` de `bank-swing`.

**Pourquoi cette forme** — il est presque vide, et c'est normal : `@SpringBootApplication` déclenche le balayage des classes du package et le montage automatique. Le seul choix explicite est une **exclusion** :

```java
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
```

Sans elle, Spring Boot verrait H2 sur le classpath et monterait sa propre `DataSource` avec sa propre gestion du schéma. Or la base de ce projet est ouverte par `BaseDeDonneesH2`, qui rejoue `schema.sql` lui-même. Laisser les deux mécanismes coexister donnerait **deux vérités** sur qui crée la base. Une seule survit, celle du cœur.

---

### `config/BeansCoreConfig.java`

**Rôle** — le pont entre Spring et `bank-core`. C'est le fichier le plus important du module.

**Pourquoi il existe** — voir [§4](#4-pourquoi-bank-core-na-pas-eu-besoin-de-changer) : il permet à Spring d'injecter des objets qui ignorent Spring.

**Articulation** — il fabrique tout ce que les contrôleurs recevront : `Connection`, `JdbcClientRepository`, `BanqueService`, `AuthService`, `HachageStrategy`, `RepertoireMotsDePasse`, `RegistreTentatives`, `JournalAudit`, `InvocateurCommande`, plus les données de démonstration.

**Le point qui mérite explication** — la connexion :

```java
@Bean(destroyMethod = "close")
public Connection connexion(@Value("${bank.base.url:jdbc:h2:./data/bank}") String url) {
    return BaseDeDonneesH2.ouvrir(url);
}
```

`@Value` lit une propriété de configuration, avec une valeur par défaut après le `:`. C'est ce qui permet aux tests de pointer chacun vers leur propre base en mémoire, sans toucher au code.

`destroyMethod = "close"` dit à Spring de fermer la connexion à l'arrêt — l'équivalent du `addShutdownHook` de `Main`.

C'est aussi **la limite connue de cette phase** : une seule connexion, partagée par toutes les requêtes. Voir [§7](#7-ce-qui-a-été-volontairement-laissé-de-côté).

Un détail sur les données de démonstration : le bean rendu implémente `CommandLineRunner`, une interface Spring dont la méthode `run()` est appelée au démarrage. Elle est cachée derrière une interface au nom métier pour qu'on n'ait pas à connaître Spring pour comprendre ce que fait le bean :

```java
@FunctionalInterface
public interface DonneesDemonstration extends CommandLineRunner {
    void charger();
    @Override default void run(String... args) { charger(); }
}
```

---

### `config/SecurityConfig.java`

**Rôle** — dire quelles routes exigent un jeton, et comment refuser proprement celles qui n'en ont pas.

**Pourquoi cette forme** — c'est le fichier où la décision la plus structurante de la phase est prise : **Spring Security ne vérifie aucun identifiant**. Pas de `UserDetailsService`, pas de `PasswordEncoder`, pas d'`AuthenticationProvider`. Ces briques referaient, en moins bien, ce que la chaîne `EtapeAuthentification` fait déjà — existence, verrouillage après trois échecs, BCrypt, non-divulgation testée depuis la phase A. Les dupliquer donnerait **deux implémentations de la même règle, dont une seule serait testée**.

```java
return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(routes -> routes
                .requestMatchers("/api/auth/login").permitAll()
                .anyRequest().authenticated())
        .addFilterBefore(filtreJwt, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(e -> e.authenticationEntryPoint(pointDEntree()))
        .build();
```

Ligne par ligne :

- **`csrf.disable()`** — la protection anti-CSRF défend les formulaires HTML dont le navigateur envoie **automatiquement** le cookie de session. Ici il n'y a ni cookie ni session : le jeton est posé **à la main** dans un en-tête par le code appelant, ce qu'un site tiers ne peut pas faire. La désactiver n'ouvre donc rien.
- **`STATELESS`** — aucune session HTTP, pas même créée à la demande.
- **`permitAll()` sur `/api/auth/login`** — il faut bien pouvoir obtenir un jeton sans en avoir un. **Tout le reste** est protégé, par défaut : une nouvelle route ajoutée demain sera protégée sans qu'on y pense.
- **`addFilterBefore`** — notre filtre s'exécute avant celui du formulaire de connexion de Spring, qui n'a alors plus rien à faire.
- **`authenticationEntryPoint`** — déclaré explicitement pour deux raisons : le défaut de Spring Security 6 rend un **403** là où un **401** est la réponse juste (« authentifie-toi », pas « tu n'as pas le droit »), et le corps doit être le même JSON `{ "erreur": ... }` que celui du gestionnaire d'exceptions. Sans ça, l'API répondrait dans deux formats selon l'endroit où le refus se décide.

---

### `securite/JwtService.java`

**Rôle** — fabriquer et vérifier les jetons.

**Pourquoi un jeton plutôt qu'une session** — une session demande au serveur de retenir qui est connecté : de la mémoire partagée entre requêtes, et un état à répliquer si l'application tourne en plusieurs exemplaires. Un jeton signé déplace cette information chez le client. Le serveur ne retient rien, il vérifie une signature.

**Ce que le jeton contient, et pourquoi si peu** — un JWT est **signé, pas chiffré**. N'importe qui peut lire sa charge utile en la décodant en base64 :

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMiLCJub20iOiJNb3VoIiwiaWF0Ijo...
        en-tête        │         charge utile, lisible par tous
                       └─► {"sub":"123","nom":"Mouh","iat":...,"exp":...}
```

Y mettre un mot de passe, un haché ou un solde reviendrait à les **publier**. La signature garantit seulement que personne ne l'a **modifié**. Un test (`chargeUtileSansSecret`) décode la charge utile et vérifie qu'aucun haché ne s'y trouve.

**La génération** :

```java
return Jwts.builder()
        .setSubject(String.valueOf(rib))    // le RIB, seule chose qui compte
        .claim("nom", nom)
        .setIssuedAt(maintenant)
        .setExpiration(new Date(maintenant.getTime() + VALIDITE.toMillis()))
        .signWith(cle, SignatureAlgorithm.HS256)
        .compact();
```

**La validation**, et son choix le plus important :

```java
public Optional<Integer> ribDuJeton(String jeton) {
    try {
        Claims charge = Jwts.parserBuilder().setSigningKey(cle).build()
                .parseClaimsJws(jeton)      // vérifie signature ET expiration
                .getBody();
        return Optional.of(Integer.valueOf(charge.getSubject()));
    } catch (JwtException | IllegalArgumentException refus) {
        return Optional.empty();            // tous les refus se ressemblent
    }
}
```

`parseClaimsJws` vérifie la signature **et** la date : il n'y a pas de contrôle d'expiration à écrire à la main.

Rendre un `Optional` vide plutôt que de propager l'exception est délibéré : ça empêche l'appelant de renvoyer au client un message distinguant « signature invalide » de « jeton expiré ». Un attaquant n'apprend pas où en est sa tentative de forge. C'est la même idée que la non-divulgation du cœur, appliquée aux jetons.

**Le secret** vient de la configuration, jamais du code, et une clé trop courte est refusée **au démarrage** — HS256 exige 256 bits. Laisser l'application monter avec une clé faible serait pire qu'un échec bruyant.

**Durée choisie : une heure.** Assez pour une session de travail sans réauthentification, assez court pour qu'un jeton volé cesse vite de servir. Sans mécanisme de révocation (voir §7), l'expiration est la **seule** chose qui limite la durée de vie d'un jeton compromis.

---

### `securite/FiltreAuthentificationJwt.java`

**Rôle** — le portier : lire le jeton de chaque requête et dire à Spring Security qui la fait.

**Pourquoi cette forme** — il étend `OncePerRequestFilter`, qui garantit une seule exécution par requête même quand Spring fait des redirections internes.

```java
@Override
protected void doFilterInternal(HttpServletRequest requete,
                                HttpServletResponse reponse,
                                FilterChain suite) {
    jetonDe(requete)
            .flatMap(jwtService::ribDuJeton)
            .ifPresent(rib -> authentifier(rib, requete));
    suite.doFilter(requete, reponse);      // ← passe TOUJOURS la main
}
```

**Le point qui mérite explication : un refus n'est pas une erreur ici.** Jeton absent, mal signé ou expiré : le filtre laisse simplement passer la requête **sans poser d'authentification**. Il n'écrit aucune réponse d'erreur.

C'est `SecurityConfig` qui conclura, en refusant l'accès à une route protégée non authentifiée. Écrire la réponse d'erreur dans le filtre mélangerait deux responsabilités et donnerait **deux endroits** où le format du refus pourrait diverger — la même raison qui fait que `AuthController` ne capture rien non plus.

**Ce qu'il ne fait pas** — il ne vérifie aucun mot de passe et ne consulte aucun verrouillage. Ces contrôles appartiennent à la chaîne du cœur, qui ne s'exécute **qu'une fois, à la connexion**. Ici on constate seulement qu'un jeton valide a déjà été délivré.

---

### `securite/ClientAuthentifie.java`

**Rôle** — d'où les contrôleurs tiennent le RIB de l'appelant.

**Pourquoi il existe** — c'est le point de sécurité le plus important du module, et il tient en une phrase : **le RIB vient du jeton, jamais d'un paramètre**.

```java
public static int rib() {
    Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
    if (authentification == null
            || !(authentification.getPrincipal() instanceof Integer rib)) {
        throw new ClientIntrouvableException("Requête non authentifiée.");
    }
    return rib;
}
```

Un endpoint qui accepterait `GET /api/comptes/{rib}` laisserait n'importe quel client authentifié lire les comptes d'un autre en changeant un chiffre. C'est la faille classique, et celle qu'**aucune vérification de mot de passe n'attrape** : l'appelant *est* authentifié, il est juste en train de désigner quelqu'un d'autre.

Passer par une classe dédiée plutôt que de lire le contexte dans chaque contrôleur laisse **un seul endroit** à relire pour s'en assurer.

---

### `controller/AuthController.java`

**Rôle** — `POST /api/auth/login`, la seule route ouverte.

**Ce qu'il ne fait pas** — il ne vérifie rien lui-même et **ne capture aucune exception**. En cas de refus, l'exception traverse, et c'est `GestionnaireExceptionsAuthentification` qui la traduit. Un `try/catch` ici donnerait un second endroit où le format du refus pourrait diverger — et c'est précisément par là qu'une fuite d'information se glisse.

**Le point délicat : le pont RIB → nom.** L'API s'authentifie par RIB, `AuthService` par nom :

```java
private String nomDuRib(ConnexionRequete requete) {
    if (requete == null || requete.rib() == null) {
        return null;
    }
    return repository.findByRib(requete.rib()).map(Client::getNom).orElse(null);
}
```

Pourquoi pas un `authentifierParRib` dans le cœur ? Parce que cette phase ne modifie pas `bank-core`. Traduire ici coûte une lecture de plus et ne change rien à ce qui est vérifié ensuite.

**Et la non-divulgation ?** C'est la question à se poser, parce qu'un pont mal fait la casserait. Un RIB inconnu rend `null`, que `AuthService` traite **exactement** comme un nom inconnu :

- `VerificationClientExiste` vérifie la saisie contre son **haché factice** — donc au même coût en temps qu'un vrai refus, ce qui neutralise l'attaque au chronomètre ;
- puis lève la **même** exception, avec le **même** message, qu'un mot de passe faux.

Ni le code HTTP, ni le corps, ni le temps de réponse ne disent si le RIB existe. Deux tests le vérifient en comparant les réponses **entières**.

---

### `controller/CompteController.java`

**Rôle** — `GET /api/comptes/moi`, `POST /api/comptes/depot`, `POST /api/comptes/retrait`, `POST /api/livret-a`.

**Articulation avec le cœur** — les opérations passent par les **commandes**, comme côté Swing :

```java
private OperationReponse executer(Client client, Commande commande) {
    int avant = client.getHistorique().size();
    invocateur.executer(commande);          // exécute PUIS journalise à l'audit
    historique.ajouter(client, avant);      // écrit les nouvelles lignes
    return new OperationReponse(commande.libelle(), client.getSoldeCompte());
}
```

C'est `InvocateurCommande` qui journalise à l'audit — une fois, au même endroit, pour les deux frontaux. Rien n'est réécrit ici.

L'écriture de l'historique vient **après** l'exécution : une commande refusée lève, et rien n'est écrit. Même règle que pour le journal d'audit.

**L'exception : le Livret A.** Il n'y a pas de commande pour lui — le cœur n'en fournit pas, et en inventer une ici la mettrait dans le mauvais module. L'audit est donc posé à la main, comme `AuthService` le fait pour les connexions. La réponse est un **201 Created** : l'appel fait apparaître une ressource qui n'existait pas.

---

### `controller/VirementController.java`

**Rôle** — `POST /api/virements`.

**Le point de sécurité** — le corps de la requête ne contient **que le destinataire**. L'émetteur vient toujours du jeton. Le champ `ribEmetteur` absent est une décision, pas un oubli : débiter quelqu'un d'autre est **impossible**, pas seulement interdit.

**Articulation avec le cœur** — l'atomicité reste celle de `BanqueService.virer`, qui appelle `sauvegarderEnsemble` (vraie transaction SQL). Aucun `@Transactional` n'est ajouté : il en donnerait une seconde, au-dessus de la première, sans rien garantir de plus.

Un virement produit une ligne d'historique **de chaque côté** ; les deux sont écrites, sans quoi le destinataire verrait son solde bouger sans trace de l'opération qui l'a crédité.

---

### `controller/HistoriqueController.java`

**Rôle** — `GET /api/historique`.

**Le point qui mérite explication** — la lecture vient de **la base**, pas de l'objet `Client`. Un client relu revient avec un historique en mémoire vide, et la table `transactions` est la seule à porter les **vrais** horodatages. C'est exactement la raison d'être de `LigneHistorique` dans le cœur : reconstruire des `Transaction` donnerait à toutes les opérations passées la date de la relecture.

---

### `persistance/HistoriquePersistant.java`

**Rôle** — ajouter les lignes d'historique produites par une requête, sans toucher aux précédentes.

**Pourquoi il existe** — c'est le seul endroit du module où l'API du cœur ne suffisait pas, et ça vaut d'être compris.

`JdbcClientRepository.enregistrerHistorique(client)` **efface** l'historique stocké puis le réécrit depuis la mémoire du `Client` :

```java
public void enregistrerHistorique(Client client) {
    supprimerHistorique(client.getRib());   // ← efface tout
    // ... puis réinsère client.getHistorique()
}
```

C'est le bon comportement pour Swing, où le même objet `Client` vit toute la session et porte donc l'historique complet. Ce n'est **pas** utilisable depuis une API stateless : chaque requête relit un client neuf, dont l'historique en mémoire est vide. Appeler `enregistrerHistorique` après un dépôt effacerait tout le passé du client pour n'y laisser que ce dépôt.

Cette classe fait donc ce que le cœur ne sait pas encore faire : un **ajout**. Elle ne supprime jamais rien et continue la numérotation là où la table s'est arrêtée :

```java
String sql = "SELECT COALESCE(MAX(numero_ordre) + 1, 0) FROM transactions "
           + "WHERE client_rib = ?";
```

Repartir de zéro ferait échouer l'insertion sur la contrainte d'unicité `(client_rib, numero_ordre)` — ce qui, au moins, serait bruyant.

**Pourquoi ici et pas dans le cœur** — parce que cette phase ne modifie pas `bank-core`. La vraie correction est côté cœur : une méthode `ajouterAuHistorique(Client, int)`, ou mieux, un `Transaction` qui accepte son horodatage à la construction — ce que `LigneHistorique` annonce déjà comme « un ajout de trois lignes au modèle ». Le jour où ce sera fait, **cette classe disparaît** et les contrôleurs appellent le repository.

Un test (`operationsSAjoutent`) fige le comportement : avec `enregistrerHistorique`, il verrait l'historique retomber à une seule ligne après le premier dépôt.

---

### `dto/` — neuf records

**Rôle** — le contrat JSON de l'API.

**Pourquoi ne jamais sérialiser `Client` ou `Compte` directement** — Jackson sérialiserait tout ce qui ressemble à un accesseur, y compris ce qu'on n'a pas choisi d'exposer, et le JSON changerait de forme à chaque ajout de méthode au modèle. Un record dédié fige le contrat : **ce qui n'est pas listé ne sort pas**.

| DTO | Sens |
|---|---|
| `ConnexionRequete` / `ConnexionReponse` | la connexion |
| `MontantRequete` | dépôt, retrait |
| `VirementRequete` | virement — **pas de `ribEmetteur`**, c'est le jeton |
| `SituationReponse` / `CompteReponse` | la consultation |
| `OperationReponse` | ce que rend une opération réussie |
| `LigneHistoriqueReponse` | une ligne d'historique |
| `ErreurReponse` | **toute** erreur : `{ "erreur": "..." }` |

**Sur le mot de passe** — il ne peut pas fuir par accident : `Client` n'expose aucun accesseur qui le rende (un test du cœur l'interdit par réflexion depuis la phase A), et les records sont construits champ par champ. Deux tests vérifient quand même le JSON **réellement émis** — c'est lui qui part sur le réseau.

**Sur la validation** — `MontantRequete` ne porte **pas** d'annotation `@Positive`. La règle « strictement positif, deux décimales, HALF_EVEN » appartient à `Montants`, et le cœur l'applique déjà. La redoubler en annotation donnerait deux règles à maintenir et deux messages différents pour le même refus. Le DTO ne fait que transporter.

---

### `exception/GestionnaireExceptionsGlobal.java`

**Rôle** — traduire les exceptions du cœur en réponses HTTP.

**Pourquoi cette forme** — on mappe **les catégories, pas les classes feuilles**. Les quatre familles (`validation`, `etat`, `existence`, `technique`) suffisent : une nouvelle exception métier héritant de la bonne catégorie reçoit automatiquement le bon code, sans qu'on revienne ici. C'est le chantier « hiérarchie d'exceptions » qui paie une seconde fois.

Trois exceptions sont traitées à part, chacune pour une raison précise — voir le [tableau](#6-le-tableau-exception--code-http).

**Le point à retenir : le message technique ne sort pas.**

```java
@ExceptionHandler(ErreurTechniqueException.class)
public ResponseEntity<ErreurReponse> technique(ErreurTechniqueException panne) {
    JOURNAL.error("Erreur technique traitée en 500", panne);   // détail côté serveur
    return ResponseEntity.status(INTERNAL_SERVER_ERROR)
            .body(new ErreurReponse("Erreur interne du serveur."));  // phrase neutre
}
```

Une `PersistanceException` porte le détail de la panne SQL ; le renvoyer décrirait le schéma de la base à qui sonde l'API.

**Un piège rencontré, et corrigé** — le filet de sécurité `@ExceptionHandler(Exception.class)` était trop gourmand. Spring lève ses propres exceptions typées pour « chemin inconnu », « méthode non supportée », « type de contenu refusé ` — et elles portent **déjà** leur statut. Les faire tomber dans le 500 du filet transformait un banal 404 en « panne serveur ». Un test d'isolation l'a attrapé : `GET /api/comptes/456` répondait **500** au lieu de **404**. Le correctif :

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErreurReponse> imprevu(Exception panne) {
    if (panne instanceof ErrorResponse erreurHttp) {
        HttpStatusCode code = erreurHttp.getStatusCode();   // Spring a déjà tranché
        return ResponseEntity.status(code).body(new ErreurReponse(
                code.value() == 404 ? "Ressource introuvable."
                                    : "Requête refusée par le serveur."));
    }
    JOURNAL.error("Exception inattendue traitée en 500", panne);
    return ResponseEntity.status(INTERNAL_SERVER_ERROR)
            .body(new ErreurReponse("Erreur interne du serveur."));
}
```

Le message reste neutre : celui de Spring est en anglais et récite le chemin demandé.

---

### `exception/GestionnaireExceptionsAuthentification.java`

**Rôle** — les mêmes exceptions, mais en contexte de connexion, où elles ne veulent pas dire la même chose.

**Pourquoi un second gestionnaire** — c'est le fichier qui protège la non-divulgation au niveau HTTP.

Partout ailleurs, une `ClientIntrouvableException` signale une ressource absente : **404**. À la connexion, la **même** exception signale « identifiants refusés ». Un 404 y annoncerait au monde que ce RIB n'existe pas, pendant qu'un mot de passe faux rendrait autre chose. Le code HTTP redeviendrait l'oracle que le cœur s'interdit depuis la phase A : **sept tests y veillent côté métier, il aurait suffi de la couche web pour tout perdre**.

```java
@RestControllerAdvice(assignableTypes = AuthController.class)  // ce contrôleur seul
@Order(Ordered.HIGHEST_PRECEDENCE)                             // gagne sur le global
public class GestionnaireExceptionsAuthentification {

    @ExceptionHandler(ClientIntrouvableException.class)
    public ResponseEntity<ErreurReponse> identifiantsRefuses(ClientIntrouvableException refus) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErreurReponse(refus.getMessage()));
    }
}
```

La distinction est portée par le **contexte** (`assignableTypes`), pas par un `try/catch` dans le contrôleur. Le gestionnaire global garde ses codes pour tout le reste.

**Un écart assumé** — `CompteVerouilleException` rend **423 Locked**, avec le délai restant. Après trois échecs, la réponse cesse donc d'être indiscernable. Le cœur fait déjà ce choix, et pour une bonne raison : un utilisateur bloqué doit comprendre pourquoi ses bons identifiants sont refusés, sans quoi il réessaie indéfiniment. Ce que ça révèle est marginal — il faut déjà avoir provoqué trois échecs sur ce compte pour l'apprendre.

---

### `resources/application.yml`

**Rôle** — la configuration : port, URL de la base, secret JWT.

**Le point à retenir** — le secret est lu d'une variable d'environnement, avec un défaut de développement :

```yaml
bank:
  jwt:
    secret: ${BANK_JWT_SECRET:secret-de-developpement-a-remplacer-32c}
```

Quiconque connaît ce secret peut **forger un jeton pour n'importe quel RIB**. Il doit être remplacé en dehors d'un poste de travail.

---

### Les tests

| Classe | Base | Couvre |
|---|---|---|
| `BaseTestApi` | — | socle : `MockMvc`, aide `jetonDe(...)`, remise à zéro du compteur d'échecs |
| `AuthentificationApiTest` | `test-auth` | connexion réussie, refus, **non-divulgation HTTP** |
| `VerrouillageApiTest` | `test-verrouillage` | 3 échecs → 423, isolation entre clients, jeton antérieur |
| `CompteApiTest` | `test-compte` | consultation, dépôt, retrait, refus métier, absence de mot de passe dans le JSON |
| `VirementApiTest` | `test-virement` | les deux côtés bougent, destinataire inconnu, ordre de validation |
| `HistoriqueApiTest` | `test-historique` | les opérations **s'ajoutent**, trace des deux côtés, isolation |
| `LivretAApiTest` | `test-livret` | ouverture 201, seconde ouverture 409 |
| `AccesProtegeApiTest` | `test-acces` | sans jeton, jeton forgé, **isolation entre clients** |
| `JwtServiceTest` | — | unitaire, sans Spring : signature, altération, secret trop court |

**Deux choix de conception des tests** méritent d'être expliqués.

*Une base par classe.* Chaque classe déclare son `bank.base.url`. Deux valeurs différentes donnent deux contextes Spring, donc deux bases indépendantes : aucune classe ne travaille sur ce qu'une autre a laissé.

*Les soldes sont vérifiés en delta, jamais en valeur absolue.* Les tests d'une même classe partagent une base ; exiger « 500,00 € au départ » rendrait chaque test dépendant de ceux qui l'ont précédé.

Et le compteur d'échecs est remis à zéro avant chaque test — sans ça, deux tentatives ratées dans deux tests différents s'additionneraient et le troisième recevrait un 423 inattendu.

---

## 6. Le tableau exception → code HTTP

| Exception (`com.example.bank.core.exception.*`) | Code | Corps | Pourquoi |
|---|---|---|---|
| `validation.MontantInvalideException` | **400** | message du cœur | catégorie `validation` : la requête elle-même est mauvaise |
| `validation.VirementVersSoiMemeException` | **400** | message du cœur | idem |
| `validation.DureeInvalideException` | **400** | message du cœur | idem |
| `etat.SoldeInsuffisantException` | **409** | message du cœur | la requête est bien formée, c'est l'**état** qui refuse |
| `etat.OperationInterditeException` | **409** | message du cœur | idem (compte bloqué, fermé) |
| `etat.PlafondDepasseException` | **409** | message du cœur | idem |
| `etat.CompteVerouilleException` *(hors connexion)* | **423** | message du cœur | plus précis que 409 : verrouillage temporaire, le message porte les minutes |
| `etat.CompteVerouilleException` *(à la connexion)* | **423** | message du cœur | écart assumé à la non-divulgation, voir §5 |
| `existence.ClientIntrouvableException` *(hors connexion)* | **404** | message du cœur | ressource absente |
| **`existence.ClientIntrouvableException` *(à la connexion)*** | **401** | `Nom d'utilisateur ou mot de passe incorrect.` | **non-divulgation** : identique pour RIB inconnu et mot de passe faux |
| `existence.LivretAAbsentException` | **404** | message du cœur | ressource absente |
| `existence.LivretADejaExistantException` | **409** | message du cœur | existe **en trop** — un 404 serait le contresens du tableau |
| `technique.PersistanceException` | **500** | `Erreur interne du serveur.` | détail journalisé côté serveur uniquement |
| `technique.InvariantRompuException` | **500** | `Erreur interne du serveur.` | idem |
| `technique.ParametreInvalideException` | **500** | `Erreur interne du serveur.` | idem |
| `BanqueException` *(non classée)* | **400** | message du cœur | filet, avec un `WARN` au journal |
| — *(pas de jeton valide)* | **401** | `Authentification requise.` | `AuthenticationEntryPoint` |
| `HttpMessageNotReadableException` | **400** | `Corps de requête absent ou mal formé.` | JSON illisible |
| Exceptions Spring typées (`ErrorResponse`) | **leur code** | neutre | 404 chemin inconnu, 405 méthode, 415 type |
| Tout le reste | **500** | `Erreur interne du serveur.` | rien ne sort en trace Java |

---

## 7. Ce qui a été volontairement laissé de côté

Ces limites sont des **décisions**, pas des oublis. Mieux vaut les lire ici que les découvrir en production.

### Une seule connexion JDBC, partagée

`JdbcClientRepository` reçoit une `Connection`, pas une `DataSource`, et `sauvegarderEnsemble` pose `setAutoCommit(false)` puis valide sur **cette** connexion. Deux requêtes web concurrentes s'entremêleraient donc dans la même transaction.

C'est acceptable pour une application de démonstration mono-utilisateur — et `BaseDeDonneesH2` annonce déjà que cette classe « disparaîtra au profit d'une `DataSource` ». **Le passage à un pool demande de changer la signature du repository, donc de toucher au cœur** : hors périmètre de cette phase, mais c'est le premier chantier à ouvrir avant tout usage réellement concurrent.

### Pas de refresh token, pas de révocation

Un jeton vaut une heure et ne peut pas être annulé avant. Conséquences concrètes, dont une est figée par un test :

- verrouiller un compte n'invalide **pas** les jetons déjà délivrés (`jetonAnterieurResteValable`) ;
- il n'y a pas de « déconnexion » côté serveur — le client jette son jeton, c'est tout.

Une vraie révocation demande de retenir quelque chose côté serveur (liste noire, version de jeton par client), c'est-à-dire de renoncer en partie au *stateless*. C'est un arbitrage à faire consciemment, pas un ajout mécanique.

### Pas de rôles ni de permissions

La liste d'autorités du filtre est vide. Être authentifié suffit à tout faire — **sur ses propres comptes**. Il n'y a ni conseiller, ni administrateur, ni consultation d'un compte tiers. Le jour où ces rôles existeront, c'est `SecurityConfig` et `ClientAuthentifie` qu'il faudra reprendre.

### Pas d'inscription ni de changement de mot de passe

`JdbcClientRepository.creer` et `RepertoireMotsDePasse.enregistrer` existent et sont testés, mais aucun endpoint ne les expose. Les seuls clients sont ceux de la démonstration.

### Pas de CORS configuré

Un front servi depuis une autre origine (typiquement `localhost:5173` en développement) sera bloqué par le navigateur. C'est une ligne à ajouter dans `SecurityConfig` — mais elle appartient à la phase H, qui saura d'où le front est servi.

### Pas de pagination sur l'historique

`GET /api/historique` rend tout. Avec deux clients de démonstration c'est sans conséquence ; avec un vrai volume, il faudra `?page=&taille=`.

### Pas de Swagger / OpenAPI

Ajouter `springdoc-openapi` serait presque gratuit, mais la documentation de l'API n'était pas l'objet de cette phase — et un Swagger généré sans être relu documente surtout les défauts du code.

### Pas de HTTPS

Le jeton voyage en clair dans un en-tête. En HTTP simple, quiconque écoute le réseau le récupère et devient le client. **Le JWT ne protège rien sans TLS** ; c'est la terminaison TLS (reverse proxy ou configuration du serveur) qui manque, pas le code.

---

## Pour reprendre à la phase H

Ce module expose l'API ; il n'y a pas de front. Les trois points à traiter en premier, dans cet ordre :

1. **CORS**, sans quoi rien ne marchera depuis un navigateur.
2. **La connexion partagée** — dès que deux utilisateurs se servent de l'application en même temps.
3. **L'appel `enregistrerHistorique` côté cœur**, pour que `HistoriquePersistant` disparaisse.
