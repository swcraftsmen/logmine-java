#!/bin/bash
# Quick script to run LogMine benchmarks

set -e

cd "$(dirname "$0")/.."

echo "Building benchmarks..."
./gradlew :logmine-benchmarks:jmhJar --quiet

echo ""
echo "Running benchmarks (this will take ~10-15 minutes)..."
echo "Results will be saved to: logmine-benchmarks/build/reports/jmh/"
echo ""

# Run the benchmark
./gradlew :logmine-benchmarks:jmh

echo ""
echo "✅ Benchmarks complete!"
echo "View results at: logmine-benchmarks/build/reports/jmh/results.json"



