
---

```markdown
# 🚗 Vehicle Sales & Rental System

A full-stack web application for managing vehicle sales and rentals with database integration, image handling, and AI-based sentiment analysis for user reviews. This system allows users to browse vehicles, manage listings, handle rentals and sales, and analyze customer feedback.

---

## 📁 File Structure

```

Vehicle-Sales-Rental-System/
├── Vehicle/
│   └── Vehicle/
│       ├── src/
│       │   └── main/
│       │       ├── java/                  # Controllers, Services, Models
│       │       └── resources/
│       │           ├── static/            # CSS, JS, uploads
│       │           └── templates/         # HTML pages
│       ├── pom.xml                       # Maven dependencies
│       └── Application.java              # Main Spring Boot application
│
├── src/
│   └── main/
│       └── resources/
│           └── static/
│               └── uploads/              # Uploaded vehicle images
│
├── ShowProps.java
├── README.md
└── .idea/

````

---

## 📌 Project Overview

The **Vehicle Sales & Rental System** is designed to digitalize vehicle business operations by providing a centralized platform for:

- Managing vehicle listings
- Handling vehicle sales
- Managing rental bookings
- Storing customer information
- Uploading and displaying vehicle images
- Analyzing customer reviews using AI

This project demonstrates a complete full-stack workflow using Java Spring Boot, MySQL, and web technologies.

---

## ✨ Features

- Vehicle listing management
- Vehicle sales handling
- Vehicle rental management
- Image upload and storage system
- Customer review handling
- AI-based sentiment analysis for reviews
- MySQL database integration
- Web-based user interface

---

## 🧠 AI Functionality

The system includes sentiment analysis capabilities for customer reviews.

This helps to:
- Identify positive and negative feedback
- Improve business decisions
- Understand customer satisfaction trends

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java, Spring Boot |
| Frontend | HTML, CSS, JavaScript |
| Database | MySQL |
| Build Tool | Maven |
| AI Feature | Sentiment Analysis |
| IDE | IntelliJ IDEA / Eclipse |

---

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/Himaka12/Vehicle-Sales-Rental-System.git
cd Vehicle-Sales-Rental-System
````

---

### 2. Open Project in IDE

* Open IntelliJ IDEA
* Click **Open**
* Select the project folder
* Wait for indexing

> Important: The main project is inside `Vehicle/Vehicle/`

---

### 3. Reload Maven Dependencies

* Open `pom.xml`
* Right-click → **Maven → Reload Project**

OR use Maven panel → refresh icon

---

### 4. Set Up MySQL Database

Open MySQL and run:

```sql
CREATE DATABASE vehicle_sales_rental_system;
```

---

### 5. Configure Database Connection

Open:

```
src/main/resources/application.properties
```

Update:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/vehicle_sales_rental_system
spring.datasource.username=root
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

---

### 6. Connect Database in IDE (Optional)

In IntelliJ:

* Go to **Database**
* Add new MySQL connection
* Enter:

  * Host: localhost
  * Port: 3306
  * Username: root
  * Password: your password
  * Database: vehicle_sales_rental_system

---

### 7. Run the Application

* Locate main Spring Boot class (`Application.java`)
* Right-click → **Run**

OR use terminal:

```bash
mvn spring-boot:run
```

---

### 8. Open the Web Application

Open your browser:

```
http://localhost:8080
```

---

## 🧪 How the System Works

```
User → Frontend (HTML/CSS/JS)
     → Backend (Spring Boot)
     → MySQL Database
     → Response back to UI
```

---

## 📦 Modules

### Vehicle Module

Handles vehicle details, pricing, availability, and images.

### Sales Module

Manages vehicle sales records and transactions.

### Rental Module

Handles rental bookings and schedules.

### Customer Module

Stores and manages customer information.

### Review Module

Handles user reviews and sentiment analysis.

---

## 🖼️ Image Upload System

Images are stored in:

```
src/main/resources/static/uploads/
```

Ensure this folder exists when running the project.

---

## ⚠️ Common Issues

### Maven not loading

* Reload Maven project

### Database connection error

* Check username/password
* Ensure MySQL is running

### Port already in use

Change port in `application.properties`:

```properties
server.port=8081
```

### Images not showing

* Check uploads folder
* Restart application

---

## 🔮 Future Improvements

* User authentication system
* Admin dashboard
* Payment integration
* Advanced search filters
* AI recommendation system
* Cloud deployment

---

## 👨‍💻 Author

**Himaka Uthpala**
GitHub: [https://github.com/Himaka12](https://github.com/Himaka12)

---

## ⭐ Support

If you found this project useful, consider giving it a star ⭐ on GitHub.

```


