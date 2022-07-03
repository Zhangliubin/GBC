package edu.sysu.pmglab.gbc.setup;

import edu.sysu.pmglab.gbc.setup.command.GBCEntryPoint;

import java.io.IOException;

/**
 * @Data        :2020/10/11
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :命令行运行模式
 */

public class CommandMode {
    public static int run(String[] args) throws IOException {
        // 执行
        return GBCEntryPoint.submit(args);
    }
}
