package com.example.bank;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class historique extends JFrame {
    private JTextArea historiqueArea;
    private JButton fermerButton;

    public historique(List<transaction> historique) {
        setTitle("Historique des transactions");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        historiqueArea = new JTextArea();
        historiqueArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(historiqueArea);

        fermerButton = new JButton("Fermer");

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(fermerButton, BorderLayout.SOUTH);

        add(panel, BorderLayout.CENTER);

        fermerButton.addActionListener(e -> dispose());

        // Afficher l'historique des transactions
        for (transaction t : historique) {
            historiqueArea.append(t.toString() + "\n");
        }

        setVisible(true);
    }
}
