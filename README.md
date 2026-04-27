# 🚗 Vehicle Sales & Rental System

A full-stack web application for managing vehicle sales and rentals with database integration and AI-based sentiment analysis for customer reviews.

---

## 📌 Overview

The **Vehicle Sales & Rental System** is designed to simplify vehicle business operations by providing a centralized platform to:

- Browse available vehicles  
- Manage vehicle listings  
- Handle vehicle sales and rentals  
- Store and display vehicle images  
- Analyze customer feedback using sentiment analysis  

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

## 🛠️ Tech Stack

- **Backend:** Java, Spring Boot  
- **Frontend:** HTML, CSS, JavaScript  
- **Database:** MySQL  
- **Build Tool:** Maven  

---

## 📁 Project Structure


Vehicle-Sales-Rental-System/
│
├── Vehicle/Vehicle/
│ ├── src/main/java/ # Backend source code
│ ├── src/main/resources/ # Config, templates, static files
│ ├── pom.xml # Maven configuration
│ └── Application.java # Main entry point
│
├── src/main/resources/static/uploads/ # Uploaded images
├── ShowProps.java
└── README.md


---

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/Himaka12/Vehicle-Sales-Rental-System.git
cd Vehicle-Sales-Rental-System
2. Open in IDE
Open IntelliJ IDEA (recommended)
Click Open → Select project folder
Wait until indexing completes

⚠️ Main project is inside: Vehicle/Vehicle/

3. Reload Maven
Open pom.xml
Right-click → Maven → Reload Project
4. Set Up MySQL Database

Open MySQL and run:

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
Locate Application.java
Right-click → Run

OR use terminal:

mvn spring-boot:run
7. Access the Application

Open your browser and go to:

http://localhost:8080
🧩 System Modules
Vehicle Management – Manage vehicle details and availability
Sales Management – Handle vehicle purchase records
Rental Management – Manage rental bookings
Customer Management – Store customer data
Review & Sentiment Analysis – Analyze user feedback
🖼️ Image Storage

Uploaded images are stored in:

src/main/resources/static/uploads/
⚠️ Common Issues

Maven dependencies not loading
→ Reload Maven project

Database connection error
→ Check username, password, and database name

Port already in use
→ Change port in application.properties:

server.port=8081

Images not displaying
→ Ensure uploads folder exists

🔮 Future Improvements
User authentication (login/signup)
Admin dashboard
Payment integration
Advanced search filters
AI-based recommendations
Cloud deployment
👨‍💻 Author

Himaka Uthpala
https://github.com/Himaka12

⭐ Support

If you found this project useful, consider giving it a ⭐ on GitHub.
