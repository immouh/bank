package com.example.bank;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Création d'un client
         client[] c = new client[2];
         c[0] = new client("Mouh","tata",123);
        c[1]= new client("amine","matoub",456);
        //System.out.println("Solde du compte : " + client.getSoldeCompte());
        // Création d'un livret A avec un montant initial de 1000
       c[0].creerLivretA(1000);

        // Effectuer quelques transactions
        c[0].effectuerDepot(500);
       // System.out.println("Solde du compte : " + client.getSoldeCompte());
        c[0].effectuerRetrait(200);

        c[0].effectuerVirement(300, false,c[1]); // Virement depuis le livret A vers le compte

        // Affichage du solde du compte et du livret A
        //System.out.println("Solde du compte : " + p.getSoldeCompte());
        //System.out.println("Solde du livret A : " + client.getSoldeLivretA());

        // Affichage de l'historique des transactions
        //List<transaction> historique = client.getHistorique();
        //System.out.println("Historique des transactions :");
       // for (transaction transaction : historique) {
         //   System.out.println(transaction);
        //}
        new GestionnaireInterfaceGraphique(c);
    }
}