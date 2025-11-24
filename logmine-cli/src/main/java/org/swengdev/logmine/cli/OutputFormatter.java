package org.swengdev.logmine.cli;

import org.fusesource.jansi.Ansi;
import org.swengdev.logmine.LogPattern;

import java.util.List;

import static org.fusesource.jansi.Ansi.Color.*;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * Formats and colorizes CLI output for better readability.
 *
 * <p>Provides ANSI colored terminal output for log patterns, highlighting variables and patterns.
 */
public class OutputFormatter {

  private final boolean colorEnabled;
  private final String patternPlaceholder;

  public OutputFormatter(boolean colorEnabled, String patternPlaceholder) {
    this.colorEnabled = colorEnabled;
    this.patternPlaceholder = patternPlaceholder != null ? patternPlaceholder : null;
  }

  public void printHeader(String title) {
    if (colorEnabled) {
      System.out.println(ansi().bold().fg(CYAN).a("═".repeat(60)).reset());
      System.out.println(ansi().bold().fg(CYAN).a("  " + title).reset());
      System.out.println(ansi().bold().fg(CYAN).a("═".repeat(60)).reset());
    } else {
      System.out.println("=" .repeat(60));
      System.out.println("  " + title);
      System.out.println("=".repeat(60));
    }
  }

  public void printInfo(String message) {
    if (colorEnabled) {
      System.out.println(ansi().fg(BLUE).a("ℹ " + message).reset());
    } else {
      System.out.println("[INFO] " + message);
    }
  }

  public void printError(String message) {
    if (colorEnabled) {
      System.err.println(ansi().bold().fg(RED).a("✗ " + message).reset());
    } else {
      System.err.println("[ERROR] " + message);
    }
  }

  public void printCluster(int clusterNum, LogPattern pattern) {
    double percentage = 0.0;
    // Note: We don't have total count here, so we'll skip percentage for now
    // or pass it separately if needed

    // Cluster header
    if (colorEnabled) {
      System.out.print(ansi().bold().fg(YELLOW).a("Cluster #" + clusterNum).reset());
      System.out.print(ansi().fg(WHITE).a(" │ ").reset());
      System.out.print(ansi().bold().fg(GREEN).a(pattern.getSupportCount() + " messages").reset());
      System.out.println();
    } else {
      System.out.println("Cluster #" + clusterNum + " | " + pattern.getSupportCount() + " messages");
    }

    // Pattern
    System.out.print("  Pattern: ");
    printPattern(pattern.getTokens());
    System.out.println();

    // Pattern ID
    if (colorEnabled) {
      System.out.println(
          ansi()
              .fg(CYAN)
              .a("  ID: ")
              .reset()
              .a(pattern.getShortPatternId())
              .reset());
    } else {
      System.out.println("  ID: " + pattern.getShortPatternId());
    }
  }

  private void printPattern(List<String> tokens) {
    for (int i = 0; i < tokens.size(); i++) {
      String token = tokens.get(i);

      if (isVariable(token)) {
        // Highlight variables
        if (patternPlaceholder != null) {
          // Use custom placeholder
          if (colorEnabled) {
            System.out.print(ansi().bold().fg(RED).a(patternPlaceholder).reset());
          } else {
            System.out.print(patternPlaceholder);
          }
        } else {
          // Show actual variable
          if (colorEnabled) {
            System.out.print(ansi().bold().fg(RED).a(token).reset());
          } else {
            System.out.print(token);
          }
        }
      } else {
        // Normal token
        if (colorEnabled) {
          System.out.print(ansi().fg(WHITE).a(token).reset());
        } else {
          System.out.print(token);
        }
      }

      // Add space between tokens
      if (i < tokens.size() - 1) {
        System.out.print(" ");
      }
    }
  }

  private boolean isVariable(String token) {
    // Check if token is a variable (surrounded by angle brackets)
    return token.startsWith("<") && token.endsWith(">");
  }

  public void printSummary(int clusteredCount, int unclustered, int total) {
    if (colorEnabled) {
      System.out.println(ansi().bold().fg(CYAN).a("─".repeat(60)).reset());
      System.out.println(ansi().bold().fg(CYAN).a("  Summary").reset());
      System.out.println(ansi().bold().fg(CYAN).a("─".repeat(60)).reset());
    } else {
      System.out.println("-".repeat(60));
      System.out.println("  Summary");
      System.out.println("-".repeat(60));
    }

    double clusteredPercent = (clusteredCount * 100.0 / total);
    double unclusteredPercent = (unclustered * 100.0 / total);

    if (colorEnabled) {
      System.out.printf(
          "  %s: %s (%s)\n",
          ansi().a("Clustered").reset(),
          ansi().bold().fg(GREEN).a(clusteredCount).reset(),
          ansi().fg(GREEN).a(String.format("%.1f%%", clusteredPercent)).reset());

      if (unclustered > 0) {
        System.out.printf(
            "  %s: %s (%s)\n",
            ansi().a("Unclustered").reset(),
            ansi().bold().fg(YELLOW).a(unclustered).reset(),
            ansi().fg(YELLOW).a(String.format("%.1f%%", unclusteredPercent)).reset());
      }

      System.out.printf(
          "  %s: %s\n",
          ansi().a("Total").reset(), ansi().bold().a(total).reset());
    } else {
      System.out.printf("  Clustered: %d (%.1f%%)\n", clusteredCount, clusteredPercent);

      if (unclustered > 0) {
        System.out.printf("  Unclustered: %d (%.1f%%)\n", unclustered, unclusteredPercent);
      }

      System.out.printf("  Total: %d\n", total);
    }
  }

  public void println() {
    System.out.println();
  }

  public void println(String message) {
    System.out.println(message);
  }

  /**
   * Prints a progress bar for long-running operations.
   *
   * @param current Current progress value
   * @param total Total value
   * @param label Label to display
   */
  public void printProgress(int current, int total, String label) {
    if (!colorEnabled) {
      return; // Skip progress in non-color mode
    }

    int barWidth = 40;
    double progress = (double) current / total;
    int filled = (int) (barWidth * progress);

    Ansi ansi = ansi();
    ansi.saveCursorPosition();
    ansi.a("\r"); // Carriage return to overwrite line

    // Label
    ansi.fg(CYAN).a(label).reset().a(": ");

    // Progress bar
    ansi.a("[");
    ansi.fg(GREEN).a("=".repeat(Math.max(0, filled)));
    ansi.a(" ".repeat(Math.max(0, barWidth - filled)));
    ansi.reset().a("]");

    // Percentage
    ansi.a(String.format(" %.1f%% ", progress * 100));

    // Count
    ansi.fg(WHITE).a(String.format("(%d/%d)", current, total)).reset();

    System.out.print(ansi);
    System.out.flush();

    if (current >= total) {
      System.out.println(); // New line when complete
    }
  }
}

