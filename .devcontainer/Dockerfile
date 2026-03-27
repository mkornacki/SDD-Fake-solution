# Use the official Java base image with the longest LTS support (Java 21)
FROM mcr.microsoft.com/devcontainers/java:11  

# Install Python, pip, and curl (required for uv and Specify CLI)
RUN apt-get update && apt-get install -y python3 python3-pip curl && rm -rf /var/lib/apt/lists/*

USER vscode
# Install uv
RUN curl -LsSf https://astral.sh/uv/install.sh | sh
# Install specify-cli from GitHub (uv 0.11.0 syntax)
ENV PATH="/home/vscode/.local/bin:${PATH}"
RUN uv tool install specify-cli --from git+https://github.com/github/spec-kit.git@v0.4.1
