FROM eclipse-temurin:17-jdk-jammy

# Install base dependencies
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    python3-venv \
    curl \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js (LTS, version 20)
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs && \
    npm install -g pnpm && \
    rm -rf /var/lib/apt/lists/*

# Set up Python virtual environment for Jupyter
ENV VIRTUAL_ENV=/opt/venv
RUN python3 -m venv $VIRTUAL_ENV
ENV PATH="$VIRTUAL_ENV/bin:$PATH"

# Install JupyterLab
RUN pip install --no-cache-dir jupyterlab

# Download and install Spencer Park's IJava kernel
# Note: Since IJava requires Python to be run to install the kernel, we use the venv
RUN curl -L https://github.com/SpencerPark/IJava/releases/download/v1.3.0/ijava-1.3.0.zip > ijava.zip && \
    unzip ijava.zip -d ijava-kernel && \
    cd ijava-kernel && \
    python3 install.py --sys-prefix && \
    cd .. && \
    rm -rf ijava.zip ijava-kernel

# Set up the working directory
WORKDIR /app

# The entrypoint script will be copied via docker-compose volume or added here
# In this structure, we'll copy it directly so the image can be self-contained
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

# Expose Jupyter port
EXPOSE 8888

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
