package com.datamaster.common.utils.ca;

import org.bouncycastle.util.io.pem.PemReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CaCertificateTest {

    private static X509Certificate rootCertificate;
    private static PrivateKey rootPrivateKey;

    @BeforeAll
    static void generateRootCertificate() throws Exception {
        String subject = "CN=DataMaster Test CA,OU=IT,O=DataMaster,C=CN";
        List<MultipartFile> files = CaGenerateRootCertificate.generateRootCertificate(subject);
        assertEquals(2, files.size());
        assertEquals("rootCA.cer", files.get(0).getOriginalFilename());
        assertEquals("privateKey.pem", files.get(1).getOriginalFilename());
        rootCertificate = readCertificate(files.get(0));
        rootPrivateKey = readPrivateKey(files.get(1));
        assertEquals(new X500Principal(subject), rootCertificate.getSubjectX500Principal());
    }

    @Test
    void rootCertificateIsSelfSignedAndMatchesPrivateKey() throws Exception {
        rootCertificate.checkValidity();
        rootCertificate.verify(rootCertificate.getPublicKey());
        assertEquals(3, rootCertificate.getVersion());
        assertEquals("SHA256withRSA", rootCertificate.getSigAlgName());
        assertEquals(rootCertificate.getSubjectX500Principal(), rootCertificate.getIssuerX500Principal());
        assertPrivateKeyMatches(rootCertificate, rootPrivateKey);
    }

    @Test
    void issuedCertificateIncludesDomainSan() throws Exception {
        assertIssuedCertificate("api.example.com", 2, "api.example.com");
    }

    @Test
    void issuedCertificateIncludesIpv4San() throws Exception {
        assertIssuedCertificate("192.0.2.10", 7, "192.0.2.10");
    }

    @Test
    void issuedCertificateIncludesWildcardSan() throws Exception {
        assertIssuedCertificate("*.example.com", 2, "*.example.com");
    }

    @Test
    void issuedCertificateWithoutDomainOmitsSan() throws Exception {
        X509Certificate certificate = issueCertificate("DataMaster User");
        assertNull(certificate.getSubjectAlternativeNames());
    }

    private void assertIssuedCertificate(String commonName, int sanType, String sanValue) throws Exception {
        X509Certificate certificate = issueCertificate(commonName);
        Collection<List<?>> names = certificate.getSubjectAlternativeNames();
        assertNotNull(names);
        assertEquals(1, names.size());
        List<?> san = names.iterator().next();
        assertEquals(sanType, san.get(0));
        assertEquals(sanValue, san.get(1));
    }

    private X509Certificate issueCertificate(String commonName) throws Exception {
        String subject = "CN=" + commonName + ",O=DataMaster,C=CN";
        List<MultipartFile> files = CaCertificateIssuer.issueCertificate(
                subject, rootCertificate, rootPrivateKey, 1L);
        assertEquals(2, files.size());
        assertEquals(commonName + "_certificate.cer", files.get(0).getOriginalFilename());
        assertEquals(commonName + "_privateKey.pem", files.get(1).getOriginalFilename());
        X509Certificate certificate = readCertificate(files.get(0));
        certificate.checkValidity();
        certificate.verify(rootCertificate.getPublicKey());
        assertEquals(new X500Principal(subject), certificate.getSubjectX500Principal());
        assertEquals(rootCertificate.getSubjectX500Principal(), certificate.getIssuerX500Principal());
        assertPrivateKeyMatches(certificate, readPrivateKey(files.get(1)));
        return certificate;
    }

    private static X509Certificate readCertificate(MultipartFile file) throws Exception {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(file.getBytes()));
    }

    private static PrivateKey readPrivateKey(MultipartFile file) throws Exception {
        try (PemReader reader = new PemReader(new StringReader(
                new String(file.getBytes(), StandardCharsets.US_ASCII)))) {
            byte[] encoded = reader.readPemObject().getContent();
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));
        }
    }

    private static void assertPrivateKeyMatches(X509Certificate certificate, PrivateKey privateKey) throws Exception {
        byte[] payload = "DataMaster certificate compatibility".getBytes(StandardCharsets.UTF_8);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(payload);
        byte[] signed = signature.sign();
        signature.initVerify(certificate.getPublicKey());
        signature.update(payload);
        assertTrue(signature.verify(signed));
    }
}
