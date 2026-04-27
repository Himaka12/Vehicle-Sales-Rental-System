<div align="center">

# 🚗 Vehicle Sales & Rental System

**A full-stack web application for managing vehicle sales and rentals with AI-powered sentiment analysis**

[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com)
[![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org)

</div>

---

## 📌 Overview

The **Vehicle Sales & Rental System** is a centralized platform designed to streamline vehicle business operations. It enables users to browse vehicle listings, manage sales and rentals, store vehicle images, and analyze customer feedback using AI-driven sentiment analysis.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🚘 **Vehicle Management** | Add, update, and manage vehicle listings |
| 💰 **Sales System** | Handle vehicle purchase transactions |
| 📅 **Rental System** | Manage rental bookings and availability |
| 🖼️ **Image Storage** | Upload and serve vehicle images |
| 💬 **Customer Reviews** | Collect and display user feedback |
| 🤖 **Sentiment Analysis** | AI-powered analysis of customer reviews |
| 🗄️ **Database Integration** | Full MySQL persistence layer |
| 🌐 **Web Interface** | Intuitive browser-based UI |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java, Spring Boot |
| **Frontend** | HTML, CSS, JavaScript |
| **Database** | MySQL |
| **Build Tool** | Maven |

---

## 📁 Project Structure

```
Vehicle-Sales-Rental-System/
│
├── Vehicle/Vehicle/                     # ⚠️ Main project lives here
│   ├── src/
│   │   └── main/
│   │       ├── java/                    # Backend source code
│   │       └── resources/
│   │           ├── static/
│   │           │   └── uploads/         # Uploaded vehicle images
│   │           ├── templates/           # HTML templates
│   │           └── application.properties
│   ├── pom.xml                          # Maven configuration
│   └── Application.java                # Main entry point
│
├── ShowProps.java
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.0+
- IntelliJ IDEA (recommended)

---

### 1. Clone the Repository

```bash
git clone https://github.com/Himaka12/Vehicle-Sales-Rental-System.git
cd Vehicle-Sales-Rental-System
```

### 2. Open in IDE

1. Open **IntelliJ IDEA**
2. Click **Open** → select the project folder
3. Wait for indexing to complete

> ⚠️ **Note:** The main project is located inside `Vehicle/Vehicle/`

---

### 3. Reload Maven Dependencies

1. Open `pom.xml`
2. Right-click → **Maven** → **Reload Project**

---

### 4. Set Up the MySQL Database

Connect to your MySQL instance and run:

```sql
CREATE DATABASE vehicle_sales_rental_system;
```

---

### 5. Configure Database Connection

Open `src/main/resources/application.properties` and update:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/vehicle_sales_rental_system
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

---

### 6. Run the Application

**Option A — Via IDE:**
1. Locate `Application.java`
2. Right-click → **Run**

**Option B — Via Terminal:**
```bash
mvn spring-boot:run
```

---

### 7. Access the Application

Open your browser and navigate to:

```
http://localhost:8080
```

---

## 🧩 System Modules

```
┌─────────────────────────────────────────────────┐
│              Vehicle Sales & Rental              │
├──────────────┬───────────────┬───────────────────┤
│  🚘 Vehicle  │ 💰 Sales       │ 📅 Rental          │
│  Management  │ Management    │ Management        │
├──────────────┴───────────────┴───────────────────┤
│  👤 Customer Management                          │
├──────────────────────────────────────────────────┤
│  💬 Review & 🤖 Sentiment Analysis               │
└──────────────────────────────────────────────────┘
```

| Module | Description |
|---|---|
| **Vehicle Management** | Manage vehicle details and availability status |
| **Sales Management** | Handle and track vehicle purchase records |
| **Rental Management** | Manage bookings, duration, and returns |
| **Customer Management** | Store and manage customer data |
| **Review & Sentiment Analysis** | Collect reviews and run AI-based feedback analysis |

---

## 🖼️ Image Storage

Uploaded vehicle images are stored in:

```
src/main/resources/static/uploads/
```

> Ensure this directory exists before uploading images.

---

## ⚠️ Common Issues & Fixes

| Issue | Fix |
|---|---|
| Maven dependencies not loading | Right-click `pom.xml` → Maven → Reload Project |
| Database connection error | Verify username, password, and DB name in `application.properties` |
| Port already in use | Add `server.port=8081` to `application.properties` |
| Images not displaying | Confirm the `uploads/` directory exists in `static/` |

---

## 🔮 Future Improvements

- [ ] User authentication (login / signup)
- [ ] Admin dashboard
- [ ] Payment gateway integration
- [ ] Advanced search and filter options
- [ ] AI-based vehicle recommendations
- [ ] Cloud deployment (AWS / GCP / Azure)

---

## 👨‍💻 Author

**Himaka Uthpala**
- GitHub: [@Himaka12](https://github.com/Himaka12)

---

## ⭐ Support

If you found this project helpful, consider giving it a ⭐ on [GitHub](https://github.com/Himaka12/Vehicle-Sales-Rental-System) — it means a lot!

---

<div align="center">
  <sub>Built with ❤️ using Spring Boot & MySQL</sub>
</div>
