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

RUN ( \
  set -x; cd "$(mktemp -d)" && \
  OS="$(uname | tr '[:upper:]' '[:lower:]')" && \
  ARCH="$(uname -m | sed -e 's/x86_64/amd64/' -e 's/\(arm\)\(64\)\?.*/\1\2/' -e 's/aarch64$/arm64/')" && \
  KREW="krew-${OS}_${ARCH}" && \
  curl -fsSLO "https://github.com/kubernetes-sigs/krew/releases/latest/download/${KREW}.tar.gz" && \
  tar zxvf "${KREW}.tar.gz" && \
  ./"${KREW}" install krew \
)
RUN echo 'export PATH="$HOME/.krew/bin:$PATH"' >> ~/.bashrc

RUN PATH="$HOME/.krew/bin:$PATH" kubectl krew update
RUN PATH="$HOME/.krew/bin:$PATH" kubectl krew install tunnel

RUN curl -L https://github.com/bazelbuild/buildtools/releases/download/6.0.0/buildifier-linux-amd64 -o /usr/local/bin/buildifier && \
    chmod +x /usr/local/bin/buildifier

RUN curl -L https://github.com/bazelbuild/buildtools/releases/download/6.0.0/buildozer-linux-amd64 -o /usr/local/bin/buildozer && \
    chmod +x /usr/local/bin/buildozer
