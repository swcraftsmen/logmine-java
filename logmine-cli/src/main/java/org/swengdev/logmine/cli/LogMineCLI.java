package org.swengdev.logmine.cli;

import org.fusesource.jansi.AnsiConsole;
import org.swengdev.logmine.LogMineConfig;
import org.swengdev.logmine.LogMineProcessor;
import org.swengdev.logmine.LogPattern;
import org.swengdev.logmine.strategy.CustomVariableDetector;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * LogMine CLI - A log pattern analyzer command-line tool.
 *
 * <p>Analyzes log files and identifies common patterns using unsupervised clustering.
 *
 * <p>Usage examples:
 *
 * <pre>
 * # Analyze from stdin
 * cat myapp.log | logmine
 *
 * # Analyze files
 * logmine application.log
 *
 * # Adjust clustering granularity
 * logmine -m 0.3 application.log
 *
 * # Define custom variables
 * logmine -v "<time>:/\\d{2}:\\d{2}:\\d{2}/" application.log
 * </pre>
 */
@Command(
    name = "logmine",
    version = "LogMine CLI 1.0.0",
    description = "A log pattern analyzer - identifies common patterns in log files",
    mixinStandardHelpOptions = true,
    sortOptions = false)
public class LogMineCLI implements Callable<Integer> {

  @Parameters(
      index = "0..*",
      paramLabel = "FILE",
      description = "Log files to analyze. If not specified, reads from stdin.")
  private List<Path> files = new ArrayList<>();

  @Option(
      names = {"-m", "--max-dist"},
      paramLabel = "THRESHOLD",
      description =
          "Similarity threshold for clustering (0.0-1.0). Lower values create more granular"
              + " clusters. Default: ${DEFAULT-VALUE}")
  private double maxDist = 0.6;

  @Option(
      names = {"-v", "--variables"},
      paramLabel = "VAR",
      description =
          "Define variables to normalize before clustering. Format: 'name:/regex/'. "
              + "Example: '<time>:/\\d{2}:\\d{2}:\\d{2}/' to replace all timestamps with <time>")
  private List<String> variables = new ArrayList<>();

  @Option(
      names = {"-i", "--min-members"},
      paramLabel = "COUNT",
      description = "Minimum number of log messages in a cluster to display. Default:"
          + " ${DEFAULT-VALUE}")
  private int minMembers = 2;

  @Option(
      names = {"-p", "--pattern-placeholder"},
      paramLabel = "TEXT",
      description = "Use custom placeholder text for variable parts in patterns. Default: <*>")
  private String patternPlaceholder = null;

  @Option(
      names = {"-s", "--sorted"},
      paramLabel = "ORDER",
      description = "Sort clusters by size: 'desc' (default) or 'asc'")
  private String sorted = "desc";

  @Option(
      names = {"--no-color"},
      description = "Disable colored output")
  private boolean noColor = false;

  @Option(
      names = {"--json"},
      description = "Output results in JSON format")
  private boolean jsonOutput = false;

  @Option(
      names = {"--max-patterns"},
      paramLabel = "COUNT",
      description = "Maximum number of patterns to display. Default: unlimited")
  private int maxPatterns = Integer.MAX_VALUE;

  @Option(
      names = {"--verbose"},
      description = "Show detailed processing information")
  private boolean verbose = false;

  private OutputFormatter formatter;

  public static void main(String[] args) {
    // Enable ANSI colors on Windows
    AnsiConsole.systemInstall();

    new CommandLine(new LogMineCLI()).execute(args);

    AnsiConsole.systemUninstall();
    
    // Note: We don't call System.exit() to avoid restricted method warnings in modern Java
    // The application will exit naturally when main() completes
    // For proper exit code propagation in production, consider using picocli's
    // IExitCodeExceptionMapper or build this as a native image with proper exit handling
  }

  @Override
  public Integer call() throws Exception {
    try {
      // Initialize formatter
      formatter = new OutputFormatter(!noColor, patternPlaceholder);

      if (verbose) {
        formatter.printInfo("LogMine CLI starting...");
        formatter.printInfo("Similarity threshold: " + maxDist);
        formatter.printInfo("Min members: " + minMembers);
      }

      // Read log messages
      List<String> logMessages = readInput();

      if (logMessages.isEmpty()) {
        formatter.printError("No log messages to analyze");
        return 1;
      }

      if (verbose) {
        formatter.printInfo("Read " + logMessages.size() + " log messages");
      }

      // Build configuration
      LogMineConfig config = buildConfig();

      // Create processor with config
      LogMineProcessor processor = new LogMineProcessor(config);

      if (verbose) {
        formatter.printInfo("Processing logs...");
      }

      // Process logs
      List<LogPattern> patterns = processor.process(logMessages);

      // Filter by min members
      patterns =
          patterns.stream()
              .filter(p -> p.getSupportCount() >= minMembers)
              .collect(Collectors.toList());

      // Sort patterns
      if ("asc".equalsIgnoreCase(sorted)) {
        patterns.sort(Comparator.comparingInt(LogPattern::getSupportCount));
      } else {
        patterns.sort(Comparator.comparingInt(LogPattern::getSupportCount).reversed());
      }

      // Limit patterns
      if (patterns.size() > maxPatterns) {
        patterns = patterns.subList(0, maxPatterns);
      }

      // Output results
      if (jsonOutput) {
        outputJson(patterns, logMessages.size());
      } else {
        outputText(patterns, logMessages.size());
      }

      return 0;

    } catch (Exception e) {
      if (formatter != null) {
        formatter.printError("Error: " + e.getMessage());
      } else {
        System.err.println("Error: " + e.getMessage());
      }

      if (verbose) {
        e.printStackTrace();
      }

      return 1;
    }
  }

  private List<String> readInput() throws IOException {
    List<String> messages = new ArrayList<>();

    if (files.isEmpty()) {
      // Read from stdin
      if (verbose && formatter != null) {
        formatter.printInfo("Reading from stdin...");
      }

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (!line.isBlank()) {
            messages.add(line);
          }
        }
      }
    } else {
      // Read from files
      for (Path file : files) {
        if (verbose && formatter != null) {
          formatter.printInfo("Reading file: " + file);
        }

        try (Stream<String> lines = Files.lines(file)) {
          lines.filter(line -> !line.isBlank()).forEach(messages::add);
        }
      }
    }

    return messages;
  }

  private LogMineConfig buildConfig() {
    LogMineConfig.Builder builder = LogMineConfig.builder().similarityThreshold(maxDist);

    // Parse and add custom variables
    if (!variables.isEmpty()) {
      CustomVariableDetector.Builder detectorBuilder = new CustomVariableDetector.Builder();
      
      for (String varDef : variables) {
        String[] parts = varDef.split(":", 2);
        if (parts.length == 2) {
          // parts[0] is the variable name (currently not used by CustomVariableDetector)
          String regex = parts[1];

          // Remove surrounding slashes if present
          if (regex.startsWith("/") && regex.endsWith("/")) {
            regex = regex.substring(1, regex.length() - 1);
          }

          detectorBuilder.addVariablePattern(Pattern.compile(regex));
        }
      }

      builder.withVariableDetector(detectorBuilder.build());
    }

    return builder.build();
  }

  private void outputText(List<LogPattern> patterns, int totalMessages) {
    formatter.printHeader("LogMine Analysis Results");
    formatter.printInfo("Total messages: " + totalMessages);
    formatter.printInfo("Clusters found: " + patterns.size());
    formatter.println();

    int clusterNum = 1;
    for (LogPattern pattern : patterns) {
      formatter.printCluster(clusterNum++, pattern);
      formatter.println();
    }

    // Summary
    int clusteredCount = patterns.stream().mapToInt(LogPattern::getSupportCount).sum();
    int unclustered = totalMessages - clusteredCount;

    formatter.printSummary(clusteredCount, unclustered, totalMessages);
  }

  private void outputJson(List<LogPattern> patterns, int totalMessages) {
    // Simple JSON output without external dependencies
    StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append("  \"total_messages\": ").append(totalMessages).append(",\n");
    json.append("  \"clusters_found\": ").append(patterns.size()).append(",\n");
    json.append("  \"patterns\": [\n");

    for (int i = 0; i < patterns.size(); i++) {
      LogPattern pattern = patterns.get(i);
      json.append("    {\n");
      json.append("      \"pattern_id\": \"").append(escapeJson(pattern.getPatternId())).append("\",\n");
      json.append("      \"pattern\": \"")
          .append(escapeJson(String.join(" ", pattern.getTokens())))
          .append("\",\n");
      json.append("      \"count\": ").append(pattern.getSupportCount()).append(",\n");
      json.append("      \"percentage\": ")
          .append(String.format("%.2f", (pattern.getSupportCount() * 100.0 / totalMessages)))
          .append("\n");
      json.append("    }");
      if (i < patterns.size() - 1) {
        json.append(",");
      }
      json.append("\n");
    }

    json.append("  ]\n");
    json.append("}\n");

    System.out.println(json);
  }

  private String escapeJson(String str) {
    return str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}

