package edu.sysu.pmglab.gbc.core.calculation.ld;

/**
 * @author suranyi
 */

class TaskNode {
    /**
     * 处理的任务节点
     */
    final String chromosome;

    final int minPos;
    final int maxPos;
    final int maxSearchPos;

    static TaskNode of(String chromosome, int minPos, int maxPos, int maxSearchPos) {
        return new TaskNode(chromosome, minPos, maxPos, maxSearchPos);
    }

    static TaskNode of(String chromosome, int minPos, int maxPos) {
        return new TaskNode(chromosome, minPos, maxPos, maxPos);
    }

    TaskNode(String chromosome, int minPos, int maxPos, int maxSearchPos) {
        this.chromosome = chromosome;
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.maxSearchPos = maxSearchPos;
    }
}
