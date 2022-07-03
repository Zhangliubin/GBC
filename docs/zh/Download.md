# 下载与安装

GBC 是基于 Oracle JDK 8 开发的应用程序，任何兼容支持或兼容 Oracle JDK 8 的计算机设备都可以使用我们的软件。用户需要先下载安装 [Oracle JDK](https://www.oracle.com/cn/java/technologies/javase-downloads.html) 或 [Open JDK](https://openjdk.java.net/install/)。Apple Silicon 设备可以使用 [zulu JDK](https://www.azul.com/downloads/?package=jdk#download-openjdk) 作为替代。此外，我们也提供 Dockerfile 构建 GBC 的运行环境镜像。

| 资源类型         | 路径                                   |
| :--------------- | :------------------------------------- |
| **软件包**       | http://pmglab.top/gbc/download/gbc.jar |
| **源代码**       | https://github.com/Zhangliubin/gbc     |
| **说明文档**     | http://pmglab.top/gbc/                 |
| **API 文档** | http://pmglab.top/gbc/api-docs/        |
| **示例数据** | http://pmglab.top/gbc/download/example.zip<br />http://pmglab.top/genotypes/#/ |

## 通过 wget 下载软件包

```bash
# 下载 GBC 软件包
wget http://pmglab.top/gbc/download/gbc.jar -O gbc.jar

# 运行 GBC 软件包
java -jar gbc.jar
```

## 通过 Docker 运行 GBC 软件包

```bash
# 下载 GBC 的 Dockerfile 文件
wget http://pmglab.top/gbc/download/Dockerfile -O Dockerfile

# 从 Dockerfile 文件构建镜像
docker build -t gbc .

# 运行 GBC 软件包
docker run -it --rm gbc
```

## 从 Github 下载 GBC 源代码

```bash
# 下载 GBC 源代码
git clone https://github.com/Zhangliubin/gbc gbc-source

# 进入文件夹
cd gbc-source

# 运行 GBC 软件包
java -jar gbc.jar
```

# 更新软件包

GBC 目前已经迭代到稳定版本，通常情况下我们只会在现有的软件包基础上增加新的功能，或为增强稳定性和提升性能进行的少量代码架构更新。检查新版本可用性，请使用：

```bash
java -jar gbc.jar update
```

# 运行要求

GBC 进行了严格的内存需求控制，对于小规模的基因组数据，通常可以以默认内存分配量运行。对于大规模的基因组数据，GBC 单线程的内存使用量最多不超过 4 GB。因此，我们建议在始终不小于 4GB 的堆内存中运行 GBC 程序。用户通过以下指令分配 GBC 的运行时堆内存：

```bash
java -Xms4g -Xmx4g -jar gbc.jar
```

使用 Docker 运行 GBC 时，我们推荐使用以下模版语句：

```bash
# Macos 或 Linux
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc [options]

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -e -m 4g gbc [options]
```

在该语句中，`-v` 表示将指定的主机路径映射到容器路径 (`宿主机路径:容器路径`)，`-w` 表示设置当前工作路径 (相当于运行 `cd` 指令)，`-it` 表示在交互式终端中运行，`-m 4g` 表示设置 JVM 最大堆大小为 4GB。

# 更新日志

> [!UPDATE|label:2022/06/20]
>
> - 发布 GBC 的第二个版本，版本号 1.2。
>   - Github 仓库地址：https://github.com/Zhangliubin/gbc
>   - 文档地址：http://pmglab.top/gbc/
> - 请注意，GBC-1.1 与 GBC-1.2 是完全兼容的版本。GBC-1.2 专注于提升开发者 API 的使用体验，并移除了图形界面启动功能 (该功能仍然在 GBC-1.1 中提供)。

<p>

> [!UPDATE|label:2022/4/2]
>
> - 发布 GBC 的第一个版本，版本号 1.1。
>   - Github 仓库地址：https://github.com/Zhangliubin/Genotype-Blocking-Compressor
>   - 文档地址：http://pmglab.top/gbc/