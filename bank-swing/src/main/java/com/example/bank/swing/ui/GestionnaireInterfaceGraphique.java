package com.example.bank.swing.ui;

import com.example.bank.core.exception.BanqueException;
import com.example.bank.core.model.Client;
import com.example.bank.swing.controller.CompteController;
import com.example.bank.swing.controller.LoginController;
import com.example.bank.swing.controller.VirementController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Fenêtre de connexion. Elle ne connaît que ses controllers : aucune règle
 * d'authentification n'est écrite ici, elle affiche seulement le résultat.
 */
public class GestionnaireInterfaceGraphique extends JFrame {

    private static final String PLACEHOLDER_ID = "Nom d'utilisateur";
    private static final String PLACEHOLDER_MDP = "Mot de passe";

    private final LoginController loginController;
    private final CompteController compteController;
    private final VirementController virementController;

    public JButton bouton;
    public JPasswordField mdp;   // champ masqué : le mot de passe ne s'affiche plus en clair
    public JTextField id;        // POUR L ID

    /** Caractère de masquage d'origine, restauré dès que l'utilisateur saisit. */
    private final char echoParDefaut;

    public GestionnaireInterfaceGraphique(LoginController loginController,
                                          CompteController compteController,
                                          VirementController virementController) {
        this.loginController = loginController;
        this.compteController = compteController;
        this.virementController = virementController;

        // Initialiser la fenêtre
        setTitle("MA BANQUE");
        setSize(400, 300);// taille de la fenetre
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // orientation verticale

        id = new JTextField(PLACEHOLDER_ID, 20);
        mdp = new JPasswordField(PLACEHOLDER_MDP, 20);
        echoParDefaut = mdp.getEchoChar();
        // Le texte d'invite doit rester lisible : pas de masquage tant qu'il est affiché.
        mdp.setEchoChar((char) 0);
        bouton = new JButton(" Se connecter  ");

        id.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (id.getText().equals(PLACEHOLDER_ID)) {
                    id.setText("");
                    id.setForeground(Color.BLACK); // Réinitialiser la couleur du texte à noir
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (id.getText().isEmpty()) {
                    id.setText(PLACEHOLDER_ID);
                    id.setForeground(Color.GRAY); // Changer la couleur du texte en gris
                }
            }
        });
        mdp.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (String.valueOf(mdp.getPassword()).equals(PLACEHOLDER_MDP)) {
                    mdp.setText("");
                    mdp.setEchoChar(echoParDefaut); // la vraie saisie est masquée
                    mdp.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (mdp.getPassword().length == 0) {
                    mdp.setEchoChar((char) 0);
                    mdp.setText(PLACEHOLDER_MDP);
                    mdp.setForeground(Color.GRAY); // Changer la couleur du texte en gris
                }
            }
        });

        id.setPreferredSize(new Dimension(1, 1));
        mdp.setPreferredSize(new Dimension(1, 1));
        // Ajouter les composants à la fenêtre
        panel.add(id);
        panel.add(mdp);
        panel.add(bouton);
        panel.add(new JLabel());
        add(panel);

        bouton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Client client = loginController.authentifier(id.getText(), lireMotDePasse());
                    System.out.println("CONNEXION REUSSIE");
                    System.out.println("LA PERSONNE  : " + client.getNom());
                    new ClientInfoWindow(client, compteController, virementController);
                } catch (BanqueException ex) {
                    JOptionPane.showMessageDialog(GestionnaireInterfaceGraphique.this,
                            ex.getMessage(), "Échec de la connexion", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Afficher la fenêtre
        setVisible(true);
    }

    /** Récupère la saisie, en ignorant le texte d'invite. */
    private String lireMotDePasse() {
        String saisie = String.valueOf(mdp.getPassword());
        return PLACEHOLDER_MDP.equals(saisie) ? "" : saisie;
    }
}
