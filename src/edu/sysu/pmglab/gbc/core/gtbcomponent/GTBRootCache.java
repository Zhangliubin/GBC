package edu.sysu.pmglab.gbc.core.gtbcomponent;

import edu.sysu.pmglab.container.File;
import edu.sysu.pmglab.container.array.BaseArray;

import java.util.HashMap;

/**
 * @Data        :2020/06/22
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :全局单例模式 gtb 根节点缓存器
 */

public enum GTBRootCache {
    /* 单例模式 GTB 文件根节点 */
    INSTANCE;

    private final HashMap<File, GTBManager> cache = new HashMap<>(4);

    /**
     * 获取管理器
     */
    public static GTBManager get(File fileName) {
        synchronized (INSTANCE.cache) {
            if (!INSTANCE.cache.containsKey(fileName)) {
                INSTANCE.cache.put(fileName, new GTBManager(fileName));
            }

            return INSTANCE.cache.get(fileName);
        }
    }

    /**
     * 获取管理器
     */
    public static GTBManager[] get(File... fileNames) {
        GTBManager[] managers = new GTBManager[fileNames.length];

        for (int i = 0; i < fileNames.length; i++) {
            managers[i] = get(fileNames[i]);
        }
        return managers;
    }

    /**
     * 获取管理器
     */
    public static GTBManager[] get(BaseArray<File> fileNames) {
        return get(fileNames.toArray());
    }

    /**
     * 获取当前管理器储存的数量
     */
    public static int size() {
        synchronized (INSTANCE.cache) {
            return INSTANCE.cache.size();
        }
    }

    /**
     * 清除指定的管理器数据
     */
    public static void clear() {
        synchronized (INSTANCE.cache) {
            INSTANCE.cache.clear();
        }
    }

    /**
     * 清除指定的管理器数据
     */
    public static void clear(File... files) {
        synchronized (INSTANCE.cache) {
            for (File file : files) {
                INSTANCE.cache.remove(file);
            }
        }
    }

    /**
     * 清除指定的管理器数据
     */
    public static void clear(GTBManager... managers) {
        synchronized (INSTANCE.cache) {
            for (GTBManager manager : managers) {
                INSTANCE.cache.remove(manager.getFile());
            }
        }
    }

    /**
     * 清除指定的管理器数据
     */
    public static void clear(BaseArray<File> managers) {
        synchronized (INSTANCE.cache) {
            for (File manager: managers) {
                INSTANCE.cache.remove(manager);
            }
        }
    }

    /**
     * 检验缓存器中是否存在该管理器
     */
    public static boolean contain(File file) {
        synchronized (INSTANCE.cache) {
            return INSTANCE.cache.containsKey(file);
        }
    }

    /**
     * 获取管理器的名字
     */
    public static File[] getNames(GTBManager... managers) {
        File[] files = new File[managers.length];
        for (int i = 0; i < managers.length; i++) {
            files[i] = managers[i].getFile();
        }

        return files;
    }
}
