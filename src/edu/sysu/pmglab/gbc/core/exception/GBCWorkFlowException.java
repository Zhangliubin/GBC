package edu.sysu.pmglab.gbc.core.exception;

/**
 * @Data        :2021/04/28
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :GBC 工作流程异常
 */

public class GBCWorkFlowException extends RuntimeException {
    public GBCWorkFlowException() {
        this("");
    }

    public GBCWorkFlowException(String message) {
        super(message);
    }

}
