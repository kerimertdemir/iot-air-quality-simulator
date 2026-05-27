# Cloud-Native Smart City IoT Simulator ☁️🌍

A professional Java-based IoT simulator designed to generate real-time air quality data (PM2.5, CO2, Humidity) for Smart City ecosystems and securely publish it to **AWS IoT Core** using the MQTT protocol.

## 🚀 Technologies & Architecture
* **Backend:** Java 8+ (Maven)
* **Cloud Platform:** AWS IoT Core
* **Communication Protocol:** MQTT (Eclipse Paho Client)
* **Security & Cryptography:** BouncyCastle (X.509 Dynamic Certificate Loading, TLS 1.2)
* **Data Format:** JSON (org.json)

## ⚙️ System Features
- **Dynamic SSL Management:** X.509 Certificates and Private Keys are loaded dynamically via the `ClassLoader` from the resources folder, eliminating hardcoded absolute paths.
- **Asynchronous Data Publishing:** Generates and publishes an environmental telemetry payload every 5 seconds.
- **Time-Series Ready:** Every payload is stamped with an ISO 8601 UTC `timestamp` for seamless integration into cloud databases and analytics dashboards.

## 📊 Sample Payload
```json
{
  "sensor_id": "Smart_City_Air_Sensor_01",
  "location": "Kizilay Meydani, Ankara",
  "pm2_5_level": 42,
  "co2_level": 580,
  "humidity_percent": 45,
  "timestamp": "2026-05-27T18:31:32Z"
}
