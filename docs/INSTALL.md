# Preface

This tutorial will be primarily oriented towards a Linux installation. Bash will be used throughout, but equivalents exist for Windows (`mingw-w64` (installed directly via Git as `git-bash`)) and Mac.

# Project Installation

There are two ways to install the project: either manually or via Docker. In any case, the project must first be downloaded.

## Download the Project

There are three possible ways to download the project:

- With [git](https://git-scm.com/downloads) via HTTPS (no configuration needed).
- With [git](https://git-scm.com/downloads) via SSH (requires Git and GitHub configuration).
- With [gh-cli](https://cli.github.com/).

Choose the version that suits you according to your setup.

<details>
  <summary>Git HTTPS</summary>
  
  To download the project and its submodules, you need to run this command:
  
  ```bash
    git clone --recurse-submodules -j8 https://github.com/PRIAM-solution/PRIAM.git
  ```
  
</details>

<details>
  <summary>Git SSH</summary>
  
  To download the project and its submodules, you need to run this command:
  
  ```bash
    git clone --recurse-submodules -j8 git@github.com:PRIAM-solution/PRIAM.git
  ```
  
</details>

<details>
  <summary>GH-cli</summary>
  
  To download the project and its submodules, you need to run this command:
  
  ```bash
    gh repo clone PRIAM-solution/PRIAM -- --recurse-submodules -j8
  ```
  
</details>

Once done, you can proceed to the next step.

- [Docker Installation](./INSTALL_DOCKER.md)
- [Native Installation](./INSTALL_NATIVE.md)
