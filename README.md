### 🌿 BranchOne  
### A Modern Multi-Company & Multi-Branch Inventory Management System  
**Built with Java + Spring Boot + Vaadin Flow**

BranchOne is a **full-stack Java enterprise application** for managing **inventory, sales, and user operations** across **multiple companies and branches**.  
It leverages the robustness of **Spring Boot** and the modern UI capabilities of **Vaadin Flow** to deliver a seamless, secure, and scalable experience — all in **pure Java** (no JavaScript required).

## 🚀 Key Features

### 🏢 Multi-Company & Multi-Branch Hierarchy  
- **Super Admins** onboard companies and assign **Company Admins**.  
- **Company Admins** manage branches and onboard **Branch Managers** and **Branch Users**.  
- Enables clear **data separation**, **secure access**, and **hierarchical visibility**.

### 🔐 Role-Based Access Control (RBAC)  
- Roles include *Super Admin*, *Company Admin*, *Branch Manager*, and *Branch User*.  
- Fine-grained access management ensures users only see what’s relevant to their role.  
- Vaadin-based dashboards are **context-aware** and **securely rendered** per user level.

### ⚙️ Technical Highlights  
- **Pure Java Development:** Write both backend and frontend logic in Java—no JavaScript needed.  
- **Spring Boot Integration:** Modular, secure, and easy to scale with Spring Data JPA and Spring Security.  
- **Vaadin Flow UI:** Build dynamic, responsive interfaces using Vaadin’s powerful component library.  
- **Standalone Deployment:** Run as a self-contained JAR—no external server setup required.  
- **Automated Frontend Build:** Vaadin frontend resources compile automatically during build.

🧩 Project Structure
├── src/
│   ├── main/
│   │   ├── java/               # Backend logic (services, controllers, etc.)
│   │   └── resources/          # Configuration files (application.properties, etc.)
│   └── frontend/               # Vaadin frontend components (TypeScript, HTML, etc.)
├── build.gradle                 # Gradle build configuration
├── settings.gradle              # Gradle project settings
└── application.properties      # Application settings

# ⚙️ Configuration
Before building, ensure the following settings are configured in src/main/resources/application.properties:
# Enable production mode for optimized frontend builds
vaadin.productionMode=true

# Set the server port (default: 8080)
server.port=8080

# 🛠️ Build Instructions

Clean and Build:
# Use the Gradle wrapper to compile the project and package it into a runnable JAR:
 gradlew clean vaadinBuildFrontend bootJar

This command:

Cleans old build files.
Compiles the Vaadin frontend for production.
Packages the application into a standalone JAR.

Run the Application:
Execute the JAR file to start the server:

java -jar build/libs/BranchOne-1.0-SNAPSHOT.jar


Access the Application:
Open your browser and navigate to:
http://localhost:8080

💡 Why BranchOne?

Simplified Development: Focus on Java without worrying about frontend frameworks or JavaScript.
Scalability: Spring Boot’s modular architecture makes it easy to scale as your project grows.
Modern UI: Vaadin Flow provides a rich set of UI components out of the box.
Production-Ready: Optimized builds and standalone deployment ensure smooth operation in any environment.

📌 Next Steps

Explore the Vaadin documentation for UI customization.
Extend the backend with additional Spring Boot services.
Contribute to the project or report issues on GitHub.

Built with ❤️ for Java developers by Java developers. 🚀
