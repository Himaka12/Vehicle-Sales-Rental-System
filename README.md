

# 🚗 Vehicle Sales & Rental System

A full-stack web application for managing vehicle sales and rentals with database integration, image handling, and AI-based sentiment analysis for user reviews. This system allows users to browse vehicles, manage listings, handle rentals and sales, and analyze customer feedback.

---

## 📁 File Structure


Vehicle-Sales-Rental-System/
├── Vehicle/
│ └── Vehicle/
│ ├── src/
│ │ └── main/
│ │ ├── java/
│ │ └── resources/
│ │ ├── static/
│ │ │ └── uploads/
│ │ └── templates/
│ ├── pom.xml
│ └── Application.java
│
├── src/
│ └── main/
│ └── resources/
│ └── static/
│ └── uploads/
│
├── ShowProps.java
├── README.md
└── .idea/


---

## 📌 Project Overview

The **Vehicle Sales & Rental System** is a web-based platform designed to manage vehicle-related business operations such as selling, renting, and maintaining vehicle records.

It provides a centralized system where users can:
- Browse vehicles
- Manage vehicle listings
- Handle sales and rentals
- Upload vehicle images
- Analyze customer feedback using AI

---

## ✨ Features

- Vehicle listing management
- Vehicle sales system
- Vehicle rental system
- Image upload and storage
- Customer review handling
- AI-based sentiment analysis
- MySQL database integration
- Web-based user interface

---

## 🧠 AI Functionality

The system includes sentiment analysis for user reviews.

This helps to:
- Detect positive and negative feedback
- Understand customer satisfaction
- Improve business decisions

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java, Spring Boot |
| Frontend | HTML, CSS, JavaScript |
| Database | MySQL |
| Build Tool | Maven |
| AI Feature | Sentiment Analysis |

---

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/Himaka12/Vehicle-Sales-Rental-System.git
cd Vehicle-Sales-Rental-System
2. Open Project in IDE
Open IntelliJ IDEA
Click Open
Select the project folder
Wait for indexing

Important: Main project is inside Vehicle/Vehicle/

3. Reload Maven
Open pom.xml
Right-click → Maven → Reload Project
4. Set Up MySQL Database
CREATE DATABASE vehicle_sales_rental_system;
5. Configure Database

Open:

src/main/resources/application.properties

Update:

spring.datasource.url=jdbc:mysql://localhost:3306/vehicle_sales_rental_system
spring.datasource.username=root
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
6. Run the Application
Find Application.java
Right-click → Run

OR:

mvn spring-boot:run
7. Open in Browser
http://localhost:8080
🧪 System Flow
User → Frontend → Backend → Database → Response
📦 Modules
Vehicle Management
Sales Management
Rental Management
Customer Management
Review & Sentiment Analysis
🖼️ Image Uploads

Images are stored in:

src/main/resources/static/uploads/
⚠️ Common Issues

Maven not loading
→ Reload Maven

Database connection error
→ Check username/password

Port already in use

server.port=8081

Images not showing
→ Check uploads folder

🔮 Future Improvements
Authentication system
Admin dashboard
Payment integration
AI recommendations
Cloud deployment
👨‍💻 Author

Himaka Uthpala
https://github.com/Himaka12

⭐ Support

If you like this project, give it a star ⭐
