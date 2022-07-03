package edu.sysu.pmglab.gbc.core.gtbcomponent;

import edu.sysu.pmglab.compressor.ICompressor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @Data :2021/06/17
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :管理器构造器
 */

public class ManagerStringBuilder {
    private final GTBManager manager;
    private final String prefix = "\n  ";
    private final HashMap<String, String> builder = new HashMap<>(10);
    private final ArrayList<String> builderOrder = new ArrayList<>(10);

    ManagerStringBuilder(GTBManager manager) throws IOException {
        this.manager = manager;
        add("fileName", "GTB File Name: " + manager.getFile());

        // 记录文件大小
        add("fileSize", "GTB File Size: " + this.manager.getFile().formatSize(3));

        // 记录参考基因组
        if (manager.getReferenceSize() != 0) {
            add("reference", "Genome Reference: " + new String(manager.getReference().values()));
        }
    }

    public ManagerStringBuilder listFileBaseInfo(boolean enable) {
        FileBaseInfoManager info = this.manager.getFileBaseInfoManager();

        if (enable) {
            add("suggestToBGZF", "Suggest To BGZF: " + info.isSuggestToBGZF());
            add("phased", "Phased: " + info.isPhased());
            add("ordered GTB", "Ordered GTB: " + info.orderedGTB());
            add("blockSize", "BlockSize: " + info.getBlockSize() + (" (-bs " + info.getBlockSizeType() + ")"));
            add("compressionLevel", "Compression Level: " + (info.getCompressionLevel()) + " (" + ICompressor.getCompressorName(info.getCompressorIndex()) + ")");
        } else {
            add("phased", "Phased: " + info.isPhased());
            add("ordered GTB", "Ordered GTB: " + info.orderedGTB());
            add("suggestToBGZF", "Suggest To BGZF: " + info.isSuggestToBGZF());
        }
        return this;
    }

    public ManagerStringBuilder listSummaryInfo(boolean enable) {
        if (enable) {
            int chromosomeListLength = this.manager.getChromosomeList().length;
            int variantsNum = this.manager.getGtbTree().numOfVariants();
            int subjectNum = this.manager.getSubjectNum();
            add("dimension", "Dimension of Genotypes: " + chromosomeListLength + (chromosomeListLength <= 1 ? " chromosome, " : " chromosomes, ")
                    + variantsNum + (variantsNum <= 1 ? " variant and " : " variants and ") + subjectNum + (subjectNum <= 1 ? " subject" : " subjects"));
        } else {
            if (this.builder.containsKey("shape")) {
                remove("shape");
            }
        }
        return this;
    }

    /**
     * 计算文件的 MD 5 码
     */
    public ManagerStringBuilder calculateMd5(boolean enable) throws IOException {
        if (enable) {
            add("md5", "MD5 Code: " + this.manager.getFile().md5());
        } else {
            if (this.builder.containsKey("md5")) {
                remove("md5");
            }
        }
        return this;
    }

    /**
     * 列出样本名
     */
    public ManagerStringBuilder listSubjects(boolean enable) {
        if (enable) {
            add("subject", "Subject Sequence: " + this.manager.getSubjectManager().toString(19, 10));
        } else {
            if (this.builder.containsKey("subject")) {
                remove("subject");
            }
        }
        return this;
    }

    /**
     * 列出 GTB 树
     */
    public ManagerStringBuilder listGTBTree(boolean enable) {
        if (enable) {
            add("gtbNodes", "\nSummary of GTB Nodes:\n" + this.manager.getGtbTree());
        } else {
            if (this.builder.containsKey("gtbNodes")) {
                remove("gtbNodes");
            }
        }
        return this;
    }

    /**
     * 列出 GTB 树，并指定染色体编号
     */
    public ManagerStringBuilder listGTBTree(boolean enable, String[] chromosomes) {
        if (enable) {
            add("gtbNodes", "\nSummary of GTB Nodes:\n" + this.manager.getGtbTree().nodeInfo(chromosomes));
        } else {
            if (this.builder.containsKey("gtbNodes")) {
                remove("gtbNodes");
            }
        }
        return this;
    }

    public ManagerStringBuilder listChromosomeInfo(boolean enable) {
        if (enable) {
            add("gtbNodes", "\nSummary of GTB Nodes:\n" + this.manager.getGtbTree().chromosomeInfo());
        } else {
            if (this.builder.containsKey("gtbNodes")) {
                remove("gtbNodes");
            }
        }
        return this;
    }

    public ManagerStringBuilder listChromosomeInfo(boolean enable, String[] chromosomes) {
        if (enable) {
            add("gtbNodes", "\nSummary of GTB Nodes:\n" + this.manager.getGtbTree().chromosomeInfo(chromosomes));
        } else {
            if (this.builder.containsKey("gtbNodes")) {
                remove("gtbNodes");
            }
        }
        return this;
    }

    public void add(String key, String value) {
        this.builder.put(key, value);
        this.builderOrder.add(key);
    }

    public void remove(String key) {
        this.builder.remove(key);
        this.builderOrder.remove(key);
    }

    public String build() {
        StringBuilder out = new StringBuilder(2 << 20);
        out.append("Summary of GTB File:");
        for (int i = 0; i < this.builder.size(); i++) {
            out.append(prefix).append(this.builder.get(this.builderOrder.get(i)));
        }
        return out.toString();
    }
}