package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader;

/**
 * @Data :2021/04/12
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :索引对
 */

class IndexPair {
    public int index;
    public int groupIndex;
    public int codeIndex;

    public IndexPair(int index, int groupIndex, int codeIndex) {
        this.index = index;
        this.groupIndex = groupIndex;
        this.codeIndex = codeIndex;
    }

    public IndexPair(int groupIndex) {
        this.index = groupIndex;
    }
}