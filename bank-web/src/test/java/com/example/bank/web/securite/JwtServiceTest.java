package com.example.bank.web.securite;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le service de jetons, seul — sans contexte Spring, sans base, sans HTTP.
 *
 * Ces tests coûtent quelques millisecondes là où un {@code @SpringBootTest} en
 * coûte quelques secondes : la signature et l'expiration d'un JWT n'ont besoin
 * d'aucune des deux.
 */
@DisplayName("Service de jetons JWT")
class JwtServiceTest {

    private static final String SECRET = "un-secret-de-test-de-32-caracteres-minimum";
    private static final int RIB = 123;

    private final JwtService service = new JwtService(SECRET);

    @Test
    @DisplayName("Un jeton fraîchement émis rend le RIB qu'on y a mis")
    void allerRetourDuRib() {
        String jeton = service.genererPour(RIB, "Mouh");

        assertEquals(RIB, service.ribDuJeton(jeton).orElseThrow());
    }

    @Test
    @DisplayName("Deux clients reçoivent deux jetons portant chacun son RIB")
    void jetonsDistincts() {
        assertEquals(123, service.ribDuJeton(service.genererPour(123, "Mouh")).orElseThrow());
        assertEquals(456, service.ribDuJeton(service.genererPour(456, "amine")).orElseThrow());
    }

    /**
     * Un JWT est signé, PAS chiffré : sa charge utile se décode en base64 sans
     * clé. Ce test vérifie donc ce qu'on a choisi d'y mettre — et le mot de
     * passe n'en fait pas partie.
     */
    @Test
    @DisplayName("La charge utile du jeton ne contient aucun secret")
    void chargeUtileSansSecret() {
        String jeton = service.genererPour(RIB, "Mouh");
        String charge = new String(java.util.Base64.getUrlDecoder()
                .decode(jeton.split("\\.")[1]));

        assertTrue(charge.contains("123"), charge);
        assertTrue(charge.contains("Mouh"), charge);
        assertEquals(-1, charge.indexOf("$2a$"), "Un haché a fuité dans le jeton : " + charge);
    }

    @Test
    @DisplayName("Un jeton signé avec une autre clé est rejeté")
    void signatureEtrangereRejetee() {
        String forge = new JwtService("une-tout-autre-cle-de-32-caracteres-au-moins")
                .genererPour(RIB, "Mouh");

        assertTrue(service.ribDuJeton(forge).isEmpty());
    }

    @Test
    @DisplayName("Un jeton dont la charge utile a été modifiée est rejeté")
    void jetonAltereRejete() {
        String jeton = service.genererPour(RIB, "Mouh");
        String[] parties = jeton.split("\\.");
        String chargeForgee = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"456\"}").getBytes());

        assertTrue(service.ribDuJeton(
                parties[0] + "." + chargeForgee + "." + parties[2]).isEmpty());
    }

    @Test
    @DisplayName("Une chaîne qui n'est pas un jeton est rejetée sans lever d'exception")
    void chaineQuelconqueRejetee() {
        assertTrue(service.ribDuJeton("pas-un-jeton").isEmpty());
        assertTrue(service.ribDuJeton("").isEmpty());
        assertTrue(service.ribDuJeton("a.b.c").isEmpty());
    }

    /**
     * HS256 exige une clé d'au moins 256 bits. Un secret plus court doit être
     * refusé AU DÉMARRAGE — laisser l'application monter avec une clé faible
     * serait pire qu'un échec bruyant.
     */
    @Test
    @DisplayName("Un secret de moins de 32 caractères est refusé à la construction")
    void secretTropCourtRefuse() {
        IllegalStateException levee = assertThrows(IllegalStateException.class,
                () -> new JwtService("trop-court"));

        assertTrue(levee.getMessage().contains("32"), levee.getMessage());
    }

    @Test
    @DisplayName("La durée de validité annoncée est bien d'une heure")
    void validiteDUneHeure() {
        assertEquals(3600, JwtService.VALIDITE.toSeconds());
    }

    @Test
    @DisplayName("Deux jetons pour le même client ne sont pas forcément identiques")
    void jetonsPourLeMemeClient() {
        // Même RIB, même nom : seule la date d'émission peut différer. On
        // vérifie surtout que les deux restent valides et rendent le même RIB.
        String premier = service.genererPour(RIB, "Mouh");
        String second = service.genererPour(RIB, "Mouh");

        assertEquals(service.ribDuJeton(premier), service.ribDuJeton(second));
        assertNotEquals(0, premier.length());
    }
}
