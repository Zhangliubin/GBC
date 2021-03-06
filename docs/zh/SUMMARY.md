# Summary

### 项目简介

- [关于 GBC](README.md)
- [下载与安装](Download.md)
- [文件格式](Format.md)
- [API 文档](https://pmglab.top/gbc/api-docs/index.html)

### 使用手册

- [命令行程序](usage/README.md)
  - [压缩基因型](usage/build.md#BuildMode)
    - [程序参数](usage/build.md#Options)
    - [程序实例](usage/build.md#Examples)
    - [算法介绍](usage/build.md#Algorithm)
  - [提取基因型](usage/extract.md#ExtractMode)
    - [程序参数](usage/extract.md#Options)
    - [程序实例](usage/extract.md#Examples)
  - [显示 GTB 文件摘要信息](usage/show.md#ShowMode)
    - [程序参数](usage/show.md#Options)
    - [程序实例](usage/show.md#Examples)
  - [按照坐标对 GTB 文件排序](usage/sort.md#SortMode)
    - [程序参数](usage/sort.md#Options)
    - [程序实例](usage/sort.md#Examples)
  - [合并多个 GTB 文件](usage/merge.md#MergeGTBMode)
    - [串联多个 GTB 文件](usage/merge.md#ConcatMode)
      - [程序参数](usage/merge.md#ConcatOptions)
      - [程序实例](usage/merge.md#ConcatExamples)
    - [合并多个 GTB 文件](usage/merge.md#MergeMode)
      - [程序参数](usage/merge.md#MergeOptions)
      - [程序实例](usage/merge.md#MergeExamples)
  - [重设样本名](usage/reset-subject.md#ResetSubjectMode)
    - [程序参数](usage/reset-subject.md#Options)
    - [程序实例](usage/reset-subject.md#Examples)
  - [GTB 文件剪枝](usage/prune.md#PruneMode)
    - [程序参数](usage/prune.md#Options)
    - [程序实例](usage/prune.md#Examples)
  - [等位基因标签检查](usage/allele-check.md#AlleleCheckMode)
    - [程序参数](usage/allele-check.md#Options)
    - [程序实例](usage/allele-check.md#Examples)
  - [分裂 GTB 文件](usage/split.md#SplitMode)
    - [程序参数](usage/split.md#Options)
    - [程序实例](usage/split.md#Examples)
  - [计算 LD 系数](usage/ld.md#LDMode)
    - [程序参数](usage/ld.md#Options)
    - [程序实例](usage/ld.md#Examples)
  - [设置染色体标签](usage/index_.md#IndexMode)
    - [Contig 文件格式](usage/index_.md#IndexMode)
    - [为 VCF 文件构建 Contig 文件](usage/index_.md#BuildContig)
    - [重建染色体标签索引](usage/index_.md#RebuildContig)
- 使用 API 工具进行开发
  - GTB 组件与操作 GTB 文件
  - MBEG 编码
  - 位点对象
