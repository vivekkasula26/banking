package com.beko.DemoBank_v1.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import java.net.URL;
import java.net.UnknownHostException;

@RestController
@RequestMapping("/tls-dns")
public class SimulateTlsDns {

    private static final Logger log = LoggerFactory.getLogger(SimulateTlsDns.class);

    // 1. Expired SSL Certificate
    @GetMapping("/expired-cert")
    public ResponseEntity<String> expiredCert() {
        try {
            URL url = new URL("https://expired.badssl.com/");
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.connect();
            return ResponseEntity.ok("Connected (expired cert site)");
        } catch (SSLHandshakeException e) {
            log.error("Expired SSL Certificate error at /expired-cert", e);
            return ResponseEntity.status(495).body("SSL Expired Cert Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error at /expired-cert", e);
            return ResponseEntity.status(500).body("Unexpected Error: " + e.getMessage());
        }
    }

    // 2. Wrong Domain (DNS error)
    @GetMapping("/wrong-domain")
    public ResponseEntity<String> wrongDomain() {
        try {
            URL url = new URL("https://nonexistent1234.fake-domain.com/");
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.connect();
            return ResponseEntity.ok("Connected (unexpected)");
        } catch (UnknownHostException e) {
            log.error("DNS resolution failed at /wrong-domain", e);
            return ResponseEntity.status(502).body("DNS Resolution Failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error at /wrong-domain", e);
            return ResponseEntity.status(500).body("Unexpected Error: " + e.getMessage());
        }
    }

    // 3. Certificate Mismatch (hostname mismatch)
    @GetMapping("/cert-mismatch")
    public ResponseEntity<String> certMismatch() {
        try {
            URL url = new URL("https://wrong.host.badssl.com/");
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.connect();
            return ResponseEntity.ok("Connected (hostname mismatch site)");
        } catch (SSLHandshakeException e) {
            log.error("SSL Hostname mismatch at /cert-mismatch", e);
            return ResponseEntity.status(496).body("SSL Hostname Mismatch Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error at /cert-mismatch", e);
            return ResponseEntity.status(500).body("Unexpected Error: " + e.getMessage());
        }
    }

}
