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

echo "Applying pgv-jupyter.patch..."
if [ -f "/app/pgv-jupyter.patch" ]; then
    cd /app/pgv
    git apply ../pgv-jupyter.patch || true
    cd /app
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


echo "Building pgv-exporter..."
if [ -f "/app/pgv/examples/pgv-exporter/pom.xml" ]; then
    cd /app/pgv/examples/pgv-exporter
    mvn package -DskipTests
    cd /app
fi

echo "Starting JupyterLab..."
exec jupyter lab --ip=0.0.0.0 --port=8888 --no-browser --allow-root
