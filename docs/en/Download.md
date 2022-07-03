# Download

GBC is developed based on Oracle JDK 8. It is available on any computer device that supports or is compatible with Oracle JDK 8. Users are required to download and install the [Oracle JDK](https://www.oracle.com/cn/java/technologies/javase-downloads.html) or [Open JDK](https://openjdk.java.net/install/) firstly. Apple Silicon devices can use the [zulu JDK](https://www.azul.com/downloads/?package=jdk#download-openjdk) as an alternative. In addition, we also provide Docker image for building the GBC runtime environment.

| Type      | URL                                |
| :--------------- | :------------------------------------- |
| Software | http://pmglab.top/gbc/download/gbc.jar |
| Source Code | https://github.com/Zhangliubin/gbc     |
| Online Manual | http://pmglab.top/gbc/                 |
| API docs | http://pmglab.top/gbc/api-docs/        |
| Example Data | http://pmglab.top/gbc/download/example.zip<br />http://pmglab.top/genotypes/#/ |

## Download software package by wget

```bash
# Download GBC software
wget http://pmglab.top/gbc/download/gbc.jar -O gbc.jar

# setup GBC
java -jar gbc.jar
```

## Use the GBC in Docker

```bash
# download dockerfile
wget http://pmglab.top/gbc/download/Dockerfile -O Dockerfile

# build image
docker build -t gbc .

# setup GBC
docker run -it --rm gbc
```

## Download GBC from Github

```bash
# download source code from github
git clone https://github.com/Zhangliubin/gbc gbc-source

# go to the folder
cd gbc-source

# setup GBC
java -jar gbc.jar
```

# Update GBC

GBC has now iterated to a stable release, and generally we only add new features to existing packages, or minor code architecture updates to enhance stability and improve performance. To check the availability of new releases, please use:

```bash
java -jar gbc.jar update
```

# System requirements

GBC has a strict memory requirement control and can usually be run at the default memory allocation for small genomic data. For large scale genomic data, the memory usage of GBC is limited to a maximum of 4 GB for a single thread, therefore, we recommend running GBC programs in a heap memory of no less than 4 GB at all times. The user allocates the GBC runtime heap memory with the following command:

```bash
java -Xms4g -Xmx4g -jar gbc.jar
```

When running GBC with Docker, we recommend using the following template command:

```bash
# Macos or Linux
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc [options]

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -e -m 4g gbc [options]
```

In this command, `-v` means to map the specified host path to the container path (`host_path:container_path`), `-w` means to set the current working path (equivalent to running the `cd path` command), `-it` means to run in an interactive terminal, and `-m 4g` means to set the maximum heap size of the JVM to 4GB.

# Updates

> [!UPDATE|label:2022/07/01]
>
> - Release the second version of GBC, version number 1.2,
>   - Github repository address: https://github.com/Zhangliubin/gbc
>   - Online Manual: http://pmglab.top/gbc/
> - Note that GBC-1.1 and GBC-1.2 are fully compatible versions, but GBC-1.2 and later versions will focus on the development of API tools and no longer maintain GUI programs.

<p>

> [!UPDATE|label:2022/04/02]
>
> - Release the first version of GBC, version number 1.1.
>   - Github repository address：https://github.com/Zhangliubin/Genotype-Blocking-Compressor
>   - Online Manual: http://pmglab.top/gbc/history/