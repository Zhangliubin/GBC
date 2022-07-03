package edu.sysu.pmglab.gbc.core.gtbcomponent;

import edu.sysu.pmglab.container.VolumeByteStream;

/**
 * @Data        :2021/02/20
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :参考序列管理器
 */

public class GTBReferenceManager {
    private final VolumeByteStream reference;

    /**
     * 构造器方法
     */
    public GTBReferenceManager(){
        this.reference = new VolumeByteStream(16);
    }

    /**
     * 构造器方法
     * @param capacity 序列容器初始长度
     */
    public GTBReferenceManager(int capacity){
        this.reference = new VolumeByteStream(capacity);
    }

    /**
     * 加载新的参考序列
     * @param reference 参考序列字节数组
     */
    public void load(byte[] reference){
        this.reference.reset();
        this.reference.writeSafety(reference);
    }

    /**
     * 加载新的参考序列
     * @param reference 参考序列字符串
     */
    public void load(String reference){
        load(reference.getBytes());
    }

    /**
     * 加载新的参考序列
     * @param reference 参考序列定容字节数组
     */
    public void load(VolumeByteStream reference){
        this.reference.wrap(reference.values());
    }

    /**
     * 获取参考序列
     * @return 参考序列
     */
    public VolumeByteStream getReference(){
        return this.reference;
    }

    /**
     * 获取参考序列长度
     * @return 参考序列长度
     */
    public int size(){
        return this.reference.size();
    }

    @Override
    public String toString() {
        return new String(reference.values());
    }
}
