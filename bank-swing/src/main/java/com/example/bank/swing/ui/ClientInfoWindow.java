package com.example.bank.swing.ui;

import com.example.bank.core.model.Client;
import com.example.bank.core.model.offre.compte.Compte;
import com.example.bank.swing.controller.CompteController;
import com.example.bank.swing.controller.VirementController;

import javax.swing.*;
import java.awt.*;

/**
 * Tableau de bord du client.
 *
 * La fenêtre ne reçoit plus de copies de champs (nom, rib, solde, épargne,
 * historique) : elle garde une référence au Client et redemande son état au
 * {@link CompteController} après chaque opération. C'est ce qui corrige
 * l'affichage figé après un virement.
 *
 * Les soldes sont lus sur les {@link Compte} du client et non plus sur des champs
 * du Client : la ligne d'épargne apparaît donc dès qu'un Livret A existe,
 * sans que la fenêtre ait à demander « le livret existe-t-il ? ».
 */
public class ClientInfoWindow extends JFrame {

    private final CompteController compteController;
    private final VirementController virementController;

    private Client client;

    private final JLabel nomLabel = new JLabel();
    private final JLabel ribLabel = new JLabel();
    private final JLabel soldeLabel = new JLabel();
    private final JLabel epargneLabel = new JLabel();
    private final JLabel totalLabel = new JLabel();

    public ClientInfoWindow(Client client,
                            CompteController compteController,
                            VirementController virementController) {
        this.client = client;
        this.compteController = compteController;
        this.virementController = virementController;

        setTitle("Informations du client");
        setSize(320, 220);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        // 2 colonnes, autant de lignes que nécessaire : la grille ne casse plus
        // selon que le Livret A est affiché ou non.
        panel.setLayout(new GridLayout(0, 2));

        JButton virement = new JButton("Faire un virement ");
        JButton fermerButton = new JButton("Fermer");
        JButton historiqueButton = new JButton("Afficher historique");

        panel.add(nomLabel);
        panel.add(epargneLabel);
        panel.add(ribLabel);
        panel.add(soldeLabel);
        panel.add(fermerButton);
        panel.add(virement);
        panel.add(historiqueButton);
        panel.add(totalLabel);

        add(panel, BorderLayout.CENTER);

        fermerButton.addActionListener(e -> dispose());
        // La fenêtre de virement prévient quand une opération a abouti :
        // on relit alors l'état du client et on réaffiche.
        virement.addActionListener(e ->
                new VirementWindow(client, virementController, this::rafraichir));
        historiqueButton.addActionListener(e -> new HistoriqueWindow(client.getHistorique()));

        afficherEtat();
        setVisible(true);
    }

    /** Relit le client via le controller, puis réaffiche. */
    private void rafraichir() {
        client = compteController.rafraichir(client);
        afficherEtat();
    }

    private void afficherEtat() {
        nomLabel.setText("Nom : " + client.getNom());
        ribLabel.setText("RIB : " + client.getRib());

        Compte compteCourant = client.getCompteCourant();
        soldeLabel.setText("Solde : " + compteCourant.getSolde() + " €"
                + " (" + compteCourant.getEtat().libelle() + ")");

        // La ligne d'épargne n'existe à l'écran que si le compte existe.
        client.getLivretA().ifPresentOrElse(
                livret -> {
                    epargneLabel.setText("Epargne  : " + livret.getSolde() + " €");
                    epargneLabel.setVisible(true);
                },
                () -> epargneLabel.setVisible(false));

        // Le total n'a de sens qu'à partir de deux comptes.
        totalLabel.setText("Total : " + client.soldeTotal() + " €");
        totalLabel.setVisible(client.getComptes().size() > 1);
    }
}
