1. **Overview and Features**: Files that describe the purpose of the project, its functionality, and user roles (e.g., admin, customer, employee).
2. **Architecture**: Files that define the structure of the project, such as folder organization, servlets, and utility classes.
3. **Technology Stack**: Files that specify the technologies and libraries used, including versions (e.g., `pom.xml`, `web.xml`, and library imports).
4. **Use Case Diagram**: Files that describe user interactions and workflows.
5. **Class Diagram**: Java files that define the main classes, their relationships, and methods.

I will now search for files and code snippets that provide this information. To create the requested documentation, I will extract relevant information from the provided files and attachments. Here's the breakdown of the documentation:

---

### **Overview**
JaBa Banking is a web-based banking application designed to provide banking services to customers, employees, and administrators. It supports functionalities like account management, fund transfers, loan applications, and transaction tracking.

---

### **Features**
1. **Customer Portal**:
   - View account details and balances.
   - Transfer funds between accounts.
   - Apply for loans and track loan status.
   - View transaction history.

2. **Employee Portal**:
   - Manage flagged transactions.
   - Provide chat support to customers.
   - Review and flag suspicious transactions.

3. **Admin Portal**:
   - Approve or reject loan applications.
   - Manage user accounts and roles.
   - View system statistics and transaction data.

---

### **Architecture**
- **Frontend**: JSP pages for dynamic content rendering, JavaScript for interactivity.
- **Backend**: Java Servlets for business logic and API endpoints.
- **Database**: SQLite for data persistence.
- **Utilities**: Shared utility classes for database operations, authentication, and data formatting.

**Key Components**:
- **Controllers**: Handle HTTP requests and responses (e.g., `ApproveLoanServlet`, `TransactionListServlet`).
- **Models**: Represent data entities (e.g., `User`, `Transaction`).
- **Utilities**: Provide helper functions (e.g., `DatabaseUtil` for database connections).
- **Frontend**: JSP pages and JavaScript for user interaction.

---

### **Technologies Stack**
1. **Frontend**:
   - HTML5, CSS3, JavaScript (ES6+).
   - Libraries: Chart.js for graphs.
   - JSP (Java Server Pages) for dynamic content.

2. **Backend**:
   - Java Servlets (Jakarta EE).
   - SQLite (version 3.49.1.0) for database management.

3. **Utilities**:
   - JSON library (`json-20231013.jar`) for JSON handling.

4. **Build and Deployment**:
   - Apache Tomcat for servlet hosting.

---

### **Use Case Diagram**
```mermaid
graph TD
    Customer -->|Login| Dashboard
    Customer -->|Apply| Loan
    Customer -->|Transfer| Funds
    Customer -->|View| Transactions
    Employee -->|Flag| Transactions
    Employee -->|Support| Chat
    Admin -->|Approve/Reject| Loan Applications
    Admin -->|Manage| Users
    Admin -->|View| System Statistics
```

---

### **Class Diagram**
```mermaid
classDiagram
    class User {
        +int id
        +String name
        +String email
        +String role
        +String status
    }
    class Transaction {
        +int id
        +double amount
        +String type
        +String description
        +Date timestamp
    }
    class DatabaseUtil {
        +Connection getConnection()
        +boolean registerUser(User, String)
        +boolean approveLoan(int, int)
    }
    class ApproveLoanServlet {
        +doPost(HttpServletRequest, HttpServletResponse)
    }
    class TransactionListServlet {
        +doGet(HttpServletRequest, HttpServletResponse)
    }
    User --> Transaction
    ApproveLoanServlet --> DatabaseUtil
    TransactionListServlet --> DatabaseUtil
```

---

This documentation provides a concise overview of the project, its features, architecture, technology stack, and diagrams for use cases and class relationships.# JaBa Banking Documentation

## Overview
JaBa Banking is a secure web-based banking application that provides comprehensive financial services through a multi-role user system. The platform supports customer banking operations, employee administrative tasks, and administrative system management.

## Features
- **User Management**: Multi-role system (admin, employee, customer) with secure authentication
- **Account Management**: View and manage checking and savings accounts 
- **Transaction Processing**: Deposits, withdrawals, transfers with detailed history
- **Loan System**: Apply for loans, manage loan applications, approve/reject loans
- **Admin Dashboard**: User management, system statistics, loan approval, system configuration
- **Employee Portal**: Transaction monitoring, flagging suspicious transactions, customer support
- **Customer Dashboard**: Account overview, fund transfers, loan applications, settings
- **Security Features**: Password hashing, session management, transaction authentication

## Architecture
The application follows a Model-View-Controller (MVC) architecture:
- **Models**: Java classes representing business entities (User, Transaction, etc.)
- **Views**: JSP pages with HTML/CSS/JavaScript for user interfaces
- **Controllers**: Java servlets handling HTTP requests and business logic
- **Data Access**: DatabaseUtil class providing database operations
- **Utils**: Helper classes for password management, JSON handling, etc.

## Technology Stack
- **Backend**: Java EE/Jakarta EE 9+
- **Web**: JSP (JavaServer Pages), Servlets 5.0
- **Database**: SQLite 3.36.0
- **Frontend**: HTML5, CSS3, JavaScript ES6
- **Security**: Custom password hashing with salting (SHA-256)
- **Visualization**: Chart.js 3.7.0 for transaction graphs
- **Build System**: Standard Java build system

## Usecase Diagram

```mermaid
graph TD
    %% Actors
    Customer((Customer))
    Employee((Employee))
    Admin((Admin))
    
    %% Customer Usecases
    UC1[View Accounts]
    UC2[Transfer Funds]
    UC3[Apply for Loan]
    UC4[View Transactions]
    UC5[Manage Settings]
    
    %% Employee Usecases
    UC6[View Customer Transactions]
    UC7[Flag Suspicious Transactions]
    UC8[Provide Customer Support]
    
    %% Admin Usecases
    UC9[Manage Users]
    UC10[Approve/Reject Loans]
    UC11[Configure System]
    UC12[View Statistics]
    
    %% Relationships
    Customer -->|performs| UC1
    Customer -->|performs| UC2
    Customer -->|performs| UC3
    Customer -->|performs| UC4
    Customer -->|performs| UC5
    
    Employee -->|performs| UC6
    Employee -->|performs| UC7
    Employee -->|performs| UC8
    
    Admin -->|performs| UC9
    Admin -->|performs| UC10
    Admin -->|performs| UC11
    Admin -->|performs| UC12
    Admin -->|can also perform| UC6
    Admin -->|can also perform| UC7
```

## Class Diagram

```mermaid
classDiagram
    %% Main Classes
    User <|-- Admin
    User <|-- Employee
    User <|-- Customer
    Customer "1" -- "*" Account
    Account "1" -- "*" Transaction
    Customer "1" -- "*" Loan
    
    class User {
        +int id
        +String name
        +String email
        +String passwordHash
        +String salt
        +String role
        +String status
        +Date createdAt
    }
    
    class Account {
        +int id
        +int userId
        +String accountNumber
        +String type
        +double balance
        +Date createdAt
    }
    
    class Transaction {
        +int id
        +int fromAccountId
        +int toAccountId
        +String type
        +double amount
        +String description
        +Date timestamp
    }
    
    class Loan {
        +int loanId
        +int userId
        +double amount
        +String type
        +Date date
        +String status
        +String adminComment
    }
    
    class DatabaseUtil {
        +getConnection()
        +registerUser()
        +authenticate()
        +getUserLoansAsJson()
        +flagTransaction()
        +approveLoan()
    }
    
    class PasswordUtil {
        +generateSalt()
        +hashPassword()
        +verifyPassword()
    }
```

This documentation provides a concise overview of the JaBa Banking system's structure, functionality, and technical implementation.******