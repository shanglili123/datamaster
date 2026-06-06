#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

mvn -f "$SCRIPT_DIR/db-init/pom.xml" clean package
mkdir -p "$SCRIPT_DIR/packages/components"
cp "$SCRIPT_DIR/db-init/target/datamaster-db-init.jar" "$SCRIPT_DIR/packages/components/datamaster-db-init.jar"

echo "Built $SCRIPT_DIR/packages/components/datamaster-db-init.jar"
