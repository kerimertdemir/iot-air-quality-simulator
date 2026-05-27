package com.iot.airquality;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Random;

public class AirQualitySimulatorApp {

    // AWS IoT Core Endpoint Address
    private static final String AWS_ENDPOINT = "a2hva0xatfqa10-ats.iot.eu-north-1.amazonaws.com"; 
    private static final String CLIENT_ID = "Smart_City_Air_Sensor_01";
    private static final String TOPIC = "smartcity/airquality";

    // STRICTLY NO ABSOLUTE PATHS! Only file names, Java will locate them via Classpath.
    private static final String ROOT_CA_FILE = "AmazonRootCA1.pem";
    private static final String CERTIFICATE_FILE = "f9f607ac8e891cbd232e6ca69debcc3b2df20cf52a29c01eb53c068b5fc4b390-certificate.pem.crt";
    private static final String PRIVATE_KEY_FILE = "f9f607ac8e891cbd232e6ca69debcc3b2df20cf52a29c01eb53c068b5fc4b390-private.pem.key";

    public static void main(String[] args) {
        String brokerUrl = "ssl://" + AWS_ENDPOINT + ":8883";

        try {
            System.out.println("[SYSTEM] Initiating encrypted connection to AWS IoT Core...");
            
            MqttClient client = new MqttClient(brokerUrl, CLIENT_ID, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setKeepAliveInterval(60);

            options.setSocketFactory(getSocketFactory(ROOT_CA_FILE, CERTIFICATE_FILE, PRIVATE_KEY_FILE));

            client.connect(options);
            System.out.println("[SUCCESS] Connected to Cloud Server via MQTT Protocol!\n");

            // Fixed Location for the physical sensor
            String fixedLocation = "Kizilay Meydani, Ankara";
            Random random = new Random();

            while (true) {
                // Smart City Air Quality Mock Data Template
                JSONObject payload = new JSONObject();
                payload.put("sensor_id", CLIENT_ID);
                payload.put("location", fixedLocation);
                payload.put("pm2_5_level", random.nextInt(40) + 20);      // Air Pollution Level (20-60 PM2.5)
                payload.put("co2_level", random.nextInt(300) + 400);      // CO2 Level (400-700 ppm)
                payload.put("humidity_percent", random.nextInt(20) + 40); // Humidity Percentage (40-60%)
                payload.put("timestamp", Instant.now().toString());

                MqttMessage message = new MqttMessage(payload.toString().getBytes());
                message.setQos(1); // Quality of Service: At least once delivery guarantee

                client.publish(TOPIC, message);
                System.out.println("[DATA PUBLISHED] ☁️ " + payload.toString());

                // Sends data continuously every 5 seconds
                Thread.sleep(5000); 
            }

        } catch (Exception e) {
            System.out.println("[ERROR] Simulation crashed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========================================================================
    // DYNAMIC SSL CERTIFICATE MANAGEMENT (READING FROM RESOURCES FOLDER)
    // ========================================================================
    private static SSLSocketFactory getSocketFactory(String rootCaName, String certName, String keyName) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        
        InputStream caIn = AirQualitySimulatorApp.class.getResourceAsStream("/certs/" + rootCaName);
        InputStream certIn = AirQualitySimulatorApp.class.getResourceAsStream("/certs/" + certName);
        InputStream keyIn = AirQualitySimulatorApp.class.getResourceAsStream("/certs/" + keyName);

        if (caIn == null || certIn == null || keyIn == null) {
            throw new FileNotFoundException("Certificate files not found! Please check the names in the /certs/ folder.");
        }

        X509Certificate caCert = (X509Certificate) cf.generateCertificate(caIn);
        X509Certificate cert = (X509Certificate) cf.generateCertificate(certIn);

        KeyPair keyPair;
        try (PEMParser pemParser = new PEMParser(new InputStreamReader(keyIn))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            if (object instanceof PEMKeyPair) {
                keyPair = converter.getKeyPair((PEMKeyPair) object);
            } else {
                keyPair = new KeyPair(null, converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) object));
            }
        }

        KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
        caKs.load(null, null);
        caKs.setCertificateEntry("ca-certificate", caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(caKs);

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry("certificate", cert);
        ks.setKeyEntry("private-key", keyPair.getPrivate(), "".toCharArray(), new java.security.cert.Certificate[]{cert});
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "".toCharArray());

        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return context.getSocketFactory();
    }
}