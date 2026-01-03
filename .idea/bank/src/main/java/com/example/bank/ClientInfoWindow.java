package com.example.bank;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ClientInfoWindow extends JFrame {
    private JLabel nomLabel;
    private JLabel ribLabel;
    private JLabel soldeLabel;
    private JLabel sommelabel;
    private JButton fermerButton;
    private JButton creationlivretA;
    private JButton virement ;
    private JButton historiqueButton ;
    private List<transaction> historique;

    public ClientInfoWindow(client c ,client[] l,String nom, int rib, float solde, boolean livret, float somme , List<transaction> h) {
        setTitle("Informations du client");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 2));

        nomLabel = new JLabel("Nom : " + nom);
        sommelabel = new JLabel("Epargne  : " + somme);
        ribLabel = new JLabel("RIB : " + rib);
        soldeLabel = new JLabel("Solde : " + solde);
        historique= h;
        virement=new JButton("Faire un virement ");
        fermerButton = new JButton("Fermer");
        historiqueButton=new JButton("Afficher historique");

        panel.add(nomLabel);
        if (livret) {
            panel.add(sommelabel);
        }
        //panel.add(historique);
        panel.add(ribLabel);
        panel.add(soldeLabel);
        panel.add(fermerButton);
        panel.add(virement);
        panel.add(historiqueButton);

        add(panel, BorderLayout.CENTER);

        fermerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose(); // Fermer la fenêtre d'informations du client
            }
        });
        virement.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new VirementWindow(c,l);
            }
        });
        historiqueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new historique(historique);
            }
        });
        setVisible(true);
    }
}

