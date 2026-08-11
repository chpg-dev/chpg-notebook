#!/bin/bash
set -e

echo "Building pg..."
if [ -f "/app/pg/gradlew" ]; then
    cd /app/pg
    ./gradlew build -x test
    cd /app
else
    echo "Warning: /app/pg/gradlew not found. Did you initialize the submodules correctly?"
fi

echo "Building pgv..."
if [ -f "/app/pgv/package.json" ]; then
    cd /app/pgv
    pnpm install
    pnpm build
    cd /app
else
    echo "Warning: /app/pgv/package.json not found. Did you initialize the submodules correctly?"
fi

echo "Starting JupyterLab..."
exec jupyter lab --ip=0.0.0.0 --port=8888 --no-browser --allow-root
