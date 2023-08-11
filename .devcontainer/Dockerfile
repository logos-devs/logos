FROM mcr.microsoft.com/devcontainers/base:ubuntu

RUN apt update
RUN apt upgrade -y

RUN apt install -y command-not-found clang docker.io npm openjdk-17-jdk-headless openjdk-17-jre-headless postgresql-client software-properties-common sqitch libdbd-pg-perl
RUN npm install -g @bazel/bazelisk

RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && \
    unzip awscliv2.zip && \
    ./aws/install

RUN curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key add
RUN apt-add-repository -y "deb http://apt.kubernetes.io/ kubernetes-xenial main"
RUN apt install -y kubectl

RUN curl -L https://github.com/bazelbuild/buildtools/releases/download/6.0.0/buildifier-linux-amd64 -o /usr/local/bin/buildifier && \
    chmod +x /usr/local/bin/buildifier

RUN curl -L https://github.com/bazelbuild/buildtools/releases/download/6.0.0/buildozer-linux-amd64 -o /usr/local/bin/buildozer && \
    chmod +x /usr/local/bin/buildozer