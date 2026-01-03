package com.example.bank;

import javax.swing.*;
import java.awt.FlowLayout;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class GestionnaireInterfaceGraphique extends JFrame {
    public JButton bouton;//
    public JTextField mdp;//partie du mot de passe
    public JTextField id;// POUR L ID
    private  client[] clients;//LE TABLEAU DE CLIENT POUR LE CHECK
    public GestionnaireInterfaceGraphique(client[] clients) {
        // Initialiser la fenêtre
        setTitle("MA BANQUE");
        setSize(400, 300);// taille de la fenetre
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();// Utilisation d'un JPanel avec GridLayout
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Utilisation d'un BoxLayout avec une orientation verticale

        // Créer un gestionnaire de disposition FlowLayout et l'attribuer à la fenêtre
       // setLayout(new FlowLayout());
        id = new JTextField("Nom d'utilisateur", 20);
        mdp = new JTextField("Mot de passe", 20);
        bouton = new JButton(" Se connecter  ");

        id.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (id.getText().equals("Nom d'utilisateur")) {
                    id.setText("");
                    id.setForeground(Color.BLACK); // Réinitialiser la couleur du texte à noir
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (id.getText().isEmpty()) {
                    id.setText("Nom d'utilisateur");
                    id.setForeground(Color.GRAY); // Changer la couleur du texte en gris
                }
            }
        });
        mdp.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (mdp.getText().equals("Mot de passe")){
                    mdp.setText("");
                    mdp.setForeground(Color.BLACK);
                }

            }

            @Override
            public void focusLost(FocusEvent e) {
                if (mdp.getText().isEmpty()) {
                    mdp.setText("Mot de passe");
                    mdp.setForeground(Color.GRAY); // Changer la couleur du texte en gris
                }
            }
        });


        id.setPreferredSize(new Dimension(1, 1)); // Largeur de 200 pixels, hauteur de 20 pixels
        mdp.setPreferredSize(new Dimension(1, 1)); //
        // Ajouter les composants à la fenêtres
        panel.add(id);
        panel.add(mdp);
        panel.add(bouton);
        panel.add(new JLabel());
        add(panel); // Ajout du JPanel au centre de la fenêtre

        this.clients=clients;
        bouton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String nom = id.getText();
                String password = mdp.getText();
                boolean connexionReussie=false;
                for (client c : clients ){
                    if (c!=null && c.getMdp().equals(password)&& c.getNom().equals(nom)){
                        System.out.println("CONNEXION REUSSIE");
                        System.out.println("LA PERSONNE  : " +  nom+"");
                        connexionReussie=true;
                        new ClientInfoWindow(c,clients,nom, c.getRib(), c.getSoldeCompte(),c.isLivretAExiste(),c.getSoldeLivretA(),c.getHistorique()); // Créer et afficher la fenêtre d'informations du client
                        break;
                }
                if (!connexionReussie){
                    System.out.println("Nom utilisateur ou mdp faux");
                }
            }
        }});




        // Afficher la fenêtre
        setVisible(true);
    }


}
