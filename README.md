# LogMine - Fast Pattern Recognition for Log Analytics

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Build](https://img.shields.io/badge/build-passing-brightgreen.svg)]()

**Unsupervised log pattern extraction library and tools for real-time log analytics.**

LogMine automatically discovers patterns in log data without predefined templates, making it perfect for monitoring unknown or changing log formats.

---

## Project Modules

This is a multi-module project with the following components:

### 🔧 [logmine-core](logmine-core/) - Core Library

The main LogMine library for pattern extraction and log analysis.

**Features:**
- ✅ Unsupervised pattern extraction
- ✅ Real-time streaming mode
- ✅ Zero external dependencies
- ✅ Thread-safe processing
- ✅ STREAMING: 8K logs/s; BATCH: 158K logs/s collection

**[→ View Core Library Documentation](logmine-core/README.md)**

---

###  [logmine-cli](logmine-cli/) - Command-Line Tool

A powerful CLI for analyzing log files from the terminal.

```bash
# Analyze logs
cat application.log | logmine-cli
logmine-cli -m 0.4 application.log

# With custom variables
logmine-cli \
  -v '<time>:/\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}/' \
  -v '<ip>:/\d+\.\d+\.\d+\.\d+/' \
  application.log
```

**Features:**
- ✅ Stdin/file input
- ✅ Colorful ANSI output
- ✅ JSON export
- ✅ Custom variable definitions
- ✅ Flexible clustering options

**[→ View CLI Documentation](logmine-cli/README.md)**

---

## 🚀 Quick Start

### Option 1: Use the CLI Tool (Easiest)

```bash
# Build
./gradlew :logmine-cli:installDist

# Run
cat /var/log/application.log | ./logmine-cli/build/install/logmine-cli/bin/logmine-cli
```

### Option 2: Use as Library

> **Note:** Not yet published to Maven Central. For now, build from source and use `publishToMavenLocal`.

**Build and Install Locally:**
```bash
./gradlew :logmine-core:publishToMavenLocal
```

**Then add to your project:**

**Gradle:**
```kotlin
repositories {
    mavenLocal()
}

dependencies {
    implementation("org.swengdev.logmine:logmine-core:unspecified")
}
```

**Maven:**
```xml
<dependency>
    <groupId>org.swengdev.logmine</groupId>
    <artifactId>logmine-core</artifactId>
    <version>unspecified</version>
</dependency>
```

**Code:**
```java
import org.swengdev.logmine.*;

// Create and use
LogMine logMine = new LogMine();
logMine.addLog("GET /api/users/123 HTTP/1.1 200 45ms");
logMine.addLog("GET /api/users/456 HTTP/1.1 200 67ms");

List<LogPattern> patterns = logMine.getCurrentPatterns();
// Pattern: GET * HTTP/1.1 * *
```

---

## Building from Source

### Build All Modules

```bash
git clone https://github.com/swcraftsmen/logmine
cd logmine
./gradlew build
```

### Build Individual Modules

```bash
# Core library
./gradlew :logmine-core:build

# CLI tool
./gradlew :logmine-cli:build

# Benchmarks
./gradlew :logmine-benchmarks:jmhJar
```

### Run Tests

```bash
# All tests
./gradlew test

# Specific module
./gradlew :logmine-core:test
```

---

## Project Structure

```
logmine-project/
├── logmine-core/        # Core library
│   ├── src/             # Source code
│   ├── docs/            # Core documentation
│   ├── README.md        # Library documentation
│   └── build.gradle.kts
│
├── logmine-cli/         # Command-line tool
│   ├── src/             # CLI source code
│   ├── README.md        # CLI documentation
│   └── build.gradle.kts
│
├── logmine-benchmarks/  # JMH performance benchmarks
│   ├── src/jmh/         # Benchmark code
│   ├── README.md        # Benchmark documentation
│   └── build.gradle.kts
│
├── build.gradle.kts     # Root build file
├── settings.gradle.kts  # Multi-module configuration
├── VERSIONING.md        # Versioning guide
├── LICENSE              # Apache License 2.0
└── CITATION.cff         # Citation information
```

---

## 🎯 Key Features

### Core Library
- ✅ **Unsupervised Learning** - No templates needed
- ✅ **Real-Time Processing** - Streaming mode for unlimited logs
- ✅ **Zero Dependencies** - No external libraries required
- ✅ **Thread-Safe** - Concurrent processing support
- ✅ **Stable Pattern IDs** - Content-based hashing for storage/correlation
- ✅ **Performance** - STREAMING: 8K logs/s; BATCH: 158K logs/s collection

### CLI Tool
- ✅ **Easy to Use** - Simple command-line interface
- ✅ **Colorful Output** - ANSI colored terminal display
- ✅ **Flexible Input** - Stdin pipes or file arguments
- ✅ **JSON Export** - Machine-readable output
- ✅ **Customizable** - Variables, thresholds, placeholders

---

## 📚 Documentation

### Core Documentation
- **[Core Library README](logmine-core/README.md)** - Full library documentation
- **[Understanding the Algorithm](logmine-core/docs/UNDERSTANDING_THE_ALGORITHM.md)** - How LogMine works
- **[Configuration Guide](logmine-core/docs/CONFIGURATION_GUIDE.md)** - Configuration options
- **[Multi-Format Support](logmine-core/docs/MULTI_FORMAT_SUPPORT.md)** - Handling different log formats
- **[Hierarchical Patterns](logmine-core/docs/HIERARCHICAL_PATTERNS.md)** - Multi-level pattern extraction

### Tools & Benchmarks
- **[CLI Documentation](logmine-cli/README.md)** - Command-line tool guide
- **[Benchmark Guide](logmine-benchmarks/BENCHMARKS.md)** - Performance benchmarks

### Project Information
- **[License](LICENSE)** - Apache License 2.0
- **[Citation](CITATION.md)** - How to cite this project

---

## Research Paper

LogMine is based on the research paper:

**Hamooni, H., Debnath, B., Xu, J., Zhang, H., Jiang, G., & Mueen, A. (2016).**  
*LogMine: Fast Pattern Recognition for Log Analytics.*  
In Proceedings of the 25th ACM International Conference on Information and Knowledge Management (CIKM '16).  
DOI: [10.1145/2983323.2983358](https://doi.org/10.1145/2983323.2983358)

**Paper:** [PDF](https://www.cs.unm.edu/~mueen/Papers/LogMine.pdf)

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## License

Copyright 2024 Zachary Huang

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

See the [LICENSE](LICENSE) file for full details.

---

## 🔗 Links

- **GitHub**: https://github.com/swcraftsmen/logmine
- **Website**: https://swengdev.org
- **Issues**: https://github.com/swcraftsmen/logmine/issues
- **Discussions**: https://github.com/swcraftsmen/logmine/discussions

---

##  Contact

- **Author**: Zachary Huang
- **GitHub**: [@swcraftsmen](https://github.com/swcraftsmen)

---

**Happy Log Mining!** 🪵⛏️
