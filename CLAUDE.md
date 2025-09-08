# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot application that provides a REST API for running JUnit tests and collecting code coverage data via JaCoCo. The application serves as an MCP (Model Context Protocol) server for JUnit test execution.

### Key Architecture Components

- **REST Controller**: `JUnitMcpController` handles POST requests to `/mcp/junit` for test execution
- **Test Execution**: Uses JUnit Platform Launcher to dynamically run tests based on discovery selectors
- **Coverage Analysis**: Integrates JaCoCo for code coverage collection and analysis
- **DTO Layer**: Request/Response objects for API communication and internal data transfer

The application accepts test specifications (class names or method names) in `RunRequest`, executes them using JUnit Platform, and returns both test results and coverage data in `RunResponse`.

## Build and Development Commands

```bash
# Build the project
mvn clean compile

# Run tests
mvn test

# Run tests with coverage report
mvn clean test

# Build and package
mvn clean package

# Run the Spring Boot application
mvn spring-boot:run

# Run application directly (after packaging)
java -jar target/Junit-MCP-0.0.1-SNAPSHOT.jar
```

The application runs on port 8090 (configured in application.properties).

## Testing and Coverage

- JUnit 5 (Jupiter) is the primary testing framework
- JUnit 4 (Vintage) support is available for legacy tests
- JaCoCo generates coverage reports in `target/jacoco.exec`
- Coverage analysis requires compiled classes in `target/classes`
- Test selectors support both class-level (`com.example.TestClass`) and method-level (`com.example.TestClass#testMethod`) execution

## Key Dependencies

- Spring Boot 3.5.5 with Web and Data REST starters
- JUnit Platform Launcher 6.0.0-RC2
- JaCoCo 0.8.11 for coverage analysis
- Lombok for reducing boilerplate code
- Jackson for JSON processing

## API Usage

The main endpoint `/mcp/junit` accepts POST requests with:
- `tests`: Array of test identifiers (class names or class#method)

Returns:
- `results`: Array of test execution results with status, duration, and error details
- `coverage`: Map of class coverage data with line coverage metrics