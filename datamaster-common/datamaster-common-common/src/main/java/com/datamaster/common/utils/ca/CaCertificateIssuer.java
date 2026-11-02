

package com.datamaster.common.utils.ca;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ReUtil;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * 证书颁发工具类，提供方法使用根证书和私钥签发用户证书。
 * 本工具类通过加载根证书和私钥，生成用户证书并返回MultipartFile列表。
 * @author DATAMASTER
 */
public class CaCertificateIssuer {

    /**
     * 签发用户证书并返回证书和私钥的 MultipartFile 列表
     *
     * @param userName 包含用户的详细信息
     * @param certUrl 根证书的 URL
     * @param privateKeyUrl 私钥的 URL
     * @param validity 证书的有效期（年）
     * @return List<MultipartFile> 包含用户证书和私钥的 MultipartFile 列表
     * @throws Exception 如果签发过程中发生错误
     */
    public static List<MultipartFile> issueCertificate(String userName, String certUrl,
                                                       String privateKeyUrl, Long validity) throws Exception {
        // 加载根证书和私钥
        X509Certificate rootCertificate = loadRootCertificate(certUrl);
        PrivateKey rootPrivateKey = loadRootPrivateKey(privateKeyUrl);
        return issueCertificate(userName, rootCertificate, rootPrivateKey, validity);
    }

    static List<MultipartFile> issueCertificate(String userName, X509Certificate rootCertificate,
                                               PrivateKey rootPrivateKey, Long validity) throws Exception {
        X500Principal userDnName = new X500Principal(userName);
        List<MultipartFile> fileList = new ArrayList<>();

        // 获取根证书的主体信息，作为用户证书的颁发者信息
        X500Principal rootDnName = rootCertificate.getSubjectX500Principal();

        // 生成用户的密钥对（公钥和私钥）
        KeyPair userKeyPair = generateUserKeyPair();
        PublicKey userPublicKey = userKeyPair.getPublic();
        PrivateKey userPrivateKey = userKeyPair.getPrivate();

        // 使用公开 API 创建 X.509 用户证书
        long currentTime = System.currentTimeMillis();
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                rootDnName, BigInteger.valueOf(currentTime), new Date(currentTime),
                new Date(currentTime + validity * 365L * 24L * 60L * 60L * 1000L),
                userDnName, userPublicKey);

        // 添加主题扩展字段 (SAN)，用于浏览器 https 验证
        String dnsName = ReUtil.get("CN=([^,]+)", userDnName.getName(), 1);
        // 判断是否是IP地址或域名
        boolean isIpAddress = Validator.isIpv4(dnsName);
        // 判断是否是域名
        boolean isDomain = ReUtil.isMatch("^(\\*\\.)?([\\w-]+\\.)+[a-zA-Z]{2,}$", dnsName);

        if (isIpAddress || isDomain) {
            GeneralName san = new GeneralName(
                    isIpAddress ? GeneralName.iPAddress : GeneralName.dNSName, dnsName);
            certBuilder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(san));
        }

        // 使用根证书的私钥签署用户证书
        X509Certificate userCertificate = new JcaX509CertificateConverter().getCertificate(
                certBuilder.build(new JcaContentSignerBuilder("SHA256withRSA").build(rootPrivateKey)));

        // 将用户证书转换为 MultipartFile
        fileList.add(convertCertificateToMultipartFile(userCertificate, dnsName + "_certificate.cer"));

        // 将用户私钥保存为 PEM 文件并转换为 MultipartFile
        fileList.add(convertPrivateKeyToMultipartFile(userPrivateKey, dnsName + "_privateKey.pem"));

        return fileList;
    }

    /**
     * 生成 RSA 密钥对
     *
     * @return 生成的 RSA 密钥对
     * @throws Exception 如果密钥对生成失败
     */
    private static KeyPair generateUserKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * 将 X.509 证书对象转换为 MultipartFile
     *
     * @param certificate X.509 证书对象
     * @param fileName 文件名
     * @return MultipartFile 形式的证书
     * @throws Exception 如果转换过程中发生错误
     */
    private static MultipartFile convertCertificateToMultipartFile(X509Certificate certificate, String fileName) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.US_ASCII))) {
            writer.write("-----BEGIN CERTIFICATE-----\n");
            writer.write(Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(certificate.getEncoded()));
            writer.write("\n-----END CERTIFICATE-----\n");
        }
        return new MockMultipartFile(fileName, fileName, "application/x-x509-ca-cert", outputStream.toByteArray());
    }

    /**
     * 将私钥对象转换为 MultipartFile
     *
     * @param privateKey 私钥对象
     * @param fileName 文件名
     * @return MultipartFile 形式的私钥
     * @throws Exception 如果转换过程中发生错误
     */
    private static MultipartFile convertPrivateKeyToMultipartFile(PrivateKey privateKey, String fileName) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PemWriter pemWriter = new PemWriter(new OutputStreamWriter(outputStream, StandardCharsets.US_ASCII))) {
            PemObject pemObject = new PemObject("PRIVATE KEY", privateKey.getEncoded());
            pemWriter.writeObject(pemObject);
        }
        return new MockMultipartFile(fileName, fileName, "application/x-pem-file", outputStream.toByteArray());
    }

    /**
     * 从指定的 URL 加载根证书
     *
     * @param certUrl 根证书的 URL
     * @return 加载的 X509Certificate 对象
     * @throws Exception 如果加载过程中发生错误
     */
    public static X509Certificate loadRootCertificate(String certUrl) throws Exception {
        try (InputStream certStream = new URL(getServerIpAndPort() + certUrl).openStream()) {
            if (certStream == null) {
                throw new IllegalArgumentException("根证书文件未找到！");
            }
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(certStream);
        }
    }

    /**
     * 从指定的 URL 加载私钥
     *
     * @param privateKeyUrl 私钥的 URL
     * @return 加载的 PrivateKey 对象
     * @throws Exception 如果加载过程中发生错误
     */
    public static PrivateKey loadRootPrivateKey(String privateKeyUrl) throws Exception {
        try (InputStream keyStream = new URL(getServerIpAndPort() + privateKeyUrl).openStream()) {
            if (keyStream == null) {
                throw new IllegalArgumentException("私钥文件未找到！");
            }
            PemReader pemReader = new PemReader(new InputStreamReader(keyStream));
            byte[] keyBytes = pemReader.readPemObject().getContent();
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        }
    }


    /**
     * 获取当前后端服务器的 IP 和端口
     *
     * @return 服务器的 IP 和端口，格式为 "IP:端口"
     */
    public static String getServerIpAndPort() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        // 获取服务器的 IP 和端口
        int serverPort = request.getLocalPort();

        return "http://127.0.0.1" + ":" + serverPort;
    }


    public static void main(String[] args) throws Exception {
        // 定义用户信息
        String userName = "CN=::www.wangming.xyz, OU=IT, O=盐城市国有资产投资集团有限公司, L=Yancheng, ST=Yancheng, C=CN";

        // 定义根证书和私钥的 URL
        String certUrl = "http://127.0.0.1:8000/local-plus/66c1f165146fbf2cdaf53f55.cer";
        String privateKeyUrl = "http://127.0.0.1:8000/local-plus/66c1f166146fbf2cdaf53f56.pem";

        // 签发证书并获取 MultipartFile 列表
        List<MultipartFile> files = issueCertificate(userName, certUrl, privateKeyUrl, 1L);

        // 打印生成的文件名称
        for (MultipartFile file : files) {
            System.out.println("生成的文件: " + file.getOriginalFilename());
        }
    }
}
