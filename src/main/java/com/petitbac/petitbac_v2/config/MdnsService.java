package com.petitbac.petitbac_v2.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Annonce le serveur sous une adresse fixe et lisible sur le reseau local : petitbac.local.
 *
 * Grace a mDNS (le meme mecanisme que Bonjour/AirPlay), tout appareil sur le meme Wi-Fi
 * peut ouvrir http://petitbac.local:8080 sans connaitre l'adresse IP. L'hote n'a donc plus
 * qu'a dicter cette adresse une seule fois ; ensuite, seul le code de salle est echange.
 *
 * Aucune configuration systeme requise : l'annonce vit aussi longtemps que l'application tourne.
 */
@Component
public class MdnsService {

    private static final Logger log = LoggerFactory.getLogger(MdnsService.class);

    /** Nom annonce sur le reseau -> resoluble en "petitbac.local". */
    private static final String HOSTNAME = "petitbac";

    @Value("${server.port:8080}")
    private int port;

    private JmDNS jmdns;
    private ServiceInfo serviceInfo;

    /** Adresse fixe a partager (ex : http://petitbac.local:8080), vide si mDNS indisponible. */
    private String url = "";

    public String getUrl() {
        return url;
    }

    @PostConstruct
    public void start() {
        try {
            InetAddress lanIp = resolveLanAddress();
            jmdns = JmDNS.create(lanIp, HOSTNAME);

            // Service HTTP annonce (utile pour les outils de decouverte type "Bonjour Browser")
            serviceInfo = ServiceInfo.create(
                    "_http._tcp.local.", "Petit Bac", port, "path=/lobby");
            jmdns.registerService(serviceInfo);

            url = "http://" + HOSTNAME + ".local:" + port;
            log.info("mDNS actif. Adresse fixe a partager : {}  (IP reseau : {})",
                    url, lanIp.getHostAddress());
        } catch (Exception e) {
            // Echec non bloquant : le jeu reste accessible via l'IP brute classique.
            log.warn("mDNS indisponible ({}). Repli sur l'adresse IP classique.", e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        try {
            if (jmdns != null) {
                jmdns.unregisterAllServices();
                jmdns.close();
            }
        } catch (Exception e) {
            log.debug("Fermeture mDNS : {}", e.getMessage());
        }
    }

    /**
     * Determine l'IP du reseau local reellement utilisee pour communiquer.
     *
     * On ouvre un socket UDP "vers l'exterieur" : aucun paquet n'est envoye, mais l'OS
     * choisit l'interface de sortie par defaut. Cela selectionne la bonne carte Wi-Fi/Ethernet
     * et evite les pieges (loopback 127.0.0.1, interface virtuelle WSL/Docker).
     */
    private InetAddress resolveLanAddress() throws Exception {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            InetAddress local = socket.getLocalAddress();
            if (local != null && !local.isAnyLocalAddress()) {
                return local;
            }
        } catch (Exception ignored) {
            // Pas de route reseau : on retombe sur la resolution classique ci-dessous.
        }
        return InetAddress.getLocalHost();
    }
}
