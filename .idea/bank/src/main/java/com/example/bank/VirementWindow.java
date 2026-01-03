package com.example.bank;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class VirementWindow extends JFrame {
    private JTextField montantField;
    private JTextField destinataireField;
    private JButton effectuerButton;

    public VirementWindow(client c, client[] clients) {
        setTitle("Faire un virement");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 2));

        JLabel montantLabel = new JLabel("Montant :");
        JLabel destinataireLabel = new JLabel("Destinataire (RIB) :");

        montantField = new JTextField(10);
        destinataireField = new JTextField(10);
        effectuerButton = new JButton("Effectuer le virement");

        panel.add(montantLabel);
        panel.add(montantField);
        panel.add(destinataireLabel);
        panel.add(destinataireField);
        panel.add(new JLabel()); // Ajoute un espace vide pour l'alignement
        panel.add(effectuerButton);

        add(panel, BorderLayout.CENTER);

        effectuerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    float montant = Float.parseFloat(montantField.getText());
                    int destinataireRib = Integer.parseInt(destinataireField.getText());

                    boolean virementEffectue = false;

                    for (client dest : clients) {
                        if (dest != null && dest.getRib() == destinataireRib) {
                            c.effectuerVirement(montant, false, dest);
                            virementEffectue = true;
                            JOptionPane.showMessageDialog(null, "Virement de " + montant + "€ à " + dest.getNom() + " effectué avec succès !");
                            break;
                        }
                    }

                    if (!virementEffectue) {
                        JOptionPane.showMessageDialog(null, "Erreur : Destinataire introuvable.", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Erreur : Veuillez entrer un montant et un RIB valides.", "Erreur de saisie", JOptionPane.ERROR_MESSAGE);
                }

                dispose(); // Fermer la fenêtre de virement après l'exécution
            }
        });

        setVisible(true);
    }
}
