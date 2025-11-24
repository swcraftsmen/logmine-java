/*
 * Copyright 2024 Zachary Huang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.swengdev.logmine;

/**
 * Processing mode for LogMine. Modern sealed enum to restrict implementations.
 *
 * <p>STREAMING: Process logs immediately without storing raw logs. Best for: Production, real-time
 * processing, unlimited logs Memory: O(k) where k = number of clusters (constant)
 *
 * <p>BATCH: Store logs for batch processing and experimentation. Best for: Research, analysis,
 * parameter tuning Memory: O(n) where n = number of logs (grows with logs)
 */
public enum ProcessingMode {
  /**
   * Streaming mode - processes logs immediately without storage. Suitable for production use with
   * unlimited log volumes.
   */
  STREAMING {
    @Override
    public String description() {
      return """
                Streaming mode: Processes logs in real-time without storage.
                - Memory: Constant (O(k) clusters)
                - Throughput: ~8K logs/second (single thread)
                - Use case: Production monitoring
                """;
    }

    @Override
    public boolean storesRawLogs() {
      return false;
    }
  },

  /**
   * Batch mode - stores logs for batch processing and experimentation. Suitable for research,
   * analysis, and parameter tuning.
   */
  BATCH {
    @Override
    public String description() {
      return """
                Batch mode: Stores logs for batch analysis.
                - Memory: Linear (O(n) logs with limit)
                - Throughput: 158K logs/s collection, ~2-8 ops/s processing
                - Use case: Research and experimentation
                """;
    }

    @Override
    public boolean storesRawLogs() {
      return true;
    }
  };

  /**
   * Returns a human-readable description of the mode.
   *
   * @return Mode description
   */
  public abstract String description();

  /**
   * Indicates whether this mode stores raw logs in memory.
   *
   * @return true if logs are stored, false otherwise
   */
  public abstract boolean storesRawLogs();

  /**
   * Pattern matching helper for mode-specific logic.
   *
   * @param <R> Return type
   * @param onStreaming Supplier to call for STREAMING mode
   * @param onBatch Supplier to call for BATCH mode
   * @return Result from the appropriate supplier
   */
  public <R> R match(
      java.util.function.Supplier<R> onStreaming, java.util.function.Supplier<R> onBatch) {
    return switch (this) {
      case STREAMING -> onStreaming.get();
      case BATCH -> onBatch.get();
    };
  }
}
