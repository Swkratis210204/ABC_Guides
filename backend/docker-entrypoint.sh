#!/bin/sh
# A freshly mounted volume is owned by root. Give the data folder to the app user, then drop root and start the app.
set -e
mkdir -p "$DATA_DIR"
chown -R app:app "$DATA_DIR" 2>/dev/null || true
exec su-exec app java -jar /app/app.jar
