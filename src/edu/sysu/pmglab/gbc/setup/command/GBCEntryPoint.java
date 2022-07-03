package edu.sysu.pmglab.gbc.setup.command;

import edu.sysu.pmglab.bgztools.BGZToolkit;
import edu.sysu.pmglab.commandParser.exception.ParameterException;
import edu.sysu.pmglab.container.File;
import edu.sysu.pmglab.container.array.Array;
import edu.sysu.pmglab.container.array.BaseArray;
import edu.sysu.pmglab.container.array.IntArray;
import edu.sysu.pmglab.container.array.StringArray;
import edu.sysu.pmglab.downloader.HttpDownloader;
import edu.sysu.pmglab.easytools.ArrayUtils;
import edu.sysu.pmglab.easytools.MD5;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.calculation.ld.ILDModel;
import edu.sysu.pmglab.gbc.core.calculation.ld.LDTask;
import edu.sysu.pmglab.gbc.core.common.allelechecker.AlleleFreqGapTestChecker;
import edu.sysu.pmglab.gbc.core.common.allelechecker.Chi2TestChecker;
import edu.sysu.pmglab.gbc.core.common.allelechecker.LDTestChecker;
import edu.sysu.pmglab.gbc.core.common.allelechecker.MixChecker;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant.*;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBRootCache;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBToolkit;
import edu.sysu.pmglab.gbc.core.gtbcomponent.ManagerStringBuilder;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.GTBReader;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbwriter.GTBOutputParam;
import edu.sysu.pmglab.unifyIO.FileStream;
import edu.sysu.pmglab.unifyIO.partwriter.BGZOutputParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

/**
 * @author :suranyi
 * @Description :程序入口点
 */

public enum GBCEntryPoint {
    /**
     * 单例
     */
    INSTANCE;

    private static final Logger logger = LoggerFactory.getLogger("GBC");

    public static int submit(String[] args) {
        // 解析参数
        try {
            GBCParser runMode = GBCParser.parse(args);
            if (runMode.help.isPassedIn) {
                System.out.println(GBCParser.usage());
                return 0;
            }

            if (runMode.update.isPassedIn) {
                if (runMode.update.value.length == 0) {
                    return UpdateFunction(new File("./gbc.jar"));
                } else if (runMode.update.value.length == 1) {
                    return UpdateFunction(runMode.update.value[0]);
                } else {
                    throw new ParameterException("update takes 0 or 1 positional argument (" + runMode.update.value.length + " given)");
                }
            }

            if (runMode.build.isPassedIn) {
                if (args.length == 1) {
                    System.out.println(BuildParser.usage());
                    return 0;
                }

                BuildParser options = BuildParser.parse(args);
                if (options.help.isPassedIn) {
                    System.out.println(BuildParser.usage());
                    return 0;
                }

                return BuildFunction(options);
            }

            if (runMode.extract.isPassedIn) {
                if (args.length == 1) {
                    System.out.println(ExtractParser.usage());
                    return 0;
                }

                ExtractParser options = ExtractParser.parse(args);
                if (options.help.isPassedIn) {
                    System.out.println(ExtractParser.usage());
                    return 0;
                }

                return ExtractFunction(options);
            }

            if (runMode.show.isPassedIn) {
                if (args.length == 1) {
                    System.out.println(ShowParser.usage());
                    return 0;
                }

                ShowParser options = ShowParser.parse(args);
                if (options.help.isPassedIn) {
                    System.out.println(ShowParser.usage());
                    return 0;
                }

                return ShowFunction(options);
            }

            if (runMode.sort.isPassedIn) {
                if (args.length == 1) {
                    System.out.println(SortParser.usage());
                    return 0;
                }

                SortParser options = SortParser.parse(args);
                if (options.help.isPassedIn) {
                    System.out.println(SortParser.usage());
                    return 0;
                }

                return SortFunction(options);
            }

            if (runMode.concat.isPassedIn) {
                if (args.length == 1) {
                    System.out.println(ConcatParser.usage());
                    return 0;
                }

                ConcatParser options = ConcatParser.parse(args);
                if (options.help.isPassedIn) {
                    System.out.println(ConcatParser.usage());
                    return 0;
                }

                return ConcatFunction(options);
            }

            if (runMode.merge.isPassedIn) {
                if (args.length == 1) {
                    System.out.println(MergeParser.usage());
                    return 0;
                }

                MergeParser options = MergeParser.parse(args);
                if (options.help.isPassedIn) {
                    System.out.println(MergeParser.usage());
                    return 0;
                }

                return MergeFunction(options);
            }

            if (runMode.resetSubject.isPassedIn) {
                if (args.length == 1) {
                    System.out.println(ResetSubjectParser.usage());
                    return 0;
                }

                ResetSubjectParser options = ResetSubjectParser.parse(args);
                if (options.help.isPassedIn) {
                    System.out.println(ResetSubjectParser.usage());
                    return 0;
                }

                return ResetSubjectFunction(options);
            }

            if (runMode.prune.isPassedIn) {
                if (args.length == 1) {
                    System.out.println(PruneParser.usage());
                    return 0;
                }

                PruneParser options = PruneParser.parse(args);
                if (options.help.isPassedIn) {
                    System.out.println(PruneParser.usage());
                    return 0;
                }

                return PruneFunction(options);
            }

            if (runMode.alleleCheck.isPassedIn) {
                if (args.length == 1) {
                    System.out.println(AlleleCheckParser.usage());
                    return 0;
                }

                AlleleCheckParser options = AlleleCheckParser.parse(args);
                if (options.help.isPassedIn) {
                    System.out.println(AlleleCheckParser.usage());
                    return 0;
                }

                return AlleleCheckFunction(options);
            }

            if (runMode.split.isPassedIn) {
                if (args.length == 1) {
                    System.out.println(SplitParser.usage());
                    return 0;
                }

                SplitParser options = SplitParser.parse(args);
                if (options.help.isPassedIn) {
                    System.out.println(SplitParser.usage());
                    return 0;
                }

                return SplitFunction(options);
            }

            if (runMode.index.isPassedIn) {
                if (args.length == 1) {
                    System.out.println(IndexParser.usage());
                    return 0;
                }

                IndexParser options = IndexParser.parse(args);
                if (options.help.isPassedIn) {
                    System.out.println(IndexParser.usage());
                    return 0;
                }

                return IndexFunction(options);
            }

            if (runMode.ld.isPassedIn) {
                if (args.length == 1) {
                    System.out.println(LDParser.usage());
                    return 0;
                }

                LDParser options = LDParser.parse(args);
                if (options.help.isPassedIn) {
                    System.out.println(LDParser.usage());
                    return 0;
                }

                return LDFunction(options);
            }

            if (runMode.bgzip.isPassedIn) {
                BGZToolkit.main(ArrayUtils.copyOfRange(args, 1, args.length));
                return 0;
            }

            if (runMode.md5.isPassedIn) {
                if (args.length == 1) {
                    System.out.println(MD5Parser.usage());
                    return 0;
                }

                MD5Parser options = MD5Parser.parse(args);
                if (options.help.isPassedIn) {
                    System.out.println(MD5Parser.usage());
                    return 0;
                }

                return MD5Function(options);
            }

            if (runMode.download.isPassedIn) {
                HttpDownloader.main(ArrayUtils.copyOfRange(args, 1, args.length));
                return 0;
            }

        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("{}", e.getMessage(), e);
            } else {
                logger.error("{}", e.getMessage());
            }
        }

        return 0;
    }

    /**
     * 下载最新版 GBC
     */
    static int UpdateFunction(File to) throws IOException {
        HttpDownloader.updateResource("http://pmglab.top/gbc/download/gbc.jar", to, "GBC");
        return 0;
    }

    /**
     * GBC 压缩工作
     */
    static int BuildFunction(BuildParser options) throws IOException {
        // 判断输出文件是否存在
        if (!checkFileExists(!options.yes.isPassedIn, options.output.value)) {
            return -1;
        }

        // 加载染色体资源文件
        if (options.contig.isPassedIn) {
            ChromosomeTags.load(options.contig.value);
        }

        // 预处理工作
        GTBOutputParam outputParam = new GTBOutputParam()
                .setPhased(options.phased.isPassedIn)
                .simplyAllele(options.simply.isPassedIn)
                .setReordering(!options.noReordering.isPassedIn)
                .setWindowSize(options.windowsize.value)
                .setBlockSizeType(options.blocksizetype.value)
                .setCompressor(options.compressor.value, options.level.value)
                .setMaxAllelesNum(options.maxAllele.value)
                .splitMultiallelics(options.biallelic.isPassedIn)
                .readyParas(options.readyparas.value)
                .filterByAC(options.seqAc.value)
                .filterByAF(options.seqAf.value)
                .filterByAN(options.seqAn.value);

        GTBToolkit.Build task = GTBToolkit.Build.instance(options.build.value, options.output.value)
                .setOutputParam(outputParam)
                .setThreads(options.threads.value);

        // 设置质控参数
        if (options.noQc.isPassedIn) {
            task.controlDisable();
        } else {
            // 质量控制
            task.setGenotypeQualityControlDp(options.gtyDp.value)
                    .setGenotypeQualityControlGq(options.gtyGq.value)
                    .setVariantPhredQualityScore(options.seqQual.value)
                    .setVariantQualityControlDp(options.seqDp.value)
                    .setVariantQualityControlMq(options.seqMq.value);
        }

        // 输出日志
        logger.info("\n" + task);
        long jobStart = System.currentTimeMillis();
        task.submit();
        long jobEnd = System.currentTimeMillis();

        // 结束任务，输出日志信息
        logger.info("Total Processing time: {} s; GTB size: {}", String.format("%.3f", (float) (jobEnd - jobStart) / 1000), options.output.value.formatSize(3));
        logger.info("You can use command `show {}` to view all the information.", options.output.value);

        return 0;
    }

    /**
     * GBC 解压工作
     */
    static int ExtractFunction(ExtractParser options) throws IOException {
        // 判断输出文件是否存在
        if (!checkFileExists(!options.yes.isPassedIn, options.output.value)) {
            return -1;
        }

        // 加载染色体资源文件
        if (options.contig.isPassedIn) {
            ChromosomeTags.load(options.contig.value);
        }

        // 根据文件尾名或类型确定提取的方法
        if (options.oGtb.isPassedIn || (!options.oText.isPassedIn && !options.oBgz.isPassedIn && options.output.value.withExtension(".gtb"))) {
            // 指定了输出为 gtb 格式
            GTBOutputParam outputParam = new GTBOutputParam(options.extract.value)
                    .simplyAllele(options.simply.isPassedIn)
                    .setReordering(!options.noReordering.isPassedIn)
                    .setWindowSize(options.windowsize.value)
                    .setBlockSizeType(options.blocksizetype.value)
                    .setMaxAllelesNum(options.maxAllele.value)
                    .splitMultiallelics(options.biallelic.isPassedIn)
                    .readyParas(options.readyparas.value)
                    .filterByAC(options.seqAc.value)
                    .filterByAF(options.seqAf.value)
                    .filterByAN(options.seqAn.value);

            // 设定向型
            if (options.phased.isPassedIn) {
                outputParam.setPhased(options.phased.value);
            } else {
                outputParam.setPhased(GTBRootCache.get(options.extract.value).isPhased());
            }

            GTBToolkit.Subset task = GTBToolkit.Subset.instance(options.extract.value, options.output.value)
                    .setThreads(options.threads.value)
                    .setOutputParam(outputParam);

            if (options.subject.isPassedIn) {
                task.setSubjects(options.subject.value);
            }

            if (options.range.isPassedIn) {
                task.setRanges(options.range.value);
            }

            if (options.retainNode.isPassedIn) {
                task.setPruner(tree -> tree.retain(options.retainNode.value));
            }

            if (options.random.isPassedIn) {
                task.setPositions(parsePositions(options.random.value));
            }

            if (options.hidegt.isPassedIn) {
                throw new ParameterException("'--hideGT' are not allowed to be used in '--o-gtb' format");
            }

            // 提交任
            logger.info("\n" + task);
            long jobStart = System.currentTimeMillis();
            task.submit();
            long jobEnd = System.currentTimeMillis();

            // 结束任务，输出日志信息
            logger.info("Total Processing time: {} s; GTB size: {}", String.format("%.3f", (float) (jobEnd - jobStart) / 1000), options.output.value.formatSize(3));
            logger.info("You can use command `show {}` to view all the information.", options.output.value);
        } else {
            // vcf 格式
            GTBToolkit.Formatter task = GTBToolkit.Formatter.instance(options.extract.value, options.output.value)
                    .setCLM(!options.noClm.isPassedIn)
                    .setThreads(options.threads.value);

            // 设置是否压缩输出文件
            if (options.oText.isPassedIn) {
                task.setOutputParam(null);
            } else if (options.oBgz.isPassedIn || options.level.isPassedIn) {
                task.setOutputParam(new BGZOutputParam(options.level.value));
            } else {
                if (options.output.value.withExtension(".gz")) {
                    task.setOutputParam(new BGZOutputParam(options.level.value));
                } else {
                    task.setOutputParam(null);
                }
            }

            // 设定向型
            if (options.phased.isPassedIn) {
                task.setPhased(options.phased.value);
            } else {
                task.setPhased(GTBRootCache.get(options.extract.value).isPhased());
            }

            if (options.subject.isPassedIn) {
                task.setSubjects(options.subject.value);
            }

            if (options.range.isPassedIn) {
                task.setRanges(options.range.value);
            }

            if (options.retainNode.isPassedIn) {
                task.setPruner(tree -> tree.retain(options.retainNode.value));
            }

            if (options.random.isPassedIn) {
                task.setPositions(parsePositions(options.random.value));
            }

            if (options.hidegt.isPassedIn) {
                task.setFileFormat(GTBToolkit.Formatter.FileFormatter.VCFFormat_WithoutGenotype);
            }

            BaseArray<IVariantQC> filter = new Array<>();
            if (options.seqAn.isPassedIn) {
                filter.add(new AlleleNumberController(options.seqAn.value[0], options.seqAn.value[1]));
            }

            if (options.seqAc.isPassedIn) {
                filter.add(new AlleleCountController(options.seqAc.value[0], options.seqAc.value[1]));
            }

            if (options.seqAf.isPassedIn) {
                filter.add(new AlleleFrequencyController(options.seqAf.value[0], options.seqAf.value[1]));
            }

            task.setCondition(variant -> {
                if (variant.getAlternativeAlleleNum() <= options.maxAllele.value) {
                    for (IVariantQC qc : filter) {
                        if (!qc.filter(variant)) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    return false;
                }
            });

            // 提交任务
            logger.info("\n" + task);
            long jobStart = System.currentTimeMillis();
            task.submit();
            long jobEnd = System.currentTimeMillis();

            // 结束任务，输出日志信息
            logger.info("Total Processing time: {} s; Output size: {}", String.format("%.3f", (float) (jobEnd - jobStart) / 1000), options.output.value.formatSize(3));
        }

        return 0;
    }

    /**
     * GBC 快速查看数据模式
     */
    static int ShowFunction(ShowParser options) throws IOException {
        // 加载染色体资源文件
        if (options.contig.isPassedIn) {
            ChromosomeTags.load(options.contig.value);
        }

        // 仅打印样本信息
        if (options.listSubjectOnly.isPassedIn) {
            System.out.println(StringArray.wrap(GTBRootCache.get(options.show.value).getSubjectManager().getAllSubjects()).join(","));
            return 0;
        }

        // 仅打印位置信息
       boolean status = options.listPositionOnly.isPassedIn || options.listSiteOnly.isPassedIn || options.subject.isPassedIn || options.range.isPassedIn ||
               options.random.isPassedIn || options.retainNode.isPassedIn || options.seqAc.isPassedIn || options.seqAf.isPassedIn || options.seqAn.isPassedIn ||
               options.maxAllele.isPassedIn;

        if (status) {
            // 如果传入了过滤类参数, 则执行相关操作
            GTBManager manager = GTBRootCache.get(options.show.value);
            VariantQC variantQC = new VariantQC();
            HashSet<String> chromosomes = StringArray.wrap(manager.getChromosomeList()).toHashSet();
            Map<String, Set<Integer>> randomPositions = null;

            if (options.maxAllele.isPassedIn) {
                variantQC.add(new IVariantQC() {
                    @Override
                    public boolean filter(Variant variant) {
                        return variant.getAlternativeAlleleNum() <= options.maxAllele.value;
                    }
                });
            }

            if (options.range.isPassedIn) {
                Map<String, int[]> ranges = options.range.value;
                variantQC.add(new IVariantQC() {
                    @Override
                    public boolean filter(Variant variant) {
                        return ranges.containsKey(variant.chromosome) && (ranges.get(variant.chromosome) == null || (variant.position >= ranges.get(variant.chromosome)[0] && variant.position <= ranges.get(variant.chromosome)[1]));
                    }
                });

                chromosomes.retainAll(ranges.keySet());
            }

            if (options.random.isPassedIn) {
                Map<String, int[]> parsedPositions = parsePositions(options.random.value);
                randomPositions = new HashMap<>(parsedPositions.size());

                for (String chromosome : parsedPositions.keySet()) {
                    if (parsedPositions.get(chromosome) != null) {
                        if (options.range.isPassedIn) {
                            int minPos = options.range.value.get(chromosome)[0];
                            int maxPos = options.range.value.get(chromosome)[1];
                            randomPositions.put(chromosome, IntArray.wrap(parsedPositions.get(chromosome)).filter(position -> position >= minPos && position <= maxPos).toSet());
                        } else {
                            randomPositions.put(chromosome, IntArray.wrap(parsedPositions.get(chromosome)).toSet());
                        }
                    }
                }
                chromosomes.retainAll(randomPositions.keySet());
            }

            if (options.retainNode.isPassedIn) {
                manager.getGtbTree().retain(options.retainNode.value);
                chromosomes.retainAll(options.retainNode.value.keySet());
            }

            boolean basedOnAllele = false;
            if (options.seqAc.isPassedIn) {
                basedOnAllele = true;
                variantQC.add(new AlleleCountController(options.seqAc.value[0], options.seqAc.value[1]));
            }

            if (options.seqAf.isPassedIn) {
                basedOnAllele = true;
                variantQC.add(new AlleleFrequencyController(options.seqAf.value[0], options.seqAf.value[1]));
            }

            if (options.seqAn.isPassedIn) {
                basedOnAllele = true;
                variantQC.add(new AlleleNumberController(options.seqAn.value[0], options.seqAn.value[1]));
            }

            GTBReader reader;
            if (basedOnAllele || options.listSiteOnly.isPassedIn || !options.listPositionOnly.isPassedIn) {
                reader = new GTBReader(manager, GTBRootCache.get(options.show.value).isPhased(), true);
            } else {
                reader = new GTBReader(manager, GTBRootCache.get(options.show.value).isPhased(), false);
            }

            if (options.subject.isPassedIn) {
                reader.selectSubjects(options.subject.value);
            }

            if (options.listPositionOnly.isPassedIn) {
                for (String chromosome : manager.getChromosomeList()) {
                    if (!chromosomes.contains(chromosome)) {
                        continue;
                    }
                    reader.limit(chromosome);
                    boolean fastRangeSearch = options.range.isPassedIn && reader.searchEnable(chromosome);

                    if (fastRangeSearch) {
                        reader.search(chromosome, options.range.value.get(chromosome)[0]);
                    }

                    if (randomPositions != null) {
                        Set<Integer> positions = randomPositions.get(chromosome);
                        if (positions == null || positions.size() == 0) {
                            continue;
                        }

                        Variant variant = new Variant();
                        while (reader.readVariant(variant, positions)) {
                            if (fastRangeSearch && variant.position > options.range.value.get(chromosome)[1]) {
                                break;
                            }

                            if (variantQC.filter(variant)) {
                                System.out.println(variant.chromosome + "\t" + variant.position);
                            }
                        }
                    } else {
                        for (Variant variant : reader) {
                            if (fastRangeSearch && variant.position > options.range.value.get(chromosome)[1]) {
                                break;
                            }

                            if (variantQC.filter(variant)) {
                                System.out.println(variant.chromosome + "\t" + variant.position);
                            }
                        }
                    }
                }
            } else {
                for (String chromosome : manager.getChromosomeList()) {
                    if (!chromosomes.contains(chromosome)) {
                        continue;
                    }

                    reader.limit(chromosome);
                    boolean fastRangeSearch = options.range.isPassedIn && reader.searchEnable(chromosome);

                    if (fastRangeSearch) {
                        reader.search(chromosome, options.range.value.get(chromosome)[0]);
                    }

                    if (randomPositions != null) {
                        Set<Integer> positions = randomPositions.get(chromosome);
                        if (positions == null || positions.size() == 0) {
                            continue;
                        }

                        Variant variant = new Variant();
                        while (reader.readVariant(variant, positions)) {
                            if (fastRangeSearch && variant.position > options.range.value.get(chromosome)[1]) {
                                break;
                            }

                            if (variantQC.filter(variant)) {
                                System.out.println(variant.chromosome + "\t" + variant.position + "\t" + new String(variant.REF) + "\t" + new String(variant.ALT) + "\tAC=" + variant.getAC() + ";AF=" + String.format("%.8f", variant.getAF()) + ";AN=" + variant.getAN());
                            }
                        }
                    } else {
                        for (Variant variant : reader) {
                            if (fastRangeSearch && variant.position > options.range.value.get(chromosome)[1]) {
                                break;
                            }

                            if (variantQC.filter(variant)) {
                                System.out.println(variant.chromosome + "\t" + variant.position + "\t" + new String(variant.REF) + "\t" + new String(variant.ALT) + "\tAC=" + variant.getAC() + ";AF=" + String.format("%.8f", variant.getAF()) + ";AN=" + variant.getAN());
                            }
                        }
                    }
                }
            }

            GTBRootCache.clear(manager);
            return 0;
        }

        ManagerStringBuilder builder = GTBRootCache.get(options.show.value).getManagerStringBuilder();
        if (options.full.isPassedIn) {
            builder.calculateMd5(options.addMd5.isPassedIn).listFileBaseInfo(true).listSummaryInfo(true).listSubjects(true).listGTBTree(true);
        } else {
            builder.calculateMd5(options.addMd5.isPassedIn).listFileBaseInfo(true).listSummaryInfo(true).listSubjects(options.addSubject.isPassedIn);

            if (options.addTree.isPassedIn || options.addNode.isPassedIn) {
                if (options.addNode.isPassedIn) {
                    builder.listGTBTree(true);
                } else {
                    builder.listChromosomeInfo(true);
                }
            }
        }

        System.out.println(builder.build());
        return 0;
    }

    /**
     * GBC 排序文件操作
     */
    static int SortFunction(SortParser options) throws IOException {
        // 判断输出文件是否存在
        if (!checkFileExists(!options.yes.isPassedIn, options.output.value)) {
            return -1;
        }

        // 加载染色体资源文件
        if (options.contig.isPassedIn) {
            ChromosomeTags.load(options.contig.value);
        }

        // 预处理工作
        GTBOutputParam outputParam = new GTBOutputParam(options.sort.value)
                .simplyAllele(options.simply.isPassedIn)
                .setReordering(!options.noReordering.isPassedIn)
                .setWindowSize(options.windowsize.value)
                .setBlockSizeType(options.blocksizetype.value)
                .setCompressor(options.compressor.value, options.level.value)
                .setMaxAllelesNum(options.maxAllele.value)
                .splitMultiallelics(options.biallelic.isPassedIn)
                .readyParas(options.readyparas.value)
                .filterByAC(options.seqAc.value)
                .filterByAF(options.seqAf.value)
                .filterByAN(options.seqAn.value);

        if (options.phased.isPassedIn) {
            outputParam.setPhased(options.phased.value);
        } else {
            outputParam.setPhased(GTBRootCache.get(options.sort.value).isPhased());
        }

        GTBToolkit.Sort task = GTBToolkit.Sort.instance(options.sort.value, options.output.value)
                .setOutputParam(outputParam)
                .setThreads(options.threads.value)
                .setSubjects(options.subject.value);

        logger.info("\n" + task);
        long jobStart = System.currentTimeMillis();
        task.submit();
        long jobEnd = System.currentTimeMillis();

        // 结束任务，输出日志信息
        logger.info("Total Processing time: {} s; GTB size: {}", String.format("%.3f", (float) (jobEnd - jobStart) / 1000), options.output.value.formatSize(3));
        logger.info("You can use command `show {}` to view all the information.", options.output.value);
        return 0;
    }

    /**
     * GBC 连接文件操作
     */
    static int ConcatFunction(ConcatParser options) throws IOException {
        // 判断输出文件是否存在
        if (!checkFileExists(!options.yes.isPassedIn, options.output.value)) {
            return -1;
        }

        // 加载染色体资源文件
        if (options.contig.isPassedIn) {
            ChromosomeTags.load(options.contig.value);
        }

        BaseArray<File> inputs = new Array<>(File[].class);
        for (File file : options.concat.value) {
            if (file.isDirectory()) {
                inputs.addAll(file.listFilesDeeply(fileName -> fileName.withExtension(".gtb")));
            } else {
                inputs.add(file);
            }
        }
        GTBToolkit.Concat task = GTBToolkit.Concat.instance(inputs.toArray(), options.output.value);

        logger.info("\n" + task);
        long jobStart = System.currentTimeMillis();
        task.submit();
        long jobEnd = System.currentTimeMillis();

        // 结束任务，输出日志信息
        logger.info("Total Processing time: {} s; GTB size: {}", String.format("%.3f", (float) (jobEnd - jobStart) / 1000), options.output.value.formatSize(3));
        logger.info("You can use command `show {}` to view all the information.", options.output.value);
        return 0;
    }

    /**
     * GBC 合并工作
     */
    static int MergeFunction(MergeParser options) throws IOException {
        // 判断输出文件是否存在
        if (!checkFileExists(!options.yes.isPassedIn, options.output.value)) {
            return -1;
        }

        // 加载染色体资源文件
        if (options.contig.isPassedIn) {
            ChromosomeTags.load(options.contig.value);
        }

        BaseArray<File> inputs = new Array<>(File[].class);
        for (File file : options.merge.value) {
            if (file.isDirectory()) {
                inputs.addAll(file.listFilesDeeply(fileName -> fileName.withExtension(".gtb")));
            } else {
                inputs.add(file);
            }
        }

        // 预处理工作
        GTBOutputParam outputParam = new GTBOutputParam()
                .simplyAllele(options.simply.isPassedIn)
                .setReordering(!options.noReordering.isPassedIn)
                .setWindowSize(options.windowsize.value)
                .setBlockSizeType(options.blocksizetype.value)
                .setCompressor(options.compressor.value, options.level.value)
                .setMaxAllelesNum(options.maxAllele.value)
                .splitMultiallelics(options.biallelic.isPassedIn)
                .readyParas(options.readyparas.value)
                .filterByAC(options.seqAc.value)
                .filterByAF(options.seqAf.value)
                .filterByAN(options.seqAn.value);

        if (options.phased.isPassedIn) {
            outputParam.setPhased(options.phased.value);
        } else {
            outputParam.setPhased(GTBRootCache.get(inputs.get(0)).isPhased());
        }

        GTBToolkit.Merge task = GTBToolkit.Merge.instance(inputs.toArray(), options.output.value)
                .setOutputParam(outputParam)
                .setUnion(options.union.isPassedIn)
                .setThreads(options.threads.value);

        // 输出日志
        System.out.println("\n" + task);
        long jobStart = System.currentTimeMillis();
        task.submit();
        long jobEnd = System.currentTimeMillis();

        // 结束任务，输出日志信息
        logger.info("Total Processing time: {} s; GTB size: {}", String.format("%.3f", (float) (jobEnd - jobStart) / 1000), options.output.value.formatSize(3));
        logger.info("You can use command `show {}` to view all the information.", options.output.value);

        return 0;
    }

    /**
     * GBC 重设样本名信息
     */
    static int ResetSubjectFunction(ResetSubjectParser options) throws IOException {
        // 判断输出文件是否存在
        if (!checkFileExists(!options.yes.isPassedIn, options.output.value)) {
            return -1;
        }

        // 加载染色体资源文件
        if (options.contig.isPassedIn) {
            ChromosomeTags.load(options.contig.value);
        }

        GTBToolkit.ResetSubject task = GTBToolkit.ResetSubject.instance(options.resetSubject.value, options.output.value);

        if (options.subject.isPassedIn) {
            task.setSubjects(options.subject.value);
        } else {
            String suffix = options.suffix.isPassedIn ? options.suffix.value : "";
            task.setSubjects((String[]) IntArray.wrap(ArrayUtils.range(options.begin.value, options.begin.value + GTBRootCache.get(options.resetSubject.value).getSubjectNum() - 1)).apply((Function<Integer, Object>) index -> options.prefix.value + index + suffix).toArray(new String[0]));
        }

        logger.info("\n" + task);
        long jobStart = System.currentTimeMillis();
        task.submit();
        long jobEnd = System.currentTimeMillis();

        // 结束任务，输出日志信息
        logger.info("Total Processing time: {} s; GTB size: {}", String.format("%.3f", (float) (jobEnd - jobStart) / 1000), options.output.value.formatSize(3));
        logger.info("You can use command `show {}` to view all the information.", options.output.value);
        return 0;
    }

    /**
     * GBC 重设样本名信息
     */
    static int PruneFunction(PruneParser options) throws IOException {
        // 判断输出文件是否存在
        if (!checkFileExists(!options.yes.isPassedIn, options.output.value)) {
            return -1;
        }

        // 加载染色体资源文件
        if (options.contig.isPassedIn) {
            ChromosomeTags.load(options.contig.value);
        }

        GTBToolkit.Prune task = GTBToolkit.Prune.instance(options.prune.value, options.output.value);

        if (options.deleteNode.isPassedIn) {
            task.remove(options.deleteNode.value);
        } else if (options.retainNode.isPassedIn) {
            task.retain(options.retainNode.value);
        } else if (options.deleteChrom.isPassedIn) {
            task.remove(options.deleteChrom.value);
        } else if (options.retainChrom.isPassedIn) {
            task.retain(options.retainChrom.value);
        }

        logger.info("\n" + task);
        long jobStart = System.currentTimeMillis();
        task.submit();
        long jobEnd = System.currentTimeMillis();

        // 结束任务，输出日志信息
        logger.info("Total Processing time: {} s; GTB size: {}", String.format("%.3f", (float) (jobEnd - jobStart) / 1000), options.output.value.formatSize(3));
        logger.info("You can use command `show {}` to view all the information.", options.output.value);
        return 0;
    }

    /**
     * GBC 检查碱基标签
     */
    static int AlleleCheckFunction(AlleleCheckParser options) throws IOException {
        // 判断输出文件是否存在
        if (!checkFileExists(!options.yes.isPassedIn, options.output.value)) {
            return -1;
        }

        // 加载染色体资源文件
        if (options.contig.isPassedIn) {
            ChromosomeTags.load(options.contig.value);
        }

        GTBOutputParam outputParam = new GTBOutputParam()
                .simplyAllele(options.simply.isPassedIn)
                .setReordering(!options.noReordering.isPassedIn)
                .setWindowSize(options.windowsize.value)
                .setBlockSizeType(options.blocksizetype.value)
                .setCompressor(options.compressor.value, options.level.value)
                .setMaxAllelesNum(options.maxAllele.value)
                .splitMultiallelics(options.biallelic.isPassedIn)
                .readyParas(options.readyparas.value)
                .filterByAC(options.seqAc.value)
                .filterByAF(options.seqAf.value)
                .filterByAN(options.seqAn.value);

        if (options.phased.isPassedIn) {
            outputParam.setPhased(options.phased.value);
        } else {
            outputParam.setPhased(GTBRootCache.get(options.alleleCheck.value)[1].isPhased());
        }

        GTBToolkit.AlleleCheck task = GTBToolkit.AlleleCheck.instance(options.alleleCheck.value[0], options.alleleCheck.value[1], options.output.value)
                .setOutputParam(outputParam)
                .setUnion(options.union.isPassedIn)
                .setThreads(options.threads.value);

        if (options.noLd.isPassedIn) {
            if (options.freqGap.isPassedIn) {
                task.setAlleleChecker(new AlleleFreqGapTestChecker(options.freqGap.value, options.maf.value));
            } else {
                task.setAlleleChecker(new Chi2TestChecker(options.pValue.value, options.maf.value));
            }
        } else {
            if (options.freqGap.isPassedIn) {
                task.setAlleleChecker(new MixChecker(new AlleleFreqGapTestChecker(options.freqGap.value, options.maf.value), new LDTestChecker(options.minR.value, options.flipScanThreshold.value, options.windowBp.value, options.maf.value)));
            } else {
                task.setAlleleChecker(new MixChecker(new Chi2TestChecker(options.pValue.value, options.maf.value), new LDTestChecker(options.minR.value, options.flipScanThreshold.value, options.windowBp.value, options.maf.value)));
            }
        }

        logger.info("\n" + task);
        long jobStart = System.currentTimeMillis();
        task.submit();
        long jobEnd = System.currentTimeMillis();

        // 结束任务，输出日志信息
        logger.info("Total Processing time: {} s; GTB size: {}", String.format("%.3f", (float) (jobEnd - jobStart) / 1000), options.output.value.formatSize(3));
        logger.info("You can use command `show {}` to view all the information.", options.output.value);
        return 0;
    }

    /**
     * GBC 分裂文件
     */
    static int SplitFunction(SplitParser options) throws IOException {
        // 判断输出文件是否存在
        if (!checkFileExists(!options.yes.isPassedIn, options.output.value)) {
            return -1;
        }

        // 加载染色体资源文件
        if (options.contig.isPassedIn) {
            ChromosomeTags.load(options.contig.value);
        }

        GTBToolkit.Split task = GTBToolkit.Split.instance(options.split.value, options.output.value);

        if (options.by.value.equals("chromosome")) {
            task.splitByChromosome();
        } else {
            task.splitByNode();
        }

        logger.info("\n" + task);
        long jobStart = System.currentTimeMillis();
        task.submit();
        long jobEnd = System.currentTimeMillis();

        // 结束任务，输出日志信息
        logger.info("Total Processing time: {} s; GTBs size: {}", String.format("%.3f", (float) (jobEnd - jobStart) / 1000), options.output.value.formatSize(3));
        if (options.by.value.equals("chromosome")) {
            logger.info("You can use command `show {}/chr[x].gtb` to view all the information.", options.output.value);
        } else {
            logger.info("You can use command `show {}/chr[x].node[Y].gtb` to view all the information.", options.output.value);
        }

        return 0;
    }

    /**
     * GBC LD 系数计算任务
     */
    static int LDFunction(LDParser options) throws IOException {
        // 判断输出文件是否存在
        if (!checkFileExists(!options.yes.isPassedIn, options.output.value)) {
            return -1;
        }

        // 加载染色体资源文件
        if (options.contig.isPassedIn) {
            ChromosomeTags.load(options.contig.value);
        }

        // 设置 LD 模型
        ILDModel ldModel = options.genoLd.isPassedIn ? ILDModel.GENOTYPE_LD : options.hapLd.isPassedIn ? ILDModel.HAPLOTYPE_LD : null;

        LDTask task = new LDTask(options.ld.value, options.output.value)
                .setLdModel(ldModel)
                .setMaf(options.maf.value)
                .setMinR2(options.minR2.value)
                .setWindowSizeBp(options.windowBp.value)
                .setParallel(options.threads.value);

        // 设置输出文件名
        if (options.oText.isPassedIn) {
            task.setCompressToBGZF(false);
        } else if (options.oBgz.isPassedIn || options.level.isPassedIn || options.output.value.withExtension(".gz")) {
            task.setCompressToBGZF(true, options.level.value);
        }

        // 样本选择
        if (options.subject.isPassedIn) {
            task.selectSubjects(options.subject.value);
        }

        logger.info("\n" + task);
        long jobStart = System.currentTimeMillis();
        if (options.range.isPassedIn) {
            // 按照位置访问
            task.submit(options.range.value);
        } else {
            task.submit();
        }
        long jobEnd = System.currentTimeMillis();

        // 结束任务，输出日志信息
        logger.info("Total Processing time: {} s; LD file size: {}", String.format("%.3f", (float) (jobEnd - jobStart) / 1000), options.output.value.formatSize(3));
        return 0;
    }

    /**
     * MD5 码校验工作
     */
    static int MD5Function(MD5Parser options) throws IOException {
        // 解析命令
        for (File file : options.md5.value) {
            String md5Code = MD5.check(file);

            if (options.oMd5.isPassedIn) {
                if (!checkFileExists(!options.yes.isPassedIn, file.addExtension(".md5"))) {
                    return -1;
                }

                try (FileStream fs = new FileStream(file.addExtension(".md5"), FileStream.DEFAULT_WRITER)) {
                    fs.write(md5Code);
                }
            }

            System.out.printf("MD5 (%s) = %s%n", file, md5Code);
        }

        return 0;
    }

    /**
     * GBC 索引文件工作
     */
    static int IndexFunction(IndexParser options) throws IOException {
        // 判断输出文件是否存在
        if (!checkFileExists(!options.yes.isPassedIn, options.output.value)) {
            return -1;
        }

        // 加载染色体资源文件
        if (options.fromContig.isPassedIn) {
            ChromosomeTags.load(options.fromContig.value);
        }

        boolean isGTB;

        // 判断是否为 GTB 文件，如果不是 GTB 文件，则使用转换方法
        try {
            GTBRootCache.get(options.index.value);
            isGTB = true;
        } catch (Exception ignored) {
            isGTB = false;
        }

        if (isGTB) {
            // 打开 GTB 管理器
            GTBManager manager = GTBRootCache.get(options.index.value);

            // 重设重叠群信息
            manager.resetContig(options.toContig.value);

            // 输出文件
            manager.toFile(options.output.value);
        } else {
            ChromosomeTags.build(options.index.value, options.output.value, options.deepScan.isPassedIn);
        }

        return 0;
    }

    private static boolean checkFileExists(boolean check, File file) {
        if (check) {
            if (file.isExists()) {
                Scanner scanner = new Scanner(System.in);
                logger.warn("{} already exists, do you wish to overwrite? (y or n)", file);

                // 不覆盖文件，则删除该文件
                if (!"y".equalsIgnoreCase(scanner.next().trim())) {
                    logger.error("GBC can't create {}: file exists", file);
                    return false;
                }
            }
        }

        return true;
    }

    private static Map<String, int[]> parsePositions(File file) {
        // 使用 HashSet 保存位点，实现去重效果
        HashMap<String, IntArray> elementSet = new HashMap<>();
        HashMap<String, int[]> target = new HashMap<>();

        // 打开并读取文件
        try (FileStream fs = new FileStream(file, file.withExtension(".gz") ? FileStream.GZIP_READER : FileStream.DEFAULT_READER)) {
            String line;
            String chromosome;
            int position;

            while ((line = fs.readLineToString()) != null) {
                String[] split = null;
                if (line.contains(",")) {
                    split = line.split(",");
                } else if (line.contains(" ")) {
                    split = line.split(" ");
                } else if (line.contains("\t")) {
                    split = line.split("\t");
                }

                if ((split == null) || (split.length != 2)) {
                    throw new ParameterException("couldn't convert " + line + " to 'chrom,pos' or 'chrom<\\t>position'");
                }

                chromosome = split[0];

                // 匹配 position
                position = Integer.parseInt(split[1]);
                if (!elementSet.containsKey(chromosome)) {
                    elementSet.put(chromosome, new IntArray(1024, true));
                }
                elementSet.get(chromosome).add(position);
            }

            // 为位点排序
            for (String chromosomeN : elementSet.keySet()) {
                IntArray positions = elementSet.get(chromosomeN);
                if ((positions != null) && (positions.size() > 0)) {
                    // 元素去重
                    positions.dropDuplicated();

                    // 提取最终结果
                    target.put(chromosomeN, positions.toBaseArray());
                }
            }

            return target;
        } catch (IOException e) {
            throw new ParameterException(e.getMessage());
        }
    }
}
