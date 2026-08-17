package com.example.bank.swing.ui;

import com.example.bank.core.exception.BanqueException;
import com.example.bank.core.model.Client;
import com.example.bank.swing.controller.VirementController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.math.BigDecimal;

/**
 * Formulaire de virement.
 *
 * La fenêtre ne fait que trois choses : lire la saisie, appeler le controller,
 * afficher le message d'erreur métier s'il y en a un. Aucune règle bancaire
 * n'est écrite ici.
 */
public class VirementWindow extends JFrame {

    private final VirementController virementController;
    private final Client emetteur;
    /** Prévient la fenêtre appelante qu'un virement a abouti. */
    private final Runnable apresVirement;

    private final JTextField montantField = new JTextField(10);
    private final JTextField destinataireField = new JTextField(10);
    private final JRadioButton versAutreClient = new JRadioButton("Vers un autre client", true);
    private final JRadioButton versMonLivretA = new JRadioButton("Vers mon Livret A");

    public VirementWindow(Client emetteur, VirementController virementController,
                          Runnable apresVirement) {
        this.emetteur = emetteur;
        this.virementController = virementController;
        this.apresVirement = apresVirement;

        setTitle("Faire un virement");
        setSize(380, 220);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 2));

        ButtonGroup groupe = new ButtonGroup();
        groupe.add(versAutreClient);
        groupe.add(versMonLivretA);

        JPanel typePanel = new JPanel(new GridLayout(2, 1));
        typePanel.add(versAutreClient);
        typePanel.add(versMonLivretA);

        JButton effectuerButton = new JButton("Effectuer le virement");

        // Le RIB n'a pas de sens pour un virement interne.
        ActionListener majSaisieRib = e -> {
            destinataireField.setEnabled(versAutreClient.isSelected());
            if (!versAutreClient.isSelected()) {
                destinataireField.setText("");
            }
        };
        versAutreClient.addActionListener(majSaisieRib);
        versMonLivretA.addActionListener(majSaisieRib);

        panel.add(new JLabel("Type de virement :"));
        panel.add(typePanel);
        panel.add(new JLabel("Montant :"));
        panel.add(montantField);
        panel.add(new JLabel("Destinataire (RIB) :"));
        panel.add(destinataireField);
        panel.add(new JLabel()); // Ajoute un espace vide pour l'alignement
        panel.add(effectuerButton);

        add(panel, BorderLayout.CENTER);

        effectuerButton.addActionListener(e -> effectuerVirement());

        setVisible(true);
    }

    private void effectuerVirement() {
        BigDecimal montant;
        try {
            montant = new BigDecimal(montantField.getText().trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            afficherErreur("Veuillez entrer un montant valide.", "Erreur de saisie");
            return;
        }

        try {
            if (versMonLivretA.isSelected()) {
                virementController.virerVersLivretA(emetteur.getRib(), montant);
                JOptionPane.showMessageDialog(this,
                        "Virement de " + montant + " € vers votre Livret A effectué avec succès !");
            } else {
                int destinataireRib;
                try {
                    destinataireRib = Integer.parseInt(destinataireField.getText().trim());
                } catch (NumberFormatException ex) {
                    afficherErreur("Veuillez entrer un RIB valide.", "Erreur de saisie");
                    return;
                }

                virementController.virer(emetteur.getRib(), destinataireRib, montant);
                JOptionPane.showMessageDialog(this,
                        "Virement de " + montant + " € vers le RIB " + destinataireRib
                                + " effectué avec succès !");
            }
        } catch (BanqueException ex) {
            // Montant invalide, solde insuffisant, Livret A absent,
            // destinataire introuvable, virement vers soi-même...
            afficherErreur(ex.getMessage(), "Virement refusé");
            return;
        }

        apresVirement.run();  // la fenêtre appelante réaffiche les soldes à jour
        dispose();            // fermeture uniquement si le virement a abouti
    }

    private void afficherErreur(String message, String titre) {
        JOptionPane.showMessageDialog(this, message, titre, JOptionPane.ERROR_MESSAGE);
    }
}
