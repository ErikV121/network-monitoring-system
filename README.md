# Network Monitoring System

## Overview
This project is a real-time **Network Monitoring System** that tracks network activity, 
including upload/download speeds and packet loss. It uses **Spring Boot** for the backend, **WebSockets** 
for live updates, and **Chart.js** for data visualization. The system captures network packets using
**Pcap4J** and provides real-time insights through a web-based dashboard.

## How to Clone the Project
To get a copy of the project, run:
```sh
git clone https://github.com/ErikV121/network-monitoring-system.git
cd network-monitoring-system
```

## Prerequisites
### 1. Install Java 17
- Download and install Java 17: [Download Java 17](https://adoptium.net/)
- Verify installation:
  ```sh
  java -version
  ```
  Expected output:
  ```sh
  java version "17.0.x" ...
  ```

### 2. Install Maven
Maven is required to build and run the Spring Boot application.
- Download and install Maven: [Download Maven](https://maven.apache.org/download.cgi)
- Verify installation:
  ```sh
  mvn -version
  ```
  Expected output:
  ```sh
  Apache Maven x.x.x
  ```

### 3. Install Npcap (Version 1.80+)
This project requires **Npcap 1.80+** for packet capturing.
- [Download Npcap 1.80](https://nmap.org/npcap/)
- Enable support for raw 802.11 traffic (and monitor mode) for wireless adapters.

## Running the Application
### 1. Start the Backend
Run the Spring Boot application using Maven:
```sh
./mvnw spring-boot:run
```

### 2. Access the Frontend
The frontend is served automatically by Spring Boot from the `src/main/resources/static` folder.
- Open your browser and go to:  
  **`http://localhost:8080/`**

## Usage
1. Open the web dashboard.
2. Click **Connect** and **Play**  to start monitoring.
3. View real-time network speed and packet loss data.
4. Click **Pause** to temporarily stop updates.

## Future Enhancements

