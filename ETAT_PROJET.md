# État du projet `bank` — point de reprise

**Dernière mise à jour : 31 août 2026** · branche `main`, working tree propre · `mvn test` → **97 tests, 0 échec**

Document de reprise : *où on en est, ce qui est acquis, ce qui reste, et par quoi recommencer.*
Pour le détail technique (règles de conception, tableaux de tests, conventions), voir [README.md](README.md).
Pour l'audit d'origine du projet avant nettoyage, voir [ANALYSE_PROJET.md](ANALYSE_PROJET.md).

---

## 1. En une page

Application bancaire de bureau en **Java 21 / Maven multi-module / Swing**, avec suite de tests JUnit 5 et CI GitHub Actions.

Fonctionnalités qui marchent aujourd'hui : connexion, tableau de bord client, Livret A, virement entre clients (avec traçabilité des deux côtés), historique des transactions.

| | |AC
|---|---|
| Modules | `bank-core` (métier pur) + `bank-swing` (IHM) |
| Code source | 56 fichiers Java — 1 861 lignes de main, 1 071 lignes de test |
| Tests | 97 JUnit 5, tous verts — **uniquement sur le package `offre`** |
| CI | `.github/workflows/ci.yml` — compile + test + rapport, sur push et PR vers `main` |
| Persistance | **aucune** — tout est en mémoire, perdu à la fermeture |

**La phrase à retenir sur l'état actuel :** l'architecture est saine et le métier est correct, mais deux modélisations du compte coexistent sans se parler (`Client` avec ses soldes en dur, et le package `offre` avec ses `Compte`), et la moitié du cœur historique n'a aucun test. C'est ce qu'il faut traiter avant d'ajouter la moindre fonctionnalité.

---

## 2. Ce qui a été fait, chantier par chantier

Cinq chantiers menés, tous commités sur `main` :

### Chantier 1 — Audit initial
Analyse du projet d'origine (543 lignes, 9 classes). Résultat : **9 bugs métier critiques** reproduits par exécution, projet physiquement logé **dans** `.idea/`, et mélange de deux paradigmes incompatibles (WAR Jakarta EE + application Swing avec `main()`).

### Chantier 2 — Assainissement
- Sortie du projet de `.idea/`, suppression du squelette Jakarta EE mort (`HelloResource`, `HelloApplication`, `beans.xml`, `k.ts`).
- Renommage aux conventions Java (`client` → `Client`, `transaction` → `Transaction`, `historique` → `HistoriqueWindow`).
- **Les 9 bugs corrigés**, dont les trois majeurs : le dépôt écrasait le solde (`=+` au lieu de `+=`), tout virement sortant était débité du **Livret A**, et aucun montant n'était validé.
- `float` → **`BigDecimal`** partout, échelle 2, `HALF_EVEN`, centralisé dans `Montants`.
- Sécurité de base : `JPasswordField`, et `getMdp()` remplacé par `Client.verifierMotDePasse(saisie)` — le mot de passe ne sort plus de l'objet.

### Chantier 3 — Architecture en couches
Découpage en `bank-core` (Java pur, zéro dépendance UI) et `bank-swing`. Création des couches `service` / `repository` / `exception`, plus 3 controllers côté IHM.

La règle structurante, vérifiée par analyse des imports : **`bank-core` ne connaît pas `bank-swing`**, et les fenêtres ne parlent qu'à leur controller, jamais au service ni au repository.

```
ui → controller → service → repository → model
```

Corrigé au passage : l'affichage figé après un virement (`ClientInfoWindow` garde une référence au `Client` au lieu d'une copie de ses champs), et le démarrage de Swing sur l'EDT.

### Chantier 4 — Patrons de conception : le package `offre`
20 fichiers. **Abstract Factory** (3 tiers × 4 produits) + **Strategy** pour les taux du Livret A.

```
model/offre/
├── OffreFactory              → concret/ OffreEtudianteFactory, OffreStandardFactory, OffrePremiumFactory
├── compte/ Compte            → concret/ CompteEtudiant, CompteStandard, ComptePremium
├── carte/  CarteBancaire     → concret/ CarteJeune, CarteClassique, CarteBlack
├── pret/   Pret              → concret/ PretEtudiant, PretPersonnel, PretImmobilier
└── taux/   TauxInteretStrategy → concret/ TauxLivretAEtudiant/Standard/Premium
```

L'intérêt : les combinaisons incohérentes deviennent **impossibles à construire** (pas de `CarteBlack` avec un `CompteEtudiant`, aucune fabrique ne les produit ensemble).

Les règles partagées sont des **méthodes `static` sur les interfaces**, pas recopiées : `Compte.soldeApresCredit/Debit` réutilise les règles de `Client`, et `Pret.mensualite` écrit la formule d'amortissement une seule fois.

### Chantier 5 — Tests et CI
- **97 tests JUnit 5** sur `offre`. Les mensualités attendues ont été calculées **indépendamment du code testé** (Python `decimal`, précision 50) avant d'être écrites dans les tests.
- Suite validée par **test de mutation** : passer le découvert de `CompteStandard` de 300 à 0 € fait tomber 3 tests.
- **`maven-surefire-plugin` épinglé en 3.2.5** dans le pom parent. Sans cela, Maven 3.6 utilise surefire 2.12.4, qui ne connaît que JUnit 4 et **ignorerait les 97 tests en silence** avec un `BUILD SUCCESS`. Piège à ne jamais désamorcer.
- Workflow GitHub Actions : `mvn -B clean compile` puis `mvn -B test` depuis la racine, avec publication du rapport JUnit dans les Checks et en commentaire de PR.

---

## 3. Où en est le code, concrètement

### Ce qui est solide

- **Exactitude métier** — `BigDecimal` partout, validation systématique, aucun solde négatif hors découvert autorisé.
- **Étanchéité des couches** — vérifiée mécaniquement ; le cœur est réutilisable tel quel par un futur frontal REST.
- **Traçabilité** — toute opération produit une `Transaction` immuable horodatée ; un virement en produit **deux**.
- **Erreurs** — exceptions métier typées sous `BanqueException`, remontées jusqu'à l'utilisateur en `JOptionPane`. Plus aucun `System.out.println` porteur d'information métier.
- **Filet de sécurité** — CI verte à chaque push.

### Les trois angles morts

| | Problème | Conséquence pratique |
|---|---|---|
| 🔴 | **Aucune persistance** ni création de compte | L'application ne peut pas servir en vrai. Deux clients de démo codés dans `Main`. |
| 🔴 | **Mots de passe en clair** (`mdp.equals(saisie)`), aucune limite de tentatives | Force brute possible sur la fenêtre de connexion. |
| 🟠 | **`Client` et `offre.Compte` ne se parlent pas** | `Client` porte `soldeCompte` / `soldeLivretA` en dur ; le package `offre` n'est importé **nulle part** hors de ses tests. Le Livret A est un `boolean` + un `BigDecimal`, donc `TauxInteretStrategy` n'a nulle part où être injecté et aucun intérêt n'est calculé. |

### Dette secondaire (vérifiée le 31/08/2026, toujours présente)

| # | Point | Où |
|---|---|---|
| 1 | **Le cœur historique n'a aucun test** — `Client`, `BanqueService`, `AuthService`, repositories, controllers | les 97 tests portent tous sur `offre` |
| 2 | Champs de connexion quasi invisibles : `setPreferredSize(new Dimension(1, 1))` | [GestionnaireInterfaceGraphique.java:95-96](bank-swing/src/main/java/com/example/bank/swing/ui/GestionnaireInterfaceGraphique.java#L95-L96) |
| 3 | `CompteController.deposer` / `retirer` fonctionnent mais **ne sont câblés à aucun bouton** | [CompteController.java:23-29](bank-swing/src/main/java/com/example/bank/swing/controller/CompteController.java#L23-L29) |
| 4 | Pas de déconnexion ; la fenêtre de login reste ouverte après connexion | `swing/ui/` |
| 5 | Aucun écran pour ouvrir un Livret A, alors que `creerLivretA` existe | `swing/ui/` |
| 6 | Pas de mesure de couverture (JaCoCo non configuré) | `pom.xml` |
| 7 | **9 fichiers `.idea/` toujours versionnés** | racine du dépôt |
| 8 | Les valeurs des offres sont marquées `VALEURS D'EXEMPLE, à ajuster` | `model/offre/**/concret/` |

> Note : le README liste encore « rien n'est commité » en dette n°14 — c'est **périmé**, les 5 chantiers sont sur `main` depuis les commits du 17 août.

---

## 4. Par quoi reprendre

L'ordre compte : chaque phase est le prérequis de la suivante.

### ➡️ Phase A — Tester le cœur historique *(à faire en premier)*

Porter sur `Client`, `BanqueService` et `AuthService` la méthode déjà appliquée à `offre`.

Sans tests sur `virer`, `deposer`, `retirer` et `creerLivretA`, la refonte du modèle en phase B se ferait **à l'aveugle** — c'est la seule raison pour laquelle cette phase passe avant.

À couvrir en priorité : le virement (deux clients touchés, deux transactions produites, virement vers soi-même refusé, solde intact après échec), le découvert, et le message d'erreur d'authentification identique pour un nom inconnu et un mot de passe faux.

### Phase B — Faire converger `Client` et `offre.Compte`

Remplacer `soldeCompte` / `livretAExiste` / `soldeLivretA` par une `List<Compte>` dans `Client`, et introduire **`LivretA` comme implémentation de `Compte`** recevant sa `TauxInteretStrategy` par injection.

C'est ce qui donne enfin un sens au package `offre`, et ce qui permettra d'ajouter d'autres produits (LDD, PEL, compte joint) sans toucher au reste.

### Phase C — Persistance

`JdbcClientRepository` sur SQLite ou H2, derrière l'interface `ClientRepository` déjà en place. Deux points non négociables : **`PreparedStatement` uniquement**, et un virement doit être **une transaction SQL atomique** (débit et crédit valident ou échouent ensemble).

### Phase D — Sécurité

BCrypt sur les mots de passe, verrouillage après 3 tentatives, journal d'audit des connexions et opérations sensibles.

### Phase E — Finir l'IHM

Écrans de dépôt, retrait, inscription, ouverture de Livret A, souscription d'offre. Déconnexion et cycle de vie des fenêtres. Et le correctif d'une ligne sur les champs de connexion (dette n°2).

### Phase F — Fonctionnalités bancaires

Versement des intérêts du Livret A (plafond réglementaire 22 950 €), virements programmés, agios sur découvert, export de relevé CSV puis PDF.

### Phase G — Web *(optionnel)*

Module `bank-web` en JAX-RS consommant le même `bank-core`, authentification JWT. L'architecture actuelle rend cette étape possible **sans toucher au cœur** — c'est précisément ce que le chantier 3 a acheté.

---

## 5. Mémo pratique

```bash
mvn clean package                      # compile les deux modules
java -jar bank-swing/target/bank-swing-1.0-SNAPSHOT.jar
mvn test                               # 97 tests — TOUJOURS depuis la racine, jamais depuis un module
```

**Comptes de démo** (chargés en mémoire par `Main`) : `Mouh` / `tata` (RIB 123) et `amine` / `matoub` (RIB 456).
Les champs de saisie sont presque invisibles à l'écran — voir dette n°2, ce n'est pas un bug d'affichage de votre machine.

**Conventions à respecter** (détail en [README §11](README.md)) :

- Montants : `BigDecimal` uniquement, jamais `float`/`double`. Passer par `Montants`.
- Taux : fraction décimale (`0.0320` = 3,20 %). Découvert : montant **positif**.
- Erreurs métier : lever une sous-classe de `BanqueException`, jamais écrire sur la console.
- Implémentations concrètes dans un sous-package `concret/`.
- Tests : `@DisplayName` en français, `BigDecimal` comparé avec `compareTo` et jamais `equals` (qui compare aussi l'échelle : `"60"` ≠ `"60.00"`).

---

*Point de reprise. À réviser à la fin de chaque phase.*
