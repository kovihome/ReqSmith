# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [[Unreleased]](https://github.com/kovihome/ReqSmith)
### Added
- New ReqM element definition: style
- StdRepo: Style feature

### Modified

### Fixed

---

## [[0.3.0] - 2025-03-24](https://github.com/kovihome/ReqSmith/releases/tag/0.3.0)
### Added
- New ReqM element definitions: class, entity
- StdRepo: Persistent feature
- Template view
- New view layout elements: form, datatable, linkButton, linkGroup, spacer, text
- View events
- Code Generation: Three-layer source architecture
- Forge command line options: --compile and --run

### Modified
- Cache .reqm file parsing
- Spring plugin manages persistency with Spring Data and H2 embedded database
- File-based @Template feature was renamed @Resource feature


## [[0.2.0] - 2025-01-16](https://github.com/kovihome/ReqSmith/releases/tag/0.2.0)
### Added
- Plugin manager
- New ReqM language element definitions: view, feature
- StdRepo: Web framework
- StdRepo: Web application type
- StdRepo: Template feature
- Language generator: html
- Build script (gradle) dynamic plugin and dependencies management
- Html generator implementation: Bootstrap
- Web application generator implementation: Spring Boot
### Modified
- Application name changed to `forge`

---

## [[0.1.0] - 2024-11-30](https://github.com/kovihome/ReqSmith/releases/tag/0.1.0-2)
### Added
- ReqM language basics elements (application, module, action) are defined in ANTLR4 format
- Build system plugin: gradle
- Language generator: kotlin
- StdRepo: Command line application type
- StdRepo: Base framework
- ST4 string templates
- Manage default values
- Project init option

