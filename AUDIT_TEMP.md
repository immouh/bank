# Audit d'architecture — projet `bank`

**Fichier temporaire, non destiné à être versionné.** À supprimer une fois le nettoyage décidé.

**Date :** 2 septembre 2026 · **Périmètre :** `bank-core` + `bank-swing`, phases A→D terminées, phase E non commencée
**Méthode :** lecture seule. Aucun fichier de `src/main` ni de `src/test` n'a été modifié.

| | Fichiers | Lignes |
|---|---:|---:|
| `bank-core/src/main` | 83 | 4 076 |
| `bank-swing/src/main` | 8 | 654 |
| `bank-core/src/test` | 29 | 5 167 |
| `bank-swing/src/test` | **0** | **0** |
| **Total** | **120** | **9 897** |

`mvn -B clean test` : **421 tests, 0 échec.**

---

## 1. Arborescence commentée

### `bank-core/src/main`

```
exception/                                    la hiérarchie d'erreurs métier
   15  BanqueException                        racine abstraite, ce que l'IHM attrape
   13  ClientIntrouvableException             RIB/nom inconnu, et échec d'authentification
   20  CompteVerouilleException               trop de tentatives de connexion
    9  LivretAAbsentException                 opération sur un Livret A jamais ouvert
   18  LivretADejaExistantException           seconde ouverture de Livret A
    9  MontantInvalideException               montant nul, négatif ou absent
   29  OperationInterditeException            l'état courant refuse l'opération/la transition
   27  PersistanceException                   la base n'a pas répondu
   17  PlafondDepasseException                paiement au-dessus du plafond de la carte
   12  SoldeInsuffisantException              débit au-delà du disponible
   14  VirementVersSoiMemeException           virement dont émetteur = destinataire

model/
  196  Client                                 le client, ses comptes, son historique, son offre
   43  Montants                               échelle 2 / HALF_EVEN / refus des montants ≤ 0
   56  Transaction                            ligne d'historique immuable et horodatée

model/offre/
   40  OffreFactory                           Abstract Factory : compte + carte + prêt + taux
   41  concret/OffreEtudianteFactory          famille de produits du tier Étudiante
   41  concret/OffreStandardFactory           famille de produits du tier Standard
   41  concret/OffrePremiumFactory            famille de produits du tier Premium

model/offre/compte/
  100  Compte                                 interface + règles de crédit/débit partagées
   75  CompteBase                             socle : détient solde et état, délègue
   27  concret/CompteEtudiant                 découvert 0 €, sans frais
   25  concret/CompteStandard                 découvert 300 €, frais 2 €
   26  concret/ComptePremium                  découvert 2 000 €, frais 12 €
   58  concret/LivretA                        épargne rémunérée, Strategy de taux injectée
   83  etat/EtatCompte                        State : refus par défaut, chaque état opte
   58  etat/concret/CompteActif               solde ≥ 0, tout est ouvert
   61  etat/concret/CompteEnDecouvert         solde < 0 dans la limite du tier
   50  etat/concret/CompteBloque               opposition ; seule la régularisation passe
   19  etat/concret/CompteFerme                terminal, tout est refusé

model/offre/carte/
   40  CarteBancaire                          interface : plafond, état, autorisation
   50  CarteBase                              socle : détient l'état, délègue
   26  concret/CarteJeune                     plafond 500 €, cotisation offerte
   25  concret/CarteClassique                 plafond 1 500 €, cotisation 45 €
   32  concret/CarteBlack                     plafond 10 000 €, assurance voyage
   49  etat/EtatCarte                         State : active / bloquée / expirée
   50  etat/concret/CarteActive               paie dans la limite du plafond
   30  etat/concret/CarteBloquee              opposition réversible
   19  etat/concret/CarteExpiree              terminal, pas de retour

model/offre/pret/
   94  Pret                                   interface + formule d'amortissement partagée
   60  PretBase                               socle : montant emprunté et état
   44  concret/PretEtudiant                   0,90 % sur 60 mois
   44  concret/PretPersonnel                  4,50 % sur 48 mois
   44  concret/PretImmobilier                 3,20 % sur 240 mois
   54  etat/EtatPret                          State : attente → approuvé → remboursement → soldé/défaut
   22  etat/concret/PretEnAttente             demande déposée, rien d'exigible
   31  etat/concret/PretApprouve              accordé, mensualité exigible
   34  etat/concret/PretEnRemboursement       échéancier en cours
   18  etat/concret/PretSolde                 terminal, remboursé
   23  etat/concret/PretEnDefaut              terminal, impayé déclaré

model/offre/taux/
   19  TauxInteretStrategy                    Strategy : taux annuel de l'épargne
   21  concret/TauxLivretAEtudiant            2,00 %
   21  concret/TauxLivretAStandard            3,00 %
   21  concret/TauxLivretAPremium             4,50 %

repository/
   44  ClientRepository                       interface de stockage + sauvegarde groupée
   58  InMemoryClientRepository               Map indexée par RIB, conservée pour les tests
  449  JdbcClientRepository                   ⚠ le plus gros fichier du projet — CRUD JDBC + reconstruction
   75  BaseDeDonneesH2                        ouverture de connexion et création du schéma
   27  LigneHistorique                        record de lecture, porte le vrai horodatage

service/
  124  BanqueService                          dépôt, retrait, virement, Livret A
  129  AuthService                            monte la chaîne d'authentification, en tire les conséquences

service/commande/
   42  Commande                               Command : exécuter, libellé, RIB, événement d'audit
   50  DeposerCommande                        dépôt réifié
   46  RetirerCommande                        retrait réifié
   55  VirerCommande                          virement réifié
   73  InvocateurCommande                     exécute, journalise, trace à l'audit

service/securite/
   43  HachageStrategy                        Strategy : hacher / vérifier / haché factice
   84  BCryptHachageStrategy                  BCrypt coût 12, haché factice constant
   49  EtapeAuthentification                  Chain of Responsibility : maillon abstrait
   43  VerificationClientExiste               étape 1 + défense anti-chronomètre
   30  VerificationVerrouillage               étape 2, avant le mot de passe volontairement
   48  VerificationMotDePasse                 étape 3, contient le pont de migration
   23  RepertoireMotsDePasse                  interface : où sont les hachés
   21  RepertoireMotsDePasseEnMemoire         Map, pour les tests
   55  JdbcRepertoireMotsDePasse              lit/écrit clients.mot_de_passe
   44  RegistreTentatives                     interface : compteur d'échecs et verrouillage
   71  RegistreTentativesEnMemoire            Map + horloge injectable
  106  JdbcRegistreTentatives                 table tentatives_connexion
   33  Verrouillage                           arithmétique du délai, partagée par les deux registres

service/audit/
   13  EvenementAudit                         7 natures d'événement
   25  JournalAudit                           interface, purement descriptive
   10  LigneAudit                             record d'une ligne consignée
   40  JournalAuditEnMemoire                  liste, pour les tests
   72  JdbcJournalAudit                       table journal_audit, en ajout seul

resources/
       schema.sql                             5 tables, commenté ligne à ligne
```

### `bank-swing/src/main`

```
  133  Main                                   composition root : base, sécurité, controllers, EDT
controller/
   46  CompteController                       dépôt/retrait via commandes + rafraîchissement
   27  LoginController                        passe-plat vers AuthService
   52  VirementController                     résout les RIB puis délègue au service
ui/
  128  GestionnaireInterfaceGraphique         fenêtre de connexion
  104  ClientInfoWindow                       tableau de bord, lit les Compte du client
  122  VirementWindow                         formulaire de virement
   42  HistoriqueWindow                       liste des transactions
```

### `bank-core/src/test` (29 classes, 5 167 lignes)

Miroir du `main`, sauf `repository/InMemoryClientRepository`, `BaseDeDonneesH2`, `Montants`, `Transaction`, `OffreFactory` et tout `bank-swing`, qui n'ont pas de classe de test dédiée.

---

## 2. État des lieux par package

### `exception/` — 11 fichiers, 183 lignes
Dépendances sortantes : **aucune** (seulement `java.math`). C'est le socle, il ne dépend de rien.
Tout descend de `BanqueException`, sauf deux `IllegalArgumentException` levées ailleurs (voir §6).

### `model/` — 3 fichiers, 295 lignes
Importe `exception/`, `model/offre/*`. **N'importe ni `service/` ni `repository/`** — vérifié, aucune occurrence.
`Client` (196 l.) est le plus gros du package : 6 accesseurs de lecture, 5 opérations, 2 constructeurs.
Dette signalée dans le code : aucune (les `TODO Phase A` vivent dans les tests, pas dans le modèle).

### `model/offre/compte/` — 11 fichiers, 582 lignes
Importe `exception/`, `model/Montants`, `model/offre/taux/` (pour `LivretA`).
Le plus gros : `Compte` (100 l.), dont ~60 % de javadoc et deux méthodes statiques partagées.
**Duplication repérée** : `CompteActif` et `CompteEnDecouvert` ont des corps `crediter`/`debiter`/`crediterRegularisation` **strictement identiques** (diff vide, voir §6).

### `model/offre/carte/` — 9 fichiers, 321 lignes
Importe `exception/`, `model/Montants`. Aucun fichier au-dessus de 50 lignes. Rien à signaler.

### `model/offre/pret/` — 10 fichiers, 438 lignes
Importe `exception/`, `model/Montants`. `Pret` (94 l.) porte la formule d'amortissement, écrite une fois.

### `model/offre/taux/` — 4 fichiers, 82 lignes
Aucune dépendance hors `java.math`. Le plus petit package du projet, et le seul à n'avoir qu'une méthode par classe.

### `model/offre/observation/` — **n'existe pas.** Phase E non commencée, comme annoncé.

### `service/` — 2 fichiers, 253 lignes
Importe `exception/`, `model/`, `repository/`, `service/audit/`, `service/securite/`.
`AuthService` (129 l.) est passé devant `BanqueService` (124 l.) depuis la phase D.

### `service/commande/` — 5 fichiers, 266 lignes
Importe `model/`, `service/BanqueService`, `service/audit/`.
**Duplication repérée** : `DeposerCommande` et `RetirerCommande` ne diffèrent que par 6 lignes sur 50 (voir §6).

### `service/securite/` — 13 fichiers, 650 lignes
Importe `exception/`, `model/Client`, `java.sql` (pour les deux implémentations JDBC).
Le seul package du projet avec une dépendance externe de compilation : `org.mindrot.jbcrypt`, confinée à `BCryptHachageStrategy`.
**Dette signalée dans le code** : `VerificationMotDePasse:36-45` — « PONT DE MIGRATION … À SUPPRIMER quand tous les magasins seront hachés ».
**Import inutilisé** : `java.time.Duration` dans `RegistreTentativesEnMemoire` (voir §6).

### `service/audit/` — 5 fichiers, 160 lignes
Importe `exception/PersistanceException`, `java.sql`. Ne dépend d'aucun autre service — voulu, un journal ne décide de rien.

### `repository/` — 5 fichiers, 653 lignes
Importe `exception/`, `model/`, `model/offre/*`, `java.sql`. **N'importe pas `service/`** — vérifié.
`JdbcClientRepository` (449 l.) est **le plus gros fichier du projet**, environ 3,5 fois la moyenne. Il fait quatre choses : CRUD client, écriture des comptes, écriture/lecture de l'historique, et reconstruction d'un `Client` complet. C'est le premier candidat à un découpage.

### `bank-swing/controller/` — 3 fichiers, 125 lignes
- `LoginController` → `service/AuthService` uniquement.
- `CompteController` → `service/BanqueService` + `service/commande/`.
- `VirementController` → `service/BanqueService` + **`repository/ClientRepository`** + `service/commande/`.

Le troisième court-circuite le service pour résoudre un RIB en `Client`. **C'est conforme au schéma d'origine** (le diagramme du README fait bien partir une flèche `CTRL → REPO`), mais c'est le seul endroit où un controller parle au stockage ; à surveiller.

### `bank-swing/ui/` — 4 fichiers, 396 lignes
Importent **uniquement** `core.model` et `core.exception`. Aucune fenêtre n'importe `service/` ni `repository/` — **l'étanchéité tient**.
`GestionnaireInterfaceGraphique` (128 l.) est la plus grosse fenêtre, et la seule à contenir de la dette visible (voir §6).

### Étanchéité globale — verdict

| Règle | Vérification | Verdict |
|---|---|---|
| `bank-core` ignore Swing/AWT | grep `javax.swing`, `java.awt`, `com.example.bank.swing` dans tout `bank-core` | **0 occurrence** |
| `model/` ignore service et repository | grep imports | **0 occurrence** |
| `repository/` ignore service | grep imports | **0 occurrence** |
| `ui/` ne parle qu'aux controllers et au modèle | grep imports | **respecté** |
| `controller/` → service | respecté, sauf `VirementController` → repository (conforme au schéma d'origine) | à surveiller |

---

## 3. Dettes connues — statut vérifié

### TODO #3 — ordre de validation dans `crediterLivretA` : **TOUJOURS PRÉSENT**
`model/Client.java:192-195`
```java
public void crediterLivretA(BigDecimal montant) {
    BigDecimal m = Montants.exigerPositif(montant);          // ligne 193 : montant d'abord
    getLivretA().orElseThrow(LivretAAbsentException::new)     // ligne 194 : existence ensuite
        .crediter(m);
}
```
Un montant négatif sur un client sans Livret A remonte `MontantInvalideException`, jamais `LivretAAbsentException`. Verrouillé par `ClientTest:378` (test `ordreDeValidationDuCreditLivretA`).

### TODO #4 — ordre de validation dans `virer` : **TOUJOURS PRÉSENT**
`service/BanqueService.java:86-94` — l'ordre est : émetteur non nul (87), destinataire non nul (88-90), **vers soi-même (91-93)**, **puis montant (94)**.
Un virement vers soi-même de −100 € remonte `VirementVersSoiMemeException` et tait le montant invalide. Verrouillé par `BanqueServiceTest:313`.

### TODO #5 — le modèle nu ne trace rien : **TOUJOURS PRÉSENT**
`model/Client.java:168-178` — `crediter` et `debiter` délèguent au `Compte` et ne produisent **aucune** `Transaction`. La traçabilité repose entièrement sur `BanqueService`. Verrouillé par `ClientTest:435`.
**Aggravation depuis la phase D** : l'audit est posé sur `InvocateurCommande`, pas sur le modèle — un appelant qui passerait par `BanqueService` sans commande produirait une `Transaction` mais **aucune ligne d'audit**.

### Accesseurs façade sur `Client` : **TOUJOURS PRÉSENTS, et très utilisés**
Définis en `model/Client.java:130` (`getSoldeCompte`), `:134` (`isLivretAExiste`), `:139` (`getSoldeLivretA`).

| Où | Occurrences |
|---|---:|
| Production (`bank-core/src/main`, `bank-swing/src/main`) hors `Client.java` | **0** |
| `Client.java` lui-même (ligne 185, `ouvrirLivretA`) | 1 |
| `service/BanqueServiceTest` | 29 |
| `model/ClientTest` | 20 |
| `repository/JdbcClientRepositoryTest` | 7 |
| `service/commande/CommandeTest` | 8 |
| `service/commande/InvocateurCommandeTest` | 2 |
| **Total tests** | **66** |

**Constat important** : plus aucun code de production ne les appelle — `ClientInfoWindow` est passée à `getCompteCourant()` / `getLivretA()` en phase B. Ils ne survivent plus que pour 66 assertions de test. Les supprimer est donc un chantier purement de test, mécanique mais large.

### Dette #7 — `.idea/` versionnés : **TOUJOURS PRÉSENT**
9 fichiers suivis à la racine :
`.idea/.gitignore`, `.idea/bank.iml`, `.idea/compiler.xml`, `.idea/encodings.xml`, `.idea/jarRepositories.xml`, `.idea/misc.xml`, `.idea/modules.xml`, `.idea/remote-targets.xml`, `.idea/vcs.xml`

**Incohérence** : `.gitignore:7-10` ignore `modules.xml`, `jarRepositories.xml`, `compiler.xml` et `libraries/` — mais les trois premiers sont **déjà suivis**, donc le `.gitignore` ne les affecte pas. Il faut un `git rm --cached` pour que la règle prenne effet.

### Dette #8 — « VALEURS D'EXEMPLE » : **TOUJOURS PRÉSENT, 15 marqueurs sur 11 fichiers**

| Fichier | Lignes |
|---|---|
| `model/offre/compte/concret/CompteEtudiant.java` | 10 |
| `model/offre/compte/concret/CompteStandard.java` | 10 |
| `model/offre/compte/concret/ComptePremium.java` | 11 |
| `model/offre/carte/concret/CarteJeune.java` | 10 |
| `model/offre/carte/concret/CarteClassique.java` | 10 |
| `model/offre/carte/concret/CarteBlack.java` | 11 |
| `model/offre/pret/concret/PretEtudiant.java` | 11, 19 |
| `model/offre/pret/concret/PretPersonnel.java` | 11, 19 |
| `model/offre/pret/concret/PretImmobilier.java` | 11, 19 |
| `model/offre/taux/concret/TauxLivretAEtudiant.java` | 10 |
| `model/offre/taux/concret/TauxLivretAStandard.java` | 10 |
| `model/offre/taux/concret/TauxLivretAPremium.java` | 10 |

`LivretA` n'en porte pas : il n'a aucune valeur commerciale propre, tout vient de la stratégie injectée.

### Historique non rechargé au redémarrage : **TOUJOURS VRAI**
- `repository/JdbcClientRepository.java` — `ajouterTransaction` n'y apparaît **0 fois** : la reconstruction d'un `Client` ne rejoue jamais son historique.
- La cause est inchangée : `model/Transaction.java:21` n'a qu'un constructeur à 3 arguments, la date est fixée à `LocalDateTime.now()` en interne.
- `enregistrerHistorique` n'est appelé qu'en **deux endroits**, tous deux dans l'amorce : `bank-swing/Main.java:119-120`. Aucune opération faite pendant l'exécution de l'application n'est écrite dans la table `transactions`.

**Conséquence concrète** : la table `transactions` ne contient jamais que les 5 lignes de démonstration du premier lancement, et le bouton « Afficher historique » ne montre que la session en cours.

### Dettes d'origine restantes côté IHM (non listées dans le prompt, mais toujours là)
- **#9 champs de connexion invisibles** : `ui/GestionnaireInterfaceGraphique.java:95-96`, toujours `new Dimension(1, 1)`.
- **#10 pas de déconnexion** : aucun `dispose()` ni `setVisible(false)` dans la fenêtre de connexion — elle reste ouverte après authentification.
- **#11 dépôt/retrait non câblés** : `compteController.deposer/retirer` ne sont appelés **par aucune fenêtre**.
- **#12 pas de couverture mesurée** : `jacoco` absent des deux `pom.xml`.

---

## 4. Cohérence documentaire

### `ETAT_PROJET.md` — **largement obsolète**, écrit avant la phase A

| Ligne | Contenu | Réalité |
|---|---|---|
| 3 | « Dernière mise à jour : 31 août 2026 … **97 tests** » | 421 tests |
| 71, 73, 100, 158 | « 97 tests », « les 97 tests portent tous sur `offre` » | faux depuis la phase A |
| 93 | « 🔴 Mots de passe en clair (`mdp.equals(saisie)`) » | **traité en phase D** (BCrypt) |
| 100 | « Le cœur historique n'a aucun test » | **traité en phase A** (126 tests) |
| 117 | « ➡️ Phase A — *à faire en premier* » | **faite** |
| 125, 131, 135 | Phases B, C, D décrites au futur | **faites** |

En pratique, seules les sections 1 (chiffres à refaire), 2 (historique des chantiers 1→5) et 5 (conventions) restent exactes. Les sections 3 et 4 décrivent un projet d'il y a quatre phases.

**Coquille « AC » : CORRIGÉE.** La ligne 11 est aujourd'hui `## 1. En une page`, le tableau qui suit est propre.

### `README.md` — **obsolète sur les mêmes points, plus quelques-uns qui lui sont propres**

| Ligne | Contenu | Réalité |
|---|---|---|
| 3 | « État au 17 août 2026 » | 16 jours et 4 phases de retard |
| 74, 227, 265, 280, 373 | « 97 tests » | 421 |
| 95, 155 | « `core.model.offre` … **pas encore branché** » | **branché depuis la phase B** — `Client` construit ses comptes via `OffreFactory` |
| 306 | « #1 Aucune persistance » | **H2 depuis la phase C** |
| 307 | « #2 Mots de passe en clair » | **BCrypt depuis la phase D** |
| 317 | « #7 Le cœur historique n'est pas testé » | **traité en phase A** |
| 329 | « #14 Rien n'est commité » | faux : tout est commité jusqu'à la phase B incluse |

Le diagramme mermaid (ligne ~95) affiche encore `OFFRE` en pointillés avec la mention « pas encore branché » : c'est **l'erreur documentaire la plus trompeuse** du projet, puisqu'elle décrit exactement l'inverse de la situation actuelle.

---

## 5. Tests par classe (421 au total)

| Tests | Classe | Domaine |
|---:|---|---|
| 59 | `ClientTest` | modèle |
| 50 | `BanqueServiceTest` | service |
| 30 | `JdbcClientRepositoryTest` | persistance |
| 28 | `EtatCompteTest` | State compte |
| 21 | `EtatPretTest` | State prêt |
| 18 | `EtatCarteTest` | State carte |
| 17 | `VerrouillageTest` | sécurité |
| 17 | `ChaineAuthentificationTest` | sécurité |
| 17 | `AuthServiceTest` | service (phase A, intouché) |
| 16 | `LivretATest` | modèle offre |
| 15 | `CommandeTest` | Command |
| 13 | `JournalAuditTest` | audit |
| 13 | `BCryptHachageStrategyTest` | sécurité |
| 12 | `CompteStandardTest` | modèle offre |
| 12 | `ComptePremiumTest` | modèle offre |
| 11 | `CompteEtudiantTest` | modèle offre |
| 10 | `PretPersonnelTest` | modèle offre |
| 10 | `PretImmobilierTest` | modèle offre |
| 10 | `PretEtudiantTest` | modèle offre |
| 10 | `InvocateurCommandeTest` | Command |
| 5 | `OffreStandardFactoryTest` | Abstract Factory |
| 5 | `OffrePremiumFactoryTest` | Abstract Factory |
| 5 | `OffreEtudianteFactoryTest` | Abstract Factory |
| 5 | `CarteBlackTest` | modèle offre |
| 4 | `CarteJeuneTest` | modèle offre |
| 4 | `CarteClassiqueTest` | modèle offre |
| 2 | `TauxLivretAEtudiantTest` | Strategy taux |
| 1 | `TauxLivretAStandardTest` | Strategy taux |
| 1 | `TauxLivretAPremiumTest` | Strategy taux |

### Trous de couverture — à connaître avant la phase E

| Zone non testée | Poids | Commentaire |
|---|---:|---|
| **`bank-swing` en entier** | 654 l., 8 fichiers | **0 test.** Ni controllers, ni fenêtres, ni `Main`. C'est précisément ce que la phase E va modifier. |
| `repository/InMemoryClientRepository` | 58 l. | testé seulement indirectement, à travers les services |
| `repository/BaseDeDonneesH2` | 75 l. | testé indirectement (`creerSchema` appelé par tous les tests JDBC) |
| `model/Montants` | 43 l. | testé indirectement partout, aucune classe dédiée |
| `model/Transaction` | 56 l. | idem |
| `service/securite/JdbcRegistreTentatives` | 106 l. | **la version en mémoire est testée, pas la version JDBC** |
| `service/securite/JdbcRepertoireMotsDePasse` | 55 l. | idem, non testé directement |

Les deux dernières lignes sont les plus gênantes : la logique de verrouillage **persistée** — celle qui tourne réellement dans l'application — n'a pas de test à elle. `Verrouillage` factorise l'arithmétique, ce qui limite le risque, mais le SQL (`MERGE`, expiration, suppression) n'est couvert par rien.

---

## 6. Duplications et incohérences repérées

### 6.1 Duplication — `CompteActif` et `CompteEnDecouvert` (confirmée par `diff`)
`model/offre/compte/etat/concret/CompteActif.java` et `CompteEnDecouvert.java` ont des corps **strictement identiques** pour `crediter`, `debiter` et `crediterRegularisation` — un `diff` de ces trois méthodes rend un résultat vide. Les deux états ne diffèrent en réalité que par `libelle()` et `apresVariationDeSolde()`.
C'est défendable (chaque état reste lisible seul), mais ~25 lignes sont recopiées, et une correction de règle devrait être faite deux fois.

### 6.2 Duplication — `DeposerCommande` et `RetirerCommande`
Sur 50 lignes, les deux fichiers ne diffèrent que par : le nom de classe, le nom du constructeur, l'appel au service (`deposer`/`retirer`), l'événement d'audit et un libellé. Soit **6 lignes utiles sur 50**.
La méthode privée `nomClient()` est écrite à l'identique dans les deux (`DeposerCommande:47`, `RetirerCommande:43`), et une troisième variante `nom(Client)` existe dans `VirerCommande:52`.

### 6.3 Convention — `System.out.println` métier résiduel ⚠
`bank-swing/src/main/java/com/example/bank/swing/ui/GestionnaireInterfaceGraphique.java:109-110`
```java
System.out.println("CONNEXION REUSSIE");
System.out.println("LA PERSONNE  : " + client.getNom());
```
C'est **le dernier vestige** du « le métier parle à la console » relevé dans l'audit d'origine, et il n'avait pas été repéré jusqu'ici. Deux problèmes : la convention du projet l'interdit, et la seconde ligne **écrit le nom d'un client authentifié sur la sortie standard** — au moment précis où la phase D vient de mettre en place un journal d'audit fait pour ça.

### 6.4 Convention — exceptions hors hiérarchie `BanqueException`
Deux `IllegalArgumentException` en production :
- `model/offre/pret/Pret.java:76` — durée de prêt ≤ 0
- `service/securite/BCryptHachageStrategy.java:56` — mot de passe à hacher absent

Plus une `IllegalStateException` en `model/Client.java` (`getCompteCourant`, invariant rompu).
Les trois sont défendables — ce sont des erreurs de programmation, pas des refus métier — mais aucune n'est attrapée par le `catch (BanqueException)` des fenêtres. À trancher explicitement plutôt qu'à laisser tel quel.

Neuf classes utilisent par ailleurs `Objects.requireNonNull`, donc lèvent des `NullPointerException` volontaires, hors hiérarchie elles aussi.

### 6.5 Import inutilisé
`service/securite/RegistreTentativesEnMemoire.java` importe `java.time.Duration` sans jamais s'en servir — résidu du déplacement de l'arithmétique vers `Verrouillage`.

### 6.6 Fichier anormalement gros
`repository/JdbcClientRepository.java` — **449 lignes**, contre une moyenne de 49 pour `bank-core`. Il cumule quatre responsabilités (CRUD client, comptes, historique, reconstruction d'objet). Second plus gros fichier de production : `model/Client.java` à 196 lignes, soit moins de la moitié.

### 6.7 Ce qui est propre — vérifié, rien à signaler
- **Aucun `float` ni `double`** dans tout le code de production ou de test (les 3 occurrences trouvées sont le mot « double » dans des commentaires français).
- **Aucun `equals()` sur `BigDecimal`** : la seule occurrence est un commentaire de `CompteEtudiantTest:19` qui explique pourquoi il ne faut pas le faire.
- **Aucun `printStackTrace`**.
- **Aucune violation d'étanchéité des couches** (voir le tableau du §2).
- `data/` correctement ignoré ; aucune base H2 traînant dans le dépôt.

---

## Trois points si un nettoyage est décidé

1. **`System.out.println` du nom client** (§6.3) — le seul point qui touche à la fois une convention, la sécurité et une fonctionnalité que la phase D vient de livrer. Correctif : deux lignes à supprimer.
2. **Documentation** (§4) — `README.md` affirme que le package `offre` n'est « pas encore branché » alors qu'il l'est depuis la phase B ; c'est ce qu'un nouvel arrivant lira en premier.
3. **`.idea/` versionnés** (§3) — 9 fichiers, dont 3 que le `.gitignore` croit déjà ignorer. Un `git rm --cached` suffit.

Le reste (duplications §6.1 et §6.2, taille de `JdbcClientRepository`, accesseurs façade) sont des choix de structure à arbitrer, pas des défauts à corriger d'urgence.
