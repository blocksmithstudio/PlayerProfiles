#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -B -ntp "$@" install
mvn -B -ntp "$@" -pl core dependency:build-classpath -Dmdep.outputFile=target/check-classpath.txt
java -ea -cp "core/target/classes:$(cat core/target/check-classpath.txt)" checks/BehaviorChecks.java
