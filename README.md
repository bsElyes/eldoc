# elDoc

## Overview

**elDoc** is an automated documentation generator for Java projects. It analyzes source code, extracts class and package
information, and produces rich Markdown documentation, including Mermaid diagrams and package summaries. elDoc also
generates prompts ready to use with AI models, enabling teams to create high-quality, maintainable documentation with
minimal effort.

## Features

- **Automatic Markdown Generation:** Extracts Javadoc, method signatures, dependencies, and class relationships.
- **Package Structure Awareness:** Generates documentation files in folders matching Java package structure.
- **Package Summaries:** Creates `package-summary.md` for each package, listing direct children and subpackages.
- **Mermaid Diagrams:** Visualizes package and project structure for easy navigation.
- **Global Project Diagram:** Produces a top-level diagram of all packages and their relationships.
- **AI-Ready Prompts:** Generates prompts for use with AI models to further enrich documentation.

## Project Structure

```
elDoc/
├── build.gradle
├── src/
│   └── main/
│       ├── java/
│       │   └── tn/
│       │       └── eternity/
│       │           └── eldocs/
│       │               └── DocsGenerator.java
│       └── resources/
└── ...
```

## Usage

### Prerequisites

- Java 17+
- Gradle

### Build

```bash
cd elDoc
./gradlew build
```

### Run Documentation Generation

```bash
./gradlew run --args="--sourceDir ../core/src/main/java --outputDir ../eternity-docs/docs"
```

- `--sourceDir`: Path to the source code to document.
- `--outputDir`: Path where documentation will be generated.

### Output

- Markdown files for each class/interface, organized by package.
- `package-summary.md` in each package folder.
- `project-package-diagram.md` at the root of the output directory.
- AI-ready prompts for further documentation enrichment.

## Example

After running elDoc, your documentation folder will contain:

```
docs/
├── tn/
│   └── eternity/
│       └── model/
│           ├── Asset.md
│           ├── package-summary.md
│           └── ...
├── project-package-diagram.md
└── ...
```

## Contributing

1. Fork the repository and create your branch.
2. Follow code style and documentation standards.
3. Submit a pull request with a clear description.

## License

This project is licensed under the MIT License.

## Contact

- Issues: [GitHub Issues](https://github.com/eternity-platform/elDoc/issues)
- Email: support@eternity-platform.com
