package edu.sysu.pmglab.gbc.core.gtbcomponent;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.container.BiDict;
import edu.sysu.pmglab.easytools.ArrayUtils;
import edu.sysu.pmglab.easytools.ByteCode;

import java.util.Arrays;
import java.util.Objects;

/**
 * @Data        :2021/02/24
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :简易样本名管理器
 */

class GroupSubjectFormatter {
    public final byte[] subjects;
    public final int subjectNum;

    private final BiDict<String, Integer> subjectIndexBiDict;
    public int[] relativeIndexes;

    /**
     * 简易样本管理器
     * @param subjects 样本序列
     */
    public GroupSubjectFormatter(byte[] subjects) {
        Assert.NotNull(subjects);

        this.subjects = subjects;
        this.subjectIndexBiDict = null;
        this.subjectNum = subjects.length == 0 ? 0 : ArrayUtils.valueCounts(subjects, ByteCode.TAB) + 1;
    }

    /**
     * 简易样本管理器
     * @param subjects 样本序列
     */
    public GroupSubjectFormatter(byte[] subjects, boolean buildBiDict) {
        Assert.NotNull(subjects);

        this.subjects = subjects;

        if (buildBiDict) {
            this.subjectIndexBiDict = initSubjectIndex();
            this.subjectNum = this.subjectIndexBiDict.size();
        } else {
            this.subjectIndexBiDict = null;
            this.subjectNum = subjects.length == 0 ? 0 : ArrayUtils.valueCounts(subjects, ByteCode.TAB) + 1;
        }
    }

    /**
     * 获取指定样本名的索引
     * @param subject 指定的样本名
     * @return 样本名对应的索引
     */
    public int getIndex(String subject) {
        return this.subjectIndexBiDict.valueOf(subject);
    }

    /**
     * 获取指定样本名的索引
     * @param subjects 指定的样本名
     * @return 样本名对应的索引
     */
    public int[] getIndexes(String... subjects) {
        int[] out = new int[subjects.length];
        for (int i = 0; i < subjects.length; i++) {
            out[i] = getIndex(subjects[i]);
        }
        return out;
    }

    /**
     * 获取指定样本名的索引
     * @param subjects 指定的样本名
     * @return 样本名对应的索引
     */
    public int[] get(String... subjects) {
        return getIndexes(subjects);
    }

    /**
     * 只能在内部调用，防止出错
     */
    private BiDict<String, Integer> initSubjectIndex() {
        return BiDict.of(new String(this.subjects).split("\t"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GroupSubjectFormatter)) {
            return false;
        }

        return ArrayUtils.equal(this.subjects, ((GroupSubjectFormatter) o).subjects);
    }

    public boolean equal(byte[] subjects) {
        return ArrayUtils.equal(this.subjects, subjects);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(subjectNum, subjectIndexBiDict);
        result = 31 * result + Arrays.hashCode(subjects);
        return result;
    }
}
