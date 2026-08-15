# chpg-notebook

[![Launch Binder](https://mybinder.org/badge_logo.svg)](https://mybinder.org/v2/gh/chpg-dev/chpg-notebook/main) [![Launch Binder Lab](https://mybinder.org/badge_logo.svg)](https://mybinder.org/v2/gh/chpg-dev/chpg-notebook/main?urlpath=lab)

A [Binder](https://mybinder.org/) compatible repository to try out the chpg-notebook environment with just a web browser. Depending on if the docker image is already built, spawning the notebook may take some time and so please be patient with it.

Support for Jupyter Notebook 

## Prerequisites
- Git
- Docker
- Docker Compose

## Setup
Clone the repository with its submodules:
```bash
git clone --recurse-submodules git@github.com:chpg-dev/chpg-notebook.git
```
If you have already cloned the repository without submodules, you can initialize them with:
```bash
git submodule update --init --recursive
```

## Build and Run
Build and start the Jupyter Notebook environment using Docker Compose:
```bash
docker compose up --build
```
*(Note: If you are using an older version of Docker, you might need to use `docker-compose up --build` instead.)*

This will:
1. Build the Docker image with Java, Python, Node.js, JupyterLab, and the IJava kernel.
2. Build the `pg` engine (Java) and `pgv` visualizer (TypeScript) submodules inside the container.
3. Start the JupyterLab server on port 8888.

## Testing and Access
To access the notebook, look at the terminal output after running the docker command. You will see a URL that looks like:
```
http://127.0.0.1:8888/lab?token=<your_token>
```
Open this link in your browser to access the JupyterLab environment and test the notebooks.

## Uploading Data

You can upload `.dgb` (Direct Graph Buffer) format files to the `data` directory using the Jupyter Notebook upload button.

Note that DGB files can be exported from Atlas (e.g., the Xinu operating system) using [chpg-atlas](https://github.com/benjholla/chpg-atlas).
