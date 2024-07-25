# Avant propos

Ce tuto sera principalement orienté pour une installation linux. Bash sera utilisé tout au long mais des équivalents existe sur windows (`mingw-w64` (installé directment grâce à git sous le nom de `git-bash`)) et mac.

# Installation du projet.

Il y a deux façons d'installer le projet, soit manuellement soit par docker. Dans tous les cas il faut d'abord télécharger le projet.

## Télécharger le projet

3 façons possible de le télécharger 
- Soit avec [git](https://git-scm.com/downloads) par HTTPS (pas besoin de configuration)
- Soit avec [git](https://git-scm.com/downloads) par SSH (besoin de configurer git et github)
- Soit par [gh-cli](https://cli.github.com/).

Selon votre version choisissez la version qui vous convient.

<details>
  <summary>Git HTTPS</summary>
  
  Pour pouvoir télécharger le projet et ses sous-modules, vous devez taper cette commande:
  
  ```bash
    git clone --recurse-submodules -j8 https://github.com/PRIAM-solution/PRIAM.git
  ```
  
</details>

<details>
  <summary>Git SSH</summary>
  
  Pour pouvoir télécharger le projet et ses sous-modules, vous devez taper cette commande:
  
  ```bash
    git clone --recurse-submodules -j8 git@github.com:PRIAM-solution/PRIAM.git
  ```
  
</details>

<details>
  <summary>GH-cli</summary>
  
  Pour pouvoir télécharger le projet et ses sous-modules, vous devez taper cette commande:
  
  ```bash
    gh repo clone PRIAM-solution/PRIAM -- --recurse-submodules -j8
  ```
  
</details>

Une fois fait, vous pouvez passer à l'étape suivante.

- [Installation par docker](./INSTALL_DOCKER.md)
- [Installation native](./INSTALL_NATIVE.md)

