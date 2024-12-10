# **ReqSmith**
[![Release](https://img.shields.io/github/v/release/kovihome/ReqSmith)](https://github.com/kovihome/ReqSmith/releases/tag/0.1.0-2)
[![Java 17+](https://img.shields.io/badge/java-17+-4c7e9f.svg)](http://java.oracle.com)
[![License](https://img.shields.io/github/license/kovihome/ReqSmith)](https://github.com/kovihome/ReqSmith/blob/main/LICENSE)

### **A developement system to build applications from requirements**

**ReqSmith** is a developer tool that enables users to:
1. **Model user requirements** that describe the behavior and functionality of an application.
2. **Automatically generate source code** based on the requirements.
3. **Leverage pre-built building blocks**, which are modular components that include software design patterns, best practices and predefined solutions, along with requirements and generators.

> **ℹ️ Information:** ReqSmith is in its **proof-of-concept state**. While we've taken the first steps toward building a revolutionary application development tool, **it’s not fully functional yet**. This is just the beginning of a journey to create a platform that generates complete, high-quality source code based on user requirements for any programming language or framework. \
> \
> To achieve the full potential of ReqSmith, we need **your help**! Here's how you can contribute:
> 
> - **Donate**: Help support development and bring ReqSmith closer to a fully functional version. Every contribution, big or small, helps us move faster. \
> 
> - **Contribute**: Join the development effort by contributing code, plugins, modules, or documentation. Check out the Contributing Guide for details.

---

## **Features**

- **Requirement Modeling**:  
  ReqSmith provides an intuitive language that allows users to easily define and document application requirements.

- **Source Code Generation**:  
  The tool automatically generates essential source code for applications based on the requirement models.

- **Modular Building Blocks**:  
  ReqSmith uses predefined modules that:
    - Include software design patterns.
    - Provide functionalities for application generation.
    - Are customizable and extensible.

- **Flexible Generators**:  
  ReqSmith supports custom generators, allowing users to create their own functionalities.

---

## Useful information

- Getting started with ReqSmith
- [Documentation (on wiki pages)](https://github.com/kovihome/ReqSmith/wiki)
- Release notes
- FAQ
- For developers

---

## **Use Cases**

### 1. **Modeling Requirements**
ReqSmith allows you to define simple or complex requirements in an easily readable format. For an example of a simple model of "Hello World!" application, see [Example](#example) section below.

### 2. **Automatic Code Generation**
Based on the requirements, ReqSmith can generate:
- Data structures for entities (e.g., classes or interfaces).
- Implementations for predefined actions.

### 3. **Using Building Blocks**
The modular approach enables integration of design patterns.

These patterns adapt automatically to the requirement models.

---

## **Installation**

1. Clone the repository:
   ```bash
   git clone https://github.com/kovihome/ReqSmith.git
   cd ReqSmith
   ```

2. Run the Gradle build:
   ```bash
   ./gradlew build
   ```

3. Start the application:
   ```bash
   ./gradlew run
   ```
   or install it from the distribution package

   ```bash
   unzip ./app/build/distributions/composer-0.1.0.zip -d any-folder-you-want
   ```
---

## **How to Use**

1. **Create a new ReqM project**:

   Create a project structure and a minimal requirement model file.

    ```bash
    mkdir MyProjectFolder
    cd MyProjectFolder
    compose --init -p MyProject
    ```

2. **Define a Requirement Model**:  
   Write down your project requirements in ReqM format.


3. **Generate Code**:  
   Based on the requirements, ReqSmith creates the required source code with the appropriate modules for the required language and build system.

    ```bash
   compose -l kotlin -b gradle
    ```

4. **Build your application**:
   Build the application with the generated build script.

    ```bash
   ./gradlew build
    ```

5. **Customization**:  
   Extend the predefined modules or create your own modules and generators.

---

## **Example**

### Input Requirement Model:
```plaintext
application HelloWorld from applications.CommandLineApplication {
    description: 'Hello World application'
    version: 1.0.0
}

action start {
    print 'Hello World!'
}
```

### Generated Source Code:
```kotlin
package dev.reqsmith.sample

class HelloWorld {
    fun start() {
        println ("Hello World!")
    }
}

fun main(args: Array<String>) {
    val helloworld = HelloWorld()
    helloworld.start ()
}
```

---

## **Additional Features**

- **Extensibility**:  
  Add new modules and generators easily for your own projects.

- **Integration**:  
  ReqSmith can be integrated seamlessly with existing build tools, such as Gradle or Maven.

- **CI/CD Support**:  
  The application can be configured to work in automated environments, such as Jenkins or GitHub Actions.

---

## **Contribution**

We welcome community contributions! If you want to improve ReqSmith:
1. Fork this repository.
2. Create a branch for your feature:
   ```bash
   git checkout -b feature/your-feature
   ```
3. Submit a pull request with your changes.

---

## **License**

This project is licensed under the [GNU GPL V3 License](./LICENSE). See the license file for details.

---

## **Contact**

If you have any questions, feel free to contact us at:
- **Email**: [kovihome86@gmail.com](mailto:kovihome86@gmail.com)
- **GitHub Issues**: [Issues Page](https://github.com/kovihome/ReqSmith/issues)
