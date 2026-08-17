# Analyse du projet `bank`

**Date :** 17 août 2026
**Périmètre analysé :** `/home/immouh/git/bank` (543 lignes, 9 classes Java, 1 `pom.xml`)
**Objectif :** état des lieux avant agrandissement du projet.

---

## 1. Résumé exécutif

Le projet est une **application bancaire de bureau en Java/Swing** : authentification, consultation de compte, création d'un Livret A, virement entre clients, historique des transactions. Les bases de la POO sont là (encapsulation, enum, composition, collections, héritage de `JFrame`, classes anonymes pour les listeners) et **le code compile sans erreur**.

En revanche, dans son état actuel le projet **n'est pas extensible tel quel**. Trois raisons, par ordre de gravité :

1. **Le cœur métier est faux.** Un dépôt *écrase* le solde au lieu de l'incrémenter, un retrait ne vérifie aucun plafond, et tout virement sortant est débité du **Livret A** et non du compte courant. Vérifié par exécution (section 4).
2. **Il n'y a aucune couche.** Le métier, l'interface graphique et les "données" (un `client[2]` codé en dur dans `main`) sont entremêlés. Ajouter une fonctionnalité oblige à toucher l'IHM.
3. **Le projet est physiquement rangé dans `.idea/`**, le dossier de configuration d'IntelliJ, et mélange deux paradigmes incompatibles (WAR Jakarta EE + application Swing avec un `main`).

**Conclusion :** avant d'ajouter quoi que ce soit, il faut corriger le métier, le sortir de `.idea/`, et le découper en couches. C'est un chantier de nettoyage de 2 à 3 jours qui rendra tout le reste possible.

---

## 2. Inventaire du code

### 2.1 Arborescence réelle

```
/home/immouh/git/bank/            <- racine du dépôt git
└── .idea/                        <- ⚠ dossier de config IntelliJ
    ├── bank.iml, misc.xml, compiler.xml, modules.xml, ...
    └── bank/                     <- ⚠ le VRAI projet est ici
        ├── pom.xml
        ├── mvnw, mvnw.cmd, .mvn/wrapper/
        ├── .idea/                <- ⚠ un second .idea imbriqué
        └── src/main/
            ├── java/com/example/bank/   (9 fichiers .java + 1 k.ts vide)
            └── resources/META-INF/beans.xml
```

### 2.2 Les classes

| Fichier | L. | Rôle | Couche |
|---|---:|---|---|
| [client.java](.idea/bank/src/main/java/com/example/bank/client.java) | 112 | Entité client + toute la logique métier | Modèle + Métier |
| [transaction.java](.idea/bank/src/main/java/com/example/bank/transaction.java) | 26 | Entité transaction + `enum TypeTransaction` | Modèle |
| [Main.java](.idea/bank/src/main/java/com/example/bank/Main.java) | 33 | Point d'entrée, jeu de données codé en dur | Amorçage |
| [GestionnaireInterfaceGraphique.java](.idea/bank/src/main/java/com/example/bank/GestionnaireInterfaceGraphique.java) | 106 | Fenêtre de connexion + logique d'authentification | IHM + Métier |
| [ClientInfoWindow.java](.idea/bank/src/main/java/com/example/bank/ClientInfoWindow.java) | 70 | Tableau de bord du client | IHM |
| [VirementWindow.java](.idea/bank/src/main/java/com/example/bank/VirementWindow.java) | 69 | Formulaire de virement | IHM |
| [historique.java](.idea/bank/src/main/java/com/example/bank/historique.java) | 39 | Fenêtre listant les transactions | IHM |
| [HelloResource.java](.idea/bank/src/main/java/com/example/bank/HelloResource.java) | 13 | Endpoint REST `/api/hello-world` | **Mort** |
| [HelloApplication.java](.idea/bank/src/main/java/com/example/bank/HelloApplication.java) | 8 | Déclaration JAX-RS | **Mort** |
| `k.ts` | 0 | Fichier TypeScript vide | **À supprimer** |

### 2.3 Flux applicatif

```
Main.main()
  └─ crée client[2] en dur, applique quelques opérations
  └─ new GestionnaireInterfaceGraphique(clients)     [fenêtre de login]
        └─ (bouton) parcourt clients[], compare nom + mdp
              └─ new ClientInfoWindow(c, clients, nom, rib, solde, ...)
                    ├─ (bouton) new VirementWindow(c, clients)
                    │       └─ cherche le RIB destinataire, appelle c.effectuerVirement(...)
                    └─ (bouton) new historique(listeTransactions)
```

---

## 3. Architecture : les problèmes structurels

### 3.1 Deux projets en un (bloquant)

Le `pom.xml` déclare `<packaging>war</packaging>` et dépend de **Jakarta EE** (CDI, JAX-RS, Servlet API, toutes en `provided`). Mais le code livré est une **application Swing de bureau lancée par `Main.main()`**.

Ces deux modèles sont incompatibles :

- Un **WAR** ne se lance pas avec `main()` — il se déploie dans un serveur (Tomcat, WildFly). Le `main` ne sera jamais exécuté.
- Une **application Swing** se package en JAR exécutable et n'a besoin d'aucune dépendance Jakarta.
- `HelloResource` / `HelloApplication` / `beans.xml` sont le squelette généré par l'assistant « Jakarta EE » d'IntelliJ. Personne ne les appelle.

**Il faut choisir.** C'est la décision d'architecture n°1 (voir section 7).

### 3.2 Aucune séparation des responsabilités

Tout est mélangé. Deux exemples concrets :

- **L'authentification est écrite dans un `ActionListener`** ([GestionnaireInterfaceGraphique.java:78-96](.idea/bank/src/main/java/com/example/bank/GestionnaireInterfaceGraphique.java#L78-L96)). Impossible de tester la connexion sans ouvrir une fenêtre.
- **La règle « solde suffisant ? » est dans `client`, mais le message d'erreur part en `System.out.println`** ([client.java:76](.idea/bank/src/main/java/com/example/bank/client.java#L76)). Le métier « parle » à la console : l'utilisateur ne voit jamais l'erreur, et le métier est couplé à un canal d'affichage.

Il manque les couches : `model` (données pures), `service` (règles métier), `repository` (stockage), `ui` (affichage).

### 3.3 Aucune persistance

`Main` crée `new client[2]` en dur. À la fermeture, **tout est perdu**. Il n'existe aucun moyen de créer un compte depuis l'application : le tableau est de taille fixe, donc même en mémoire on ne peut pas dépasser 2 clients.

### 3.4 État dupliqué dans l'IHM

`ClientInfoWindow` reçoit **à la fois** l'objet `c` **et** une copie de chacun de ses champs :

```java
public ClientInfoWindow(client c, client[] l, String nom, int rib, float solde,
                        boolean livret, float somme, List<transaction> h)
```

Les `JLabel` sont construits à partir de la copie. Conséquence directe : **après un virement, les soldes affichés ne changent pas** — la fenêtre montre un instantané figé au moment de la connexion. 8 paramètres dont 6 sont déductibles de `c`.

### 3.5 Configuration incohérente

| Élément | Valeur | Problème |
|---|---|---|
| `pom.xml` source/target | 11 | |
| `.idea/misc.xml` | `JDK_11`, `11 (WSL)` | |
| `.idea/bank/.idea/misc.xml` | `JDK_19`, `corretto-19` | ⚠ contradiction |
| JDK installé sur la machine | **21** | ⚠ non aligné |
| Wrapper Maven | 3.8.5 | |
| Maven installé | **3.6.3** | ⚠ non aligné |
| `.idea/misc.xml` → chemin du pom | `$PROJECT_DIR$/bank-system/bank/pom.xml` | ⚠ **ce dossier n'existe pas** |

Le `.gitignore` ignore par ailleurs `.idea/modules.xml`, `.idea/compiler.xml` et `*.iml`… mais ces fichiers sont commités quand même, parce que le projet est *dans* `.idea/`.

---

## 4. Bugs du métier (vérifiés par exécution)

J'ai compilé le modèle et exécuté une sonde. **Sortie réelle du programme :**

```
apres depot 500        -> compte=500.0
apres 2e depot 500     -> compte=500.0          ⚠ le solde n'a pas bougé
apres retrait 200      -> compte=300.0
apres virement 300     -> c0.compte=300.0  c0.livretA=700.0  c1.compte=300.0
retrait 99999 sur c1   -> -99699.0              ⚠ découvert illimité
depot -1000            -> c0.compte=-1000.0     ⚠ dépôt négatif accepté
```

### BUG-1 — `=+` au lieu de `+=` : le dépôt écrase le solde 🔴 **Critique**

[client.java:93](.idea/bank/src/main/java/com/example/bank/client.java#L93)

```java
public void effectuerDepot(float montant) {
    soldeCompte =+ montant;   // <-- affectation + plus unaire
```

`soldeCompte =+ montant` est interprété par Java comme `soldeCompte = (+montant)`. Le solde est **remplacé**, pas incrémenté. Deux dépôts de 500 € laissent le compte à 500 €. Le compilateur ne dit rien : c'est syntaxiquement valide.

**Correctif :** `soldeCompte += montant;`

### BUG-2 — Le virement sortant est débité du Livret A 🔴 **Critique**

[client.java:68-90](.idea/bank/src/main/java/com/example/bank/client.java#L68-L90)

La méthode mélange deux opérations différentes sous un seul booléen :

| `versLivretA` | Comportement réel | `dest` utilisé ? |
|---|---|---|
| `true` | Virement **interne** : compte → Livret A du *même* client | non, ignoré |
| `false` | Virement **externe** : **Livret A** → compte de `dest` | oui |

Or `VirementWindow` appelle **toujours** `effectuerVirement(montant, false, dest)` ([VirementWindow.java:48](.idea/bank/src/main/java/com/example/bank/VirementWindow.java#L48)). Donc **tout virement vers un autre client puise dans l'épargne**, jamais dans le compte courant. Confirmé ci-dessus : le virement de 300 € laisse `c0.compte` à 300 et fait tomber `c0.livretA` de 1000 à 700.

Effet de bord : un client sans Livret A ne peut **jamais** faire de virement (`soldeLivretA` vaut 0, la condition échoue silencieusement).

**Correctif :** séparer en deux méthodes distinctes — `virerVersLivretA(montant)` et `virerVers(destinataire, montant)`.

### BUG-3 — Aucune validation du montant 🔴 **Critique**

Aucune méthode ne vérifie `montant > 0`. Un dépôt de −1000 € est accepté et met le compte à −1000 (vérifié). Un retrait négatif crédite le compte.

### BUG-4 — Le retrait ne vérifie pas le solde 🔴 **Critique**

[client.java:96-101](.idea/bank/src/main/java/com/example/bank/client.java#L96-L101)

```java
public float effectuerRetrait(float montant) {
    soldeCompte -= montant;      // aucun contrôle
```

Retrait de 99 999 € sur un compte à 300 € → solde à **−99 699 €** (vérifié). Le virement contrôle le solde, le retrait non : incohérence.

### BUG-5 — Le destinataire n'a pas de trace de l'opération 🟠 **Majeur**

[client.java:84-85](.idea/bank/src/main/java/com/example/bank/client.java#L84-L85) : `dest.soldeCompte += montant;` crédite le destinataire, mais `ajouterTransaction(...)` n'est appelé que sur l'émetteur. **Le bénéficiaire voit son solde augmenter sans aucune ligne dans son historique.**

### BUG-6 — Virement vers soi-même autorisé 🟠 **Majeur**

`VirementWindow` ne vérifie pas que le RIB saisi est différent de celui de l'émetteur. Cela permet de transférer librement Livret A → compte courant en contournant la règle métier.

### BUG-7 — Message d'échec de connexion répété 🟡 **Mineur**

[GestionnaireInterfaceGraphique.java:84-96](.idea/bank/src/main/java/com/example/bank/GestionnaireInterfaceGraphique.java#L84-L96) : une accolade mal placée met le `if (!connexionReussie)` **à l'intérieur** de la boucle `for`. Le message « Nom utilisateur ou mdp faux » s'affiche donc une fois **par client non correspondant**. Avec 1000 clients, 999 messages.

De plus, l'échec ne produit **aucun retour visuel** — juste un `System.out.println` que l'utilisateur ne voit pas.

### BUG-8 — La date de transaction n'est jamais enregistrée 🟠 **Majeur**

[transaction.java:12](.idea/bank/src/main/java/com/example/bank/transaction.java#L12) :

```java
public transaction(float montant, TypeTransaction type, String descriptions) {
    this.montant = montant;
    LocalDate localDateTime;      // <-- variable locale, jamais assignée, jamais utilisée
```

L'intention était d'horodater. Il manque un champ `private LocalDateTime date;` et son initialisation. **L'historique bancaire est donc sans dates** — inutilisable en l'état.

### BUG-9 — `float` pour des montants monétaires 🟠 **Majeur**

`float` a ~7 chiffres significatifs et ne représente pas exactement 0,1. Sur des sommes réalistes (> 100 000 €) et après quelques centaines d'opérations, les centimes dérivent. **Un système bancaire ne doit jamais utiliser `float`/`double`.**

**Correctif :** `BigDecimal` (avec `RoundingMode.HALF_EVEN`), ou stocker les centimes en `long`.

---

## 5. Sécurité

| # | Constat | Gravité |
|---|---|---|
| S-1 | **Mot de passe stocké en clair** dans `client.mdp`. Aucun hachage. | 🔴 |
| S-2 | **Le champ mot de passe est un `JTextField`**, pas un `JPasswordField` ([GestionnaireInterfaceGraphique.java:28](.idea/bank/src/main/java/com/example/bank/GestionnaireInterfaceGraphique.java#L28)) → le mot de passe s'affiche en clair à l'écran, et le placeholder « Mot de passe » aussi. | 🔴 |
| S-3 | `getMdp()` **expose publiquement** le mot de passe. Un getter de mot de passe ne devrait pas exister : c'est à `client` de faire `verifierMotDePasse(saisie)`. | 🔴 |
| S-4 | Comparaison par `equals()` — sensible aux attaques temporelles (mineur ici, mais mauvaise habitude). Utiliser `MessageDigest.isEqual`. | 🟡 |
| S-5 | Aucune limitation du nombre de tentatives de connexion. | 🟠 |
| S-6 | `getHistorique()` renvoie la **liste interne mutable** : n'importe qui peut faire `c.getHistorique().clear()` et effacer l'historique. Retourner `List.copyOf(...)` ou `Collections.unmodifiableList(...)`. | 🟠 |
| S-7 | `dest.soldeCompte += montant` accède directement au champ privé d'une autre instance ([client.java:84](.idea/bank/src/main/java/com/example/bank/client.java#L84)). Légal en Java, mais contourne toute règle de validation. | 🟠 |
| S-8 | Aucun contrôle d'unicité des RIB : deux clients peuvent avoir le même RIB, le virement irait au premier trouvé. | 🟠 |

---

## 6. Qualité du code

### 6.1 Conventions de nommage

Trois classes commencent par une minuscule : `client`, `transaction`, `historique`. La convention Java impose `PascalCase` pour les classes.

Pire, `historique` désigne **deux choses différentes** : la classe fenêtre *et* le champ `List<transaction> historique` dans `client`. Cela produit du code ambigu :

```java
new historique(historique);   // ClientInfoWindow.java:64 — classe ? champ ?
```

**Renommage à faire :** `client` → `Client`, `transaction` → `Transaction`, `historique` → `HistoriqueWindow`.

### 6.2 Code mort et résidus

- `k.ts` — fichier TypeScript vide dans un dossier de sources Java.
- `HelloResource`, `HelloApplication`, `beans.xml` — squelette Jakarta EE inutilisé.
- [client.java:40](.idea/bank/src/main/java/com/example/bank/client.java#L40) — `Random random = new Random();` : champ d'instance non utilisé (le RIB est passé au constructeur), et non `private`.
- [client.java:46](.idea/bank/src/main/java/com/example/bank/client.java#L46) — la génération de RIB est commentée.
- [Main.java:11-31](.idea/bank/src/main/java/com/example/bank/Main.java#L11-L31) — 15 lignes sur 33 sont du code commenté.
- Faute de frappe : `client(String nom, String mdp, int **rin**)`.
- Fautes dans les messages utilisateur : « virement effectuer avec succes » → « effectué avec succès ».

### 6.3 Problèmes Swing

- **Swing n'est pas démarré sur l'EDT.** `Main` construit la fenêtre depuis le thread principal. Il faut `SwingUtilities.invokeLater(() -> new ...)`. C'est une violation du modèle de threading de Swing, source de bugs d'affichage aléatoires.
- [ClientInfoWindow.java:25](.idea/bank/src/main/java/com/example/bank/ClientInfoWindow.java#L25) — `new GridLayout(3, 2)` = 6 cellules, mais on ajoute **5 ou 6** composants selon `if (livret)`. La mise en page casse quand le client n'a pas de Livret A.
- [GestionnaireInterfaceGraphique.java:68-69](.idea/bank/src/main/java/com/example/bank/GestionnaireInterfaceGraphique.java#L68-L69) — `setPreferredSize(new Dimension(1, 1))` alors que le commentaire dit « 200 pixels ».
- Aucune fenêtre n'est fermée : la fenêtre de login reste ouverte après connexion, on peut ouvrir 10 fenêtres d'infos client. Pas de déconnexion.
- 10 avertissements du compilateur (`-Xlint:all`) : `serialVersionUID` manquant partout, `this-escape` dans tous les constructeurs de `JFrame`.

### 6.4 Tests

**Zéro test.** JUnit 5 est bien déclaré dans le `pom.xml` mais il n'existe même pas de dossier `src/test/`. Les bugs BUG-1 à BUG-4 auraient été détectés par un seul test de 5 lignes :

```java
@Test
void deuxDepotsSAdditionnent() {
    Compte c = new Compte();
    c.deposer(500);
    c.deposer(500);
    assertEquals(1000, c.getSolde());   // échoue aujourd'hui : 500
}
```

---

## 7. Décision d'architecture à prendre en premier

Avant tout développement, il faut trancher :

| Option | Description | Pour | Contre |
|---|---|---|---|
| **A — Bureau** | Garder Swing (ou passer à JavaFX), `packaging jar`, supprimer Jakarta EE | Continuité avec l'existant, aucune infra à installer | Pas de multi-utilisateur réel |
| **B — Web** | Réécrire l'IHM en REST + front, garder le WAR/Tomcat | Utilise le squelette JAX-RS déjà présent, multi-poste | Toute l'IHM Swing est à jeter |
| **C — Hybride** *(recommandé)* | Extraire le métier dans un module `core` pur Java, puis brancher **deux** frontaux : Swing aujourd'hui, REST plus tard | Le métier est testable et réutilisable ; on ne jette rien ; on peut migrer progressivement | Un peu plus de mise en place au départ |

**Recommandation : option C.** Elle correspond exactement à l'objectif d'« essayer tout ce qu'on a appris » : POO propre, tests, persistance, puis web — sans repartir de zéro à chaque étape.

### Cible proposée

```
bank/                            <- à la racine du dépôt, plus dans .idea/
├── pom.xml                      <- parent, <packaging>pom</packaging>
├── bank-core/                   <- Java pur, AUCUNE dépendance UI/web
│   └── src/main/java/com/example/bank/
│       ├── model/       Client, Compte, LivretA, Transaction, TypeTransaction
│       ├── service/     BanqueService, AuthService  (règles métier)
│       ├── repository/  ClientRepository (interface)
│       │                 └─ InMemoryClientRepository, JdbcClientRepository
│       └── exception/   SoldeInsuffisantException, MontantInvalideException, ...
│   └── src/test/java/           <- les tests JUnit vivent ici
├── bank-swing/                  <- IHM bureau, dépend de bank-core
└── bank-web/                    <- (plus tard) WAR JAX-RS, dépend de bank-core
```

---

## 8. Plan d'agrandissement

### Phase 0 — Assainissement (prérequis, ~1 jour)

1. `git mv` du projet hors de `.idea/` vers la racine du dépôt ; supprimer le `.idea` imbriqué.
2. Supprimer `k.ts`, `HelloResource`, `HelloApplication`, `beans.xml`, le code commenté, le champ `Random` inutilisé.
3. Renommer `client`→`Client`, `transaction`→`Transaction`, `historique`→`HistoriqueWindow`.
4. Aligner les versions : JDK **21** partout (`pom.xml`, `misc.xml`), corriger le chemin `bank-system/` fantôme.
5. **Corriger BUG-1 à BUG-4** (dépôt, virement, validation du montant, contrôle du solde). Ce sont 10 lignes de code et cela rend l'application utilisable.

### Phase 1 — Modèle et métier propres

*Notions mises en pratique : héritage, classes abstraites, interfaces, exceptions personnalisées, `BigDecimal`, immutabilité.*

6. Introduire une hiérarchie `Compte` (abstraite) → `CompteCourant`, `LivretA`, avec `LivretA` portant son taux d'intérêt. Un `Client` possède une `List<Compte>` — cela supprime les champs `livretAExiste` / `soldeLivretA` et permet d'ajouter d'autres produits (LDD, PEL, compte joint) sans toucher au reste.
7. Passer les montants en `BigDecimal`.
8. Remplacer les `System.out.println` par des exceptions métier (`SoldeInsuffisantException`, `MontantInvalideException`, `CompteIntrouvableException`), attrapées par l'IHM qui affiche un `JOptionPane`.
9. Rendre `Transaction` **immuable** (champs `final`, pas de setters) avec un vrai champ `LocalDateTime date`, un compte source et un compte destination.
10. Extraire `BanqueService` : `deposer`, `retirer`, `virer`, `ouvrirCompte`, `creerClient`.

### Phase 2 — Collections et recherche

*Notions : `Map`, `Stream`, `Comparator`, `Optional`.*

11. Remplacer `client[]` par un `Map<String, Client>` indexé par RIB → recherche en O(1) au lieu du parcours linéaire, et unicité du RIB garantie.
12. Générer les RIB automatiquement (la logique est déjà commentée dans le code) avec contrôle d'unicité, au format IBAN si possible.
13. Filtrer/trier l'historique avec l'API Stream : par date, par type, par plage de montants.

### Phase 3 — Tests

*Notions : JUnit 5, TDD, couverture.*

14. Créer `src/test/java`, ajouter `maven-surefire-plugin`.
15. Tests unitaires du métier : dépôt, retrait, solde insuffisant, montant négatif, virement, virement vers soi-même, virement vers un RIB inexistant, authentification.
16. Viser 80 % de couverture sur `bank-core` (plugin JaCoCo). L'IHM n'a pas besoin d'être couverte.

### Phase 4 — Persistance

*Notions : JDBC, SQL, DAO, transactions.*

17. Définir l'interface `ClientRepository` (`findByRib`, `findByNom`, `save`, `findAll`).
18. Implémentation 1 : `InMemoryClientRepository` (pour les tests).
19. Implémentation 2 : `JdbcClientRepository` sur **SQLite** ou **H2** (aucun serveur à installer) — tables `client`, `compte`, `transaction`.
20. Points clés à traiter : `PreparedStatement` uniquement (jamais de concaténation SQL), et **un virement doit être une transaction SQL atomique** (débit + crédit valident ou échouent ensemble).

### Phase 5 — Sécurité

21. Hacher les mots de passe avec **BCrypt** (`org.mindrot:jbcrypt` ou `spring-security-crypto`) ; supprimer `getMdp()`, le remplacer par `verifierMotDePasse(String)`.
22. Passer le champ de saisie en `JPasswordField`.
23. Verrouillage du compte après 3 tentatives échouées.
24. Journal d'audit horodaté des connexions et des opérations sensibles.

### Phase 6 — IHM

*Notions : MVC, patron Observateur.*

25. Corriger l'état figé : `ClientInfoWindow` ne prend plus que `Client` + `BanqueService`, et **relit** les soldes à chaque affichage.
26. Appliquer le patron **Observateur** : le `Compte` notifie ses vues à chaque changement de solde, l'affichage se met à jour tout seul.
27. Démarrer Swing sur l'EDT via `SwingUtilities.invokeLater`.
28. Gérer le cycle de vie des fenêtres : fermer le login après connexion, bouton de déconnexion, une seule fenêtre client à la fois.
29. Ajouter les écrans manquants : inscription, dépôt, retrait, ouverture de compte, relevé imprimable.

### Phase 7 — Fonctionnalités bancaires

30. Calcul et versement des intérêts du Livret A (avec plafond réglementaire de 22 950 €).
31. Virements programmés / récurrents (`ScheduledExecutorService`).
32. Découvert autorisé paramétrable par compte, avec agios.
33. Catégorisation des dépenses et statistiques mensuelles.
34. Export du relevé en CSV puis en PDF.

### Phase 8 — Web (optionnel, valorise le squelette existant)

35. Module `bank-web` : ressources JAX-RS `/api/clients`, `/api/comptes`, `/api/virements`, sérialisation JSON.
36. Authentification par **JWT**.
37. Front simple (HTML/JS ou React) consommant l'API.
38. `Dockerfile` + `docker-compose` (application + base de données).

---

## 9. Tableau de priorités

| Priorité | Action | Effort | Impact |
|---|---|---|---|
| 🔴 **1** | Corriger BUG-1 (`=+`) | 1 min | L'application devient utilisable |
| 🔴 **2** | Corriger BUG-2 (virement / Livret A) | 30 min | Le virement fait ce qu'il annonce |
| 🔴 **3** | Valider les montants + contrôler le solde au retrait (BUG-3, BUG-4) | 30 min | Fin des soldes négatifs |
| 🔴 **4** | Sortir le projet de `.idea/` | 15 min | Débloque toute la suite |
| 🔴 **5** | Hacher les mots de passe + `JPasswordField` (S-1, S-2, S-3) | 1 h | Sécurité minimale |
| 🟠 **6** | Découper en `model` / `service` / `repository` / `ui` | 1 j | Rend l'extension possible |
| 🟠 **7** | Premiers tests JUnit sur le métier | 0,5 j | Empêche les régressions |
| 🟠 **8** | `float` → `BigDecimal` | 2 h | Exactitude des montants |
| 🟠 **9** | Horodater les transactions (BUG-8) | 15 min | Historique exploitable |
| 🟡 **10** | Renommer les classes (PascalCase) | 30 min | Lisibilité |
| 🟡 **11** | Supprimer le code mort (Jakarta, `k.ts`, commentaires) | 30 min | Clarté |
| 🟡 **12** | Persistance SQLite/H2 | 1–2 j | Les données survivent |

---

## 10. Ce qui est déjà bien

Il ne faut pas jeter le travail existant — plusieurs choses sont correctement faites :

- **Encapsulation** respectée dans `client` : champs `private`, `final` sur `nom` et `rib`, getters explicites.
- **`enum TypeTransaction`** — le bon outil au bon endroit, bien mieux que des chaînes de caractères.
- **Composition** `Client` → `List<Transaction>` : la modélisation de l'historique est juste.
- **`toString()` redéfini** sur `transaction`, exploité directement par `historique`.
- **`VirementWindow` est la classe la mieux écrite du projet** : `try/catch` sur `NumberFormatException`, retours utilisateur via `JOptionPane`, cas d'erreur « destinataire introuvable » traité. C'est le niveau de qualité à généraliser partout ailleurs.
- **Effet placeholder** sur les champs de saisie via `FocusListener` — soin apporté à l'expérience utilisateur.
- **`historique`** utilise une lambda (`e -> dispose()`) et un `JScrollPane` : plus moderne et plus correct que les autres fenêtres.
- Le code **compile sans erreur** et est **commenté**.

Le projet montre que les fondamentaux sont acquis. Ce qui manque, c'est l'**architecture** (des couches), la **rigueur du métier** (validation, exactitude) et le **filet de sécurité** (les tests). Les phases 0 à 3 du plan comblent exactement ces trois manques.
