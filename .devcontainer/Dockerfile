FROM mcr.microsoft.com/devcontainers/base:ubuntu

SHELL ["/bin/bash", "-euc"]

RUN apt-get update && apt-get upgrade -y

RUN apt-get install -y \
    amazon-ecr-credential-helper \
    command-not-found \
    clang \
    docker.io \
    openjdk-17-jdk-headless \
    openjdk-17-jre-headless \
    openssh-server \
    postgresql-client \
    python3.10-venv \
    software-properties-common \
    sqitch \
    libdbd-pg-perl

RUN wget https://github.com/bazelbuild/bazel/releases/download/7.0.1/bazel-7.0.1-installer-linux-x86_64.sh && \
    sha256sum -c <(echo "7abc509b7dfad887abb0edc9f9d529426d26768c7cce56d09252e2b0482b4135  bazel-7.0.1-installer-linux-x86_64.sh") && \
    chmod +x bazel-7.0.1-installer-linux-x86_64.sh && \
    ./bazel-7.0.1-installer-linux-x86_64.sh && \
    rm bazel-7.0.1-installer-linux-x86_64.sh && \
    echo "source /usr/local/lib/bazel/bin/bazel-complete.bash" >> ~/.bashrc

RUN wget https://github.com/bazelbuild/buildtools/releases/download/v6.3.2/buildifier-linux-amd64 && \
    sha256sum -c <(echo "b3d452cadba5f248309f487e5276d190fa0c8825ac7016c8184520fccc6986a1  buildifier-linux-amd64") && \
    mv buildifier-linux-amd64 /usr/local/bin/buildifier && \
    chmod +x /usr/local/bin/buildifier

RUN wget https://github.com/bazelbuild/buildtools/releases/download/v6.3.2/buildozer-linux-amd64 && \
    sha256sum -c <(echo "160ec5b239ce96876b4ced3afd4985bd0e1dead87e59a63a9faf39bd1d5caefc  buildozer-linux-amd64") && \
    mv buildozer-linux-amd64 /usr/local/bin/buildozer && \
    chmod +x /usr/local/bin/buildozer

RUN wget https://github.com/bazelbuild/bazel-watcher/releases/download/v0.23.7/ibazel_linux_amd64 && \
    sha256sum -c <(echo "bb388d71ea7bc6d771156238e4a2c07bc9b5f0aa9f29ffe6d3b56e1fc993e4b6  ibazel_linux_amd64") && \
    mv ibazel_linux_amd64 /usr/local/bin/ibazel && \
    chmod +x /usr/local/bin/ibazel

RUN wget https://app.getambassador.io/download/tel2oss/releases/download/v2.14.4/telepresence-linux-amd64 && \
    sha256sum -c <(echo "ad41a0a59aad86968a4e39734371e7750a2c6f67930769b0622eb9049af30b3a  telepresence-linux-amd64") && \
    mv telepresence-linux-amd64 /usr/local/bin/telepresence && \
    chmod +x /usr/local/bin/telepresence

RUN wget https://github.com/chainguard-dev/apko/releases/download/v0.13.2/apko_0.13.2_linux_amd64.tar.gz && \
    sha256sum -c <(echo "95e3f21e01538de4438d67d3c3c841bdcc24fa2eb7b4b1c95265c6d7bfd4dc9d  apko_0.13.2_linux_amd64.tar.gz") && \
    tar xzf apko_0.13.2_linux_amd64.tar.gz && \
    mv apko_0.13.2_linux_amd64/apko /usr/local/bin/ && \
    rm -rf apko_0.13.2_linux_amd64

RUN curl -LO https://dl.k8s.io/release/v1.30.0/bin/linux/amd64/kubectl && \
    install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && \
    unzip awscliv2.zip && \
    ./aws/install && \
    rm awscliv2.zip

RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.1/install.sh | bash && \
    export NVM_DIR="$HOME/.nvm" && \
    [ -s "$NVM_DIR/nvm.sh" ] && \
    source "$NVM_DIR/nvm.sh" && \
    nvm install lts/hydrogen && \
    npm install -g pnpm

RUN apt-get remove -y command-not-found && apt-get autoremove -y

RUN echo "export PS1='\u@\h:\w $ '" >> /root/.bashrc
