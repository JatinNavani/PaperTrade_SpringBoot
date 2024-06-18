# PaperTrade_SpringBoot


Welcome to the Paper Trading Project! This project simulates a trading environment where users can practice trading without risking real money. It consists of two main components:

1. **Backend**: Implemented using Spring Boot, this component handles the core trading logic, user management, and data persistence.
2. **Frontend**: Implemented using Android Studio, this component provides a mobile interface for users to interact with the trading system.

This repository contains the backend component of the Paper Trading Project, implemented using Spring Boot. The backend handles the core trading logic, user management, and data persistence.


## Repository Structure

- **Backend (Spring Boot)**: [[Link to Spring Boot Repository](https://github.com/JatinNavani/PaperTrade_SpringBoot)](#)
- **Frontend (Android Studio)**: [[Link to Android Studio Repository](https://github.com/JatinNavani/PaperTrading)](#)

  
## Prerequisites

- Java 17 or higher
- Maven

## Configuration and Setup

1.Configure RabbitMQ

  Ensure RabbitMQ is installed and running. Update the application properties (application.properties or application.yml) with RabbitMQ configuration details.

  For more details : https://medium.com/@jatinnavani/getting-started-with-rabbitmq-on-aws-ec2-a-step-by-step-guide-8a0db9c40d3a

# RabbitMQ Config
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

Example configuration in application.properties:


2.# Create a Kite Developers account

Visit Kite Developers and create an account. Obtain API credentials (API key and API secret key) from your Kite Developer account dashboard.

Configure API credentials

Update the application properties with your Kite API credentials (API key, API secret key, and username) according to the comments in the code.



3.Get a CSV file of all stocks
Now give the path to store instruments in fetchAndStoreInstruments method in KiteService class.

4.Build the project using Maven.

mvn clean install


5.Run the Application

Run the Spring Boot application.

mvn spring-boot:run
