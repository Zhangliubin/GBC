package edu.sysu.pmglab.gbc.core.gtbcomponent;

import edu.sysu.pmglab.container.BiDict;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.container.array.StringArray;
import edu.sysu.pmglab.easytools.ArrayUtils;
import edu.sysu.pmglab.easytools.StringUtils;
import edu.sysu.pmglab.gbc.core.exception.GTBComponentException;

import java.util.Arrays;
import java.util.Objects;

/**
 * @Data :2021/02/18
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :样本名管理器
 */

public class GTBSubjectManager {
    private byte[] subjects;
    private int subjectNum;
    private BiDict<String, Integer> subjectsIndexBiDict;

    /**
     * 构造器方法
     */
    public GTBSubjectManager() {
        this((byte[]) null);
    }

    /**
     * 构造器方法
     */
    public GTBSubjectManager(VolumeByteStream subjects) {
        load(subjects);
    }

    /**
     * 构造器方法
     *
     * @param capacity 样本大小，将以此创建虚拟样本名 S1 S2 ...
     */
    public GTBSubjectManager(int capacity) {
        this(capacity == 0 ? null : range(capacity));
    }

    /**
     * 构造器方法
     *
     * @param subjects 样本名字节序列
     */
    public GTBSubjectManager(byte[] subjects) {
        load(subjects);
    }

    /**
     * 构造器方法
     *
     * @param subjects 样本名字符串序列
     */
    public GTBSubjectManager(String subjects) {
        this(((subjects == null) || subjects.length() == 0) ? null : subjects.getBytes());
    }

    /**
     * 重加载样本序列
     *
     * @param subjects 样本名字节序列
     */
    public void load(byte[] subjects) {
        if (subjects != null) {
            this.subjects = subjects;
            this.subjectsIndexBiDict = initSubjectsIndex();
        } else {
            this.subjects = new byte[0];
            this.subjectsIndexBiDict = new BiDict<>(0);
        }

        this.subjectNum = this.subjectsIndexBiDict.size();
    }

    /**
     * 重加载样本序列
     *
     * @param subjects 样本名定容字节数组序列
     */
    public GTBSubjectManager load(VolumeByteStream subjects) {
        if (subjects != null) {
            this.subjects = subjects.values();
            this.subjectsIndexBiDict = initSubjectsIndex();
        } else {
            this.subjects = new byte[0];
            this.subjectsIndexBiDict = new BiDict<>(0);
        }

        this.subjectNum = this.subjectsIndexBiDict.size();
        return this;
    }

    /**
     * 获取样本名序列
     */
    public byte[] getSubjects() {
        return this.subjects;
    }

    /**
     * 获取样本总数
     */
    public int getSubjectNum() {
        return this.subjectNum;
    }

    /**
     * 获取指定样本名的索引
     *
     * @param subject 指定的样本名
     * @return 样本名对应的索引
     */
    public int getIndex(String subject) {
        try {
            return this.subjectsIndexBiDict.valueOf(subject);
        } catch (NullPointerException e) {
            throw new GTBComponentException(subject + " not found in current GTB.");
        }
    }

    /**
     * 获取指定样本名的索引
     *
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
     *
     * @param subjects 指定的样本名
     * @return 样本名对应的索引
     */
    public int[] getIndexes(StringArray subjects) {
        int[] out = new int[subjects.size()];
        int index = 0;
        for (String subject : subjects) {
            out[index++] = getIndex(subject);
        }
        return out;
    }

    /**
     * 获取指定样本名的索引
     *
     * @param subject 指定的样本名
     * @return 样本名对应的索引
     */
    public int get(String subject) {
        return getIndex(subject);
    }

    /**
     * 获取指定样本名的索引
     *
     * @param subjects 指定的样本名
     * @return 样本名对应的索引
     */
    public int[] get(String... subjects) {
        return getIndexes(subjects);
    }

    /**
     * 获取指定样本名的索引
     *
     * @param subjects 指定的样本名
     * @return 样本名对应的索引
     */
    public int[] get(StringArray subjects) {
        return getIndexes(subjects);
    }

    /**
     * 获取指定索引对应的样本名
     *
     * @param index 指定的索引
     * @return 索引对应的样本名
     */
    public String getSubject(int index) {
        String subject = this.subjectsIndexBiDict.keyOf(index);

        if (subject == null) {
            throw new GTBComponentException(index + " not found in current GTB.");
        }
        return subject;
    }

    /**
     * 获取指定索引对应的样本名
     *
     * @param indexes 指定的索引
     * @return 索引对应的样本名
     */
    public String[] getSubjects(int... indexes) {
        String[] out = new String[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            out[i] = getSubject(indexes[i]);
        }
        return out;
    }

    /**
     * 获取所有的样本名
     */
    public String[] getAllSubjects() {
        return new String(subjects).split("\t");
    }

    /**
     * 获取指定索引对应的样本名
     *
     * @param index 指定的索引
     * @return 索引对应的样本名
     */
    public String get(int index) {
        return getSubject(index);
    }

    /**
     * 获取指定索引对应的样本名
     *
     * @param indexes 指定的索引
     * @return 索引对应的样本名
     */
    public String[] get(int... indexes) {
        return getSubjects(indexes);
    }

    /**
     * 获取指定索引对应的样本名
     *
     * @param index        指定的索引
     * @param defaultValue 不存在时，指定默认值
     * @return 索引对应的样本名
     */
    public String getOrDefault(int index, String defaultValue) {
        return this.subjectsIndexBiDict.keyOfOrDefault(index, defaultValue);
    }

    /**
     * 获取指定样本名对应的索引
     *
     * @param subject      指定的样本名
     * @param defaultValue 不存在时，指定默认值
     * @return 索引对应的样本名
     */
    public int getOrDefault(String subject, int defaultValue) {
        return this.subjectsIndexBiDict.valueOfOrDefault(subject, defaultValue);
    }

    /**
     * 只能在内部调用，防止出错
     */
    private BiDict<String, Integer> initSubjectsIndex() {
        if (this.subjects.length == 0) {
            return BiDict.of(new String[0]);
        } else {
            return BiDict.of(new String(this.subjects).split("\t"));
        }
    }

    /**
     * 内部使用的生成虚拟样本名 Sx
     *
     * @param capacity 数量
     */
    public static byte[] range(int capacity) {
        VolumeByteStream vbs = new VolumeByteStream(capacity * 5);
        vbs.write("S1".getBytes());

        for (int i = 2; i <= capacity; i++) {
            vbs.writeSafety(("\tS" + i).getBytes());
        }
        return vbs.values();
    }

    public boolean equal(byte[] subjects) {
        return ArrayUtils.equal(this.subjects, subjects);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(subjectNum, subjectsIndexBiDict);
        result = 31 * result + Arrays.hashCode(subjects);
        return result;
    }

    @Override
    public String toString() {
        return new String(subjects);
    }

    /**
     * 增加扩展字符的打印方法
     */
    public String toString(int prefixNum, int eachLineNumber) {
        if (this.subjectNum == 0) {
            return "";
        }

        String[] subjectArray = new String(subjects).split("\t");
        StringBuilder builder = new StringBuilder(subjects.length);
        String prefix = StringUtils.copyN(" ", prefixNum);
        for (int i = 0; i < subjectArray.length; i++) {
            builder.append(subjectsIndexBiDict.keyOf(i));
            if ((i + 1) % eachLineNumber != 0) {
                builder.append(" ");
            } else {
                builder.append("\n").append(prefix);
            }
        }
        return builder.toString();
    }
}
