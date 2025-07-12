Banking App Backend (Spring Boot)
This is the Spring Boot backend for the full-stack banking application. It offers RESTful APIs for account management, transactions, and secure JWT-based authentication.

Features:
✅ User Registration
🔑 Secure Login with JWT Token
💳 Deposit / Withdraw APIs
💸 Money Transfer API
👤 Account Details Management (email, address, state)
🧾 View Account Info (via token)
❌ Delete Account with Cleanup
📜 JWT Expiry and Validation Handling

Tech Stack:
Backend: Spring Boot, Java
Security: Spring Security with JWT
Database: MySQL (via Spring Data JPA)
API Testing: Postman 

Tools Used:
Postman – For testing all backend APIs (login, transfer, delete, etc.)
MySQL Workbench / CLI – To manage account and transaction data
IntelliJ IDEA – Development IDE for Spring Boot
VS Code – Development IDE for React frontend

Frontend Repository
👉 Check out the frontend repo built with React.js here:
https://github.com/Esther-Graceia-Precious/banking-frontend-react

Getting Started
To run the backend locally:
git clone https://github.com/Esther-Graceia-Precious/banking-backend-springboot.git
cd banking-app-backend

Update application.properties with your MySQL credentials.
Then run:
./mvnw spring-boot:run

Sample API Endpoints:
Endpoints marked with auth required need the Authorization: Bearer <token> header.
Method	   Endpoint	                   Description
POST	     /auth/login	               Login and receive JWT
POST	     /api/accounts	             Register a new account
POST	     /api/accounts/deposit	     Deposit funds (auth required)
POST	     /api/accounts/withdraw      Withdraw funds (auth required)
POST	     /api/accounts/transfer	     Transfer to another account
GET	       /api/accounts/view-account	 View user account details (auth required)
DELETE	   /api/accounts/delete	       Delete user account (auth required)
