FROM eclipse-temurin:17-jdk-jammy

# Install base dependencies
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    python3-venv \
    curl \
    unzip \
    git \
    maven \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js (LTS, version 20)
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs && \
    npm install -g pnpm && \
    rm -rf /var/lib/apt/lists/*

# Create jovyan user for Binder compatibility
ARG NB_USER=jovyan
ARG NB_UID=1000
ENV USER=${NB_USER}
ENV NB_UID=${NB_UID}
ENV HOME=/home/${NB_USER}

RUN adduser --disabled-password \
    --gecos "Default user" \
    --uid ${NB_UID} \
    ${NB_USER}

# Set up Python virtual environment for Jupyter
ENV VIRTUAL_ENV=/opt/venv
RUN python3 -m venv $VIRTUAL_ENV
RUN chown -R ${NB_UID} $VIRTUAL_ENV
ENV PATH="$VIRTUAL_ENV/bin:$PATH"

# Switch to jovyan user
USER ${NB_USER}
WORKDIR ${HOME}

# Install JupyterLab
RUN pip install --no-cache-dir jupyterlab

# Download and install Spencer Park's IJava kernel
# Note: Since IJava requires Python to be run to install the kernel, we use the venv
RUN curl -L https://github.com/SpencerPark/IJava/releases/download/v1.3.0/ijava-1.3.0.zip > ijava.zip && \
    unzip ijava.zip -d ijava-kernel && \
    cd ijava-kernel && \
    python3 install.py --user && \
    cd .. && \
    rm -rf ijava.zip ijava-kernel

# Expose Jupyter port
EXPOSE 8888
