package com.example.bank;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class client {
    private final String nom;//NOM DU CLIENT
    private String mdp;//MOT DE PASSE DU CLIENT
    private final int rib;// RIB DU CLIENT GENENRER
    private float soldeCompte;//SOLDE DU COMPTE
    private boolean livretAExiste;//BOOL QUI DIT SI UN LIVRET A EXSITE
    private List<transaction> historique;//TABLEAU POUR RECUPERE L HISTORIQUE
    private float soldeLivretA;//SOLDE DU LIVRET A

    //LES GETTERS
    public String getMdp() {
            return mdp;
    }
    public String getNom() {
        return nom;
    }
    public int getRib() {
        return rib;
    }
    public float getSoldeCompte() {
        return soldeCompte;
    }
    public boolean isLivretAExiste() {
        return livretAExiste;
    }
    public List<transaction> getHistorique() {
        return historique;
    }
    public float getSoldeLivretA() {
        return soldeLivretA;
    }

    //CONSTRUCTEUR DU CLIENT
        Random random = new Random();//GENERATION D UN NOMBRE DE 10 CHIFFRE POUR LE RIB

    public client(String nom,String mdp,int rin) {
        this.nom = nom;
        this.mdp=mdp;
        this.rib = rin;
                //1000000000 + random.nextInt(900000000);
        this.soldeCompte = 0;//INITIALISATION DU SOLDE DU COMPTE A 0
        this.livretAExiste = false;//LIVRET A INNEXSTITANT
        this.soldeLivretA = 0;//
        this.historique = new ArrayList<transaction>();//CREATION DUNE LISTE CHAINE
    }
    //AJOUT DES METHODES
    //ENFILER UNE TRANSACTION DANS LA LISTE CHAINE
    //LE FAIRE DANS CHAQUE OPERATION
    public void ajouterTransaction(transaction t){
        historique.add(t);
    }
    //CREATION D UN LIVRET A AVEC UNE SOMME DE DEPART
    public void creerLivretA(float sommeDepart) {
        if (livretAExiste) {
            System.out.println("Livret A déjà existant");
        } else {
            livretAExiste = true;
            soldeLivretA = sommeDepart;
        }
    }
    // EFFECTURER UN VIREMENT
    public void effectuerVirement(float montant, boolean versLivretA,client dest) {
        if (versLivretA) {
            if (livretAExiste) {
                if (soldeCompte >= montant) {
                    soldeCompte -= montant;
                    soldeLivretA += montant;
                    ajouterTransaction( new transaction(montant,transaction.TypeTransaction.VIREMENT,"virement effectuer avec succes"));
                } else {
                    System.out.println("Solde insuffisant sur le compte");
                }
            } else {
                System.out.println("Livret A inexistant");
            }
        } else {
            if (soldeLivretA >= montant) {
                soldeLivretA -= montant;
                dest.soldeCompte += montant;
                ajouterTransaction( new transaction(montant,transaction.TypeTransaction.VIREMENT,"virement effectuer avec succes"));
            } else {
                System.out.println("Solde insuffisant sur le livret A");
            }
        }
    }

    public void effectuerDepot(float montant) {
        soldeCompte =+ montant;
        ajouterTransaction( new transaction(montant,transaction.TypeTransaction.DEPOT,"depot effectuer avec succes"));
    }
    public float effectuerRetrait(float montant) {
        soldeCompte -= montant;
        ajouterTransaction( new transaction(montant,transaction.TypeTransaction.RETRAIT,"retrait  effectuer avec succes"));
        return montant;

    }



}







