package edu.sysu.pmglab.gbc.core.exception;

/**
 * @Data        :2021/04/28
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :GTB 组件异常
 */

public class GTBComponentException extends RuntimeException {
    public GTBComponentException() {
        this("");
    }

    public GTBComponentException(String message) {
        super(message);
    }
}
