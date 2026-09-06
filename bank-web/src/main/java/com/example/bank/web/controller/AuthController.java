package com.example.bank.web.controller;

import com.example.bank.core.model.Client;
import com.example.bank.core.repository.JdbcClientRepository;
import com.example.bank.core.service.AuthService;
import com.example.bank.web.dto.ConnexionRequete;
import com.example.bank.web.dto.ConnexionReponse;
import com.example.bank.web.securite.JwtService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La seule route ouverte : échanger des identifiants contre un jeton.
 *
 * LE CONTRÔLEUR NE VÉRIFIE RIEN LUI-MÊME. Il appelle {@code AuthService}, qui
 * déroule la chaîne du coeur (existence → verrouillage → mot de passe),
 * compte les échecs et consigne au journal d'audit. En cas de refus, il ne
 * capture RIEN : l'exception traverse et c'est
 * {@code GestionnaireExceptionsAuthentification} qui la traduit en réponse
 * HTTP. Un {@code try/catch} ici donnerait un second endroit où le format du
 * refus pourrait diverger — et c'est précisément par là qu'une fuite
 * d'information se glisse.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JdbcClientRepository repository;
    private final JwtService jwtService;

    public AuthController(AuthService authService,
                          JdbcClientRepository repository,
                          JwtService jwtService) {
        this.authService = authService;
        this.repository = repository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ConnexionReponse connexion(@RequestBody ConnexionRequete requete) {
        Client client = authService.authentifier(nomDuRib(requete), requete.motDePasse());
        return new ConnexionReponse(
                jwtService.genererPour(client.getRib(), client.getNom()),
                "Bearer",
                JwtService.VALIDITE.toSeconds(),
                client.getRib(),
                client.getNom());
    }

    /**
     * L'API s'authentifie par RIB, {@code AuthService} par NOM — ce pont
     * traduit l'un en l'autre.
     *
     * POURQUOI PAS UN {@code authentifierParRib} DANS LE COEUR : cette phase
     * ne modifie pas {@code bank-core}. Traduire ici coûte une lecture de
     * plus et ne change rien à ce qui est vérifié ensuite.
     *
     * LA NON-DIVULGATION EST PRÉSERVÉE, et c'est le point délicat. Un RIB
     * inconnu rend {@code null}, que {@code AuthService} traite exactement
     * comme un nom inconnu : {@code VerificationClientExiste} vérifie la
     * saisie contre son haché factice — donc au même coût en temps qu'un vrai
     * refus — puis lève la MÊME exception, avec le MÊME message, qu'un mot de
     * passe faux. Ni le code HTTP, ni le corps, ni le temps de réponse ne
     * disent si le RIB existe.
     */
    private String nomDuRib(ConnexionRequete requete) {
        if (requete == null || requete.rib() == null) {
            return null;
        }
        return repository.findByRib(requete.rib()).map(Client::getNom).orElse(null);
    }
}
