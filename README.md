# **ReqSmith**
[![Release](https://img.shields.io/github/v/release/kovihome/ReqSmith)](https://github.com/kovihome/ReqSmith/releases/tag/0.1.0-2)
[![Java 17+](https://img.shields.io/badge/java-17+-4c7e9f.svg)](http://java.oracle.com)
[![License](https://img.shields.io/github/license/kovihome/ReqSmith)](https://github.com/kovihome/ReqSmith/blob/main/LICENSE)

### A developement system to build applications from requirements

**ReqSmith** is a developer tool that enables users to:
1. **Model user requirements** that describe the behavior and functionality of an application.
2. **Automatically generate source code** based on the requirements.
3. **Leverage pre-built building blocks**, which are modular components that include software design patterns, best practices and predefined solutions, along with requirements and generators.

| **ℹ️ Important Notice:**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ReqSmith is in its **proof-of-concept state**.<br>While we've taken the first steps toward building a revolutionary application development tool, **it’s not fully functional yet**. This is just the beginning of a journey to create a platform that generates complete, high-quality source code based on user requirements for any programming language or framework.<br><br>To achieve the full potential of ReqSmith, we need **your help**! Here's how you can contribute:<br><ul><li>**Donate**: Help [support development](#support-this-project) and bring ReqSmith closer to a fully functional version. Every contribution, big or small, helps us move faster.</li><li>**Contribute**: Join the development effort by contributing code, plugins, modules, or documentation. Check out the [Contributing Guide](#Contribution) for details.</li></ul> |
  
## Features

- **Separating application planning and implementation**:  
  ReqSmith separates application planning and implementation by using a structured, model-driven approach. 
  Instead of writing code directly, developers first define system requirements using the ReqM modeling language, 
  then ReqSmith compose the requirement model with building blocks from the ReqSmith Repository into a complete the model.   
  This model is still independent of any programming language or framework.\
  \
  Once the effective requirement model is created, ReqSmith’s **Generator engine** interprets the model and generates source code using **generator plugins**. 
  These plugins handle the translation from abstract requirements into concrete implementations for different languages and frameworks. 
  This approach ensures that planning remains **technology-agnostic**, while implementation is handled flexibly based on the chosen tech stack.


- **Convention over Configuration**:  
  In the context of ReqSmith, convention over configuration means that the system relies on well-defined defaults and standardized modeling practices to reduce the need for manual setup and customization. 
  Instead of requiring developers to configure every detail explicitly, ReqSmith automates code generation based on conventional requirement structures. 


- **Requirement modeling**:  
  ReqSmith provides an intuitive, easy-to-learn language (ReqM) that allows users to easily define and document application requirements.
  Requirements can be incomplete or partially defined.


- **Requirement composing**:  
  To complete requirements ReqSmith gather missing information from its extended repository, 
  and compose them into an effective requirement model, which is the basis of the code generation.


- **Modular Building Blocks**:  
  ReqSmith repository contains modular building blocks for fulfillment of the requirements.
  These building blocks represent software design patters, best practices or user experiences, 
  and are defined in requirement model too.
  

- **Source Code Generation**:  
  The tool automatically generates essential source code for applications based on the requirement model. 
  The source code can be generated for any of popular programming language and frameworks. 


- **Flexible Generators**:  
  ReqSmith supports custom generators, allowing users to create their own functionalities.


- **Extensibility via plugins**:  
  Add new modules and generators easily for your own projects.


## Useful information

- [Installation](https://github.com/kovihome/ReqSmith/wiki/Getting-Started#step-1-installing-reqsmith)
- [Getting started with ReqSmith](https://github.com/kovihome/ReqSmith/wiki/Getting-Started)
- [Documentation (on wiki pages)](https://github.com/kovihome/ReqSmith/wiki)
- [Release notes](./CHANGELOG)
- FAQ
- For developers

## Contribution

We welcome any type of community contributions, including:
- Opinions, critics, useful comments
- Testing ReqSmith capabilities
- Create more realistic examples
- Write application code, especially plugins

If you want to help ReqSmith development, please visit [Contribution page](https://github.com/kovihome/ReqSmith/wiki/Contribution) for details.

## Support This Project

If you find this project useful, please consider supporting it:

<!--
- [GitHub Sponsors](https://github.com/sponsors/yourusername)
- [Patreon](https://www.patreon.com/yourproject)
- [Open Collective](https://opencollective.com/yourproject)
- [Liberapay]()
- [Buy me a coffee](https://buymeacoffee.com)
-->
- **[PayPal](https://paypal.me/kovihome?country.x=HU&locale.x=hu_HU)**

## License

This project is licensed under the [GNU GPL V3 License](./LICENSE). See the license file for details.

## Contact

If you have any questions, feel free to contact us at:
- **Email**: [kovihome86@gmail.com](mailto:kovihome86@gmail.com)
- **GitHub Issues**: [Issues Page](https://github.com/kovihome/ReqSmith/issues)
