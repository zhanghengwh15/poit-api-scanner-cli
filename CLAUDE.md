# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Install plugin to local Maven repository
mvn clean install -DskipTests

# Build standalone fat JAR (for CLI usage outside Maven)
mvn -Pstandalone clean package -DskipTests
```

## Project Overview

This is a Maven plugin (`maven-poit-doc-plugin`) that parses Java source code using [Smart-doc](https://github.com/smart-doc-group/smart-doc) and synchronizes API documentation to MySQL 8. It extracts Controller endpoints and model definitions from Spring-based projects, storing them in a structured database format.

## Architecture

```
SyncDocMojo (Maven entry point)
    ↓
SmartDocBootstrap (Smart-doc engine integration)
    ↓
ApiDocSupport (Tree flattening & model extraction)
    ↓
DocSyncService (Database upsert)
```

**Key Components:**

- **SyncDocMojo**: Maven Mojo with `@Mojo(name="sync")`, binds to `COMPILE` phase. Requires `requiresDependencyResolution=COMPILE` to access project's compile classpath for type resolution.
- **SmartDocBootstrap**: Wraps Smart-doc's `ApiConfig` and `ApiDataBuilder` to parse source code and produce `ApiDoc` trees.
- **ApiDocSupport**: Flattens nested `ApiDoc` trees into controller-level docs. Extracts model fields as flat JSON with `ref` references (avoiding circular references in storage).
- **DocSyncService**: Batch upserts interfaces and models to MySQL using `ON DUPLICATE KEY UPDATE`.
- **StandaloneSyncMain**: CLI entry point for running outside Maven context (useful for CI/CD or ad-hoc sync).

## Database Schema

Two tables defined in `src/main/resources/schema.sql`:

- **api_interface**: Stores endpoint metadata (path, method, controller, request/response model refs)
- **api_model_definition**: Stores class field definitions as JSON, with `ref` pointing to other model full class names

## Model Reference Pattern

To handle circular references and nested objects, models are stored with references instead of inline expansion:

```json
// Example: UserDTO fields stored in api_model_definition.fields
[
  {"name": "id", "type": "long", "desc": "User ID"},
  {"name": "role", "type": "object", "ref": "com.example.RoleDTO"}
]
```

Frontend loads models recursively by following `ref` values.

## Configuration Properties

Maven plugin parameters use `poit.doc.*` property prefix for command-line overrides:

```bash
mvn my-doc-plugin:sync -Dpoit.doc.serviceName=demo -Dpoit.doc.env=prod
```

## Plugin Goal Prefix

Configured as `my-doc-plugin` in `maven-plugin-plugin` configuration. Use `mvn my-doc-plugin:sync` or full coordinates.