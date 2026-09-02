-- =====================================================================
--  Schéma de la base bank (H2, mode fichier embarqué)
--
--  ÉCARTS ASSUMÉS PAR RAPPORT AU MODÈLE JAVA
--
--  * rib est un INT — c'est le type du champ dans Client, pas une chaîne.
--
--  * Les transactions sont rattachées au CLIENT, pas à un compte : dans le
--    modèle, l'historique appartient au Client (List<Transaction>), aucune
--    Transaction ne référence le compte mouvementé. Une colonne compte_id
--    serait une information qu'on ne sait pas remplir.
--
--  * numero_ordre conserve la POSITION de la ligne dans l'historique du
--    client. L'horodatage seul ne suffit pas : deux transactions d'un même
--    virement tombent sur la même milliseconde, et l'ordre d'affichage
--    doit rester celui de l'exécution.
--
--  * Ni le découvert autorisé ni le taux du Livret A ne sont stockés : ce
--    sont des constantes du TIER, pas des données du client. Elles se
--    déduisent de la colonne type via l'OffreFactory correspondante.
--    Les dupliquer en base, c'est se garantir qu'un jour la grille
--    tarifaire et la base diront deux choses différentes.
--
--  * Les montants sont en DECIMAL(15,2), jamais FLOAT ni DOUBLE : le
--    projet est construit sur BigDecimal, échelle 2, HALF_EVEN. Le driver
--    H2 rend un BigDecimal pour ce type, la convention traverse donc la
--    persistance sans conversion flottante.
-- =====================================================================

CREATE TABLE IF NOT EXISTS clients (
    rib          INT          PRIMARY KEY,
    nom          VARCHAR(100) NOT NULL,
    -- En clair à ce stade. Le hachage BCrypt est le sujet de la phase D.
    mot_de_passe VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS comptes (
    id         IDENTITY       PRIMARY KEY,
    client_rib INT            NOT NULL REFERENCES clients(rib),
    -- CompteEtudiant | CompteStandard | ComptePremium | LivretA
    type       VARCHAR(30)    NOT NULL,
    solde      DECIMAL(15, 2) NOT NULL,
    -- ACTIF | EN_DECOUVERT | BLOQUE | FERME
    etat       VARCHAR(30)    NOT NULL
);

CREATE TABLE IF NOT EXISTS transactions (
    id            IDENTITY       PRIMARY KEY,
    client_rib    INT            NOT NULL REFERENCES clients(rib),
    -- Position dans l'historique du client, à partir de 0.
    numero_ordre  INT            NOT NULL,
    -- DEPOT | RETRAIT | VIREMENT
    type          VARCHAR(30)    NOT NULL,
    montant       DECIMAL(15, 2) NOT NULL,
    description   VARCHAR(255),
    horodatage    TIMESTAMP      NOT NULL,
    CONSTRAINT uk_transaction_ordre UNIQUE (client_rib, numero_ordre)
);

CREATE INDEX IF NOT EXISTS idx_comptes_client ON comptes(client_rib);
CREATE INDEX IF NOT EXISTS idx_transactions_client ON transactions(client_rib);

-- =====================================================================
--  Phase D — sécurité
--
--  * clients.mot_de_passe contient désormais un HACHÉ BCrypt (~60
--    caractères, VARCHAR(255) reste large). La colonne ne change pas de
--    type : c'est son contenu qui change, et rien ne doit plus jamais y
--    écrire un mot de passe en clair.
--
--  * tentatives_connexion est séparée de clients : le compteur d'échecs est
--    un état de la mécanique d'authentification, pas un attribut bancaire.
--    Une ligne n'existe que pour un client ayant échoué au moins une fois ;
--    une connexion réussie la supprime.
--
--  * journal_audit n'a PAS de clé étrangère vers clients, volontairement :
--    un journal d'audit doit survivre à la disparition de ce qu'il décrit.
--    Effacer un client ne doit pas effacer la trace de ses connexions.
-- =====================================================================

CREATE TABLE IF NOT EXISTS tentatives_connexion (
    client_rib         INT       PRIMARY KEY REFERENCES clients(rib),
    echecs             INT       NOT NULL,
    derniere_tentative TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS journal_audit (
    id         IDENTITY    PRIMARY KEY,
    client_rib INT         NOT NULL,
    -- CONNEXION_REUSSIE | CONNEXION_ECHOUEE | COMPTE_VEROUILLE
    -- | DEPOT | RETRAIT | VIREMENT | OUVERTURE_LIVRET_A
    evenement  VARCHAR(40) NOT NULL,
    horodatage TIMESTAMP   NOT NULL,
    detail     VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_audit_client ON journal_audit(client_rib);
