package edu.sysu.pmglab.gbc.setup;

import edu.sysu.pmglab.container.array.StringArray;

import java.io.IOException;

/**
 * @Data :2020/10/11
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :程序入口。程序链接顺序：选择界面模式 -> 解析运行模式
 */

public class Setup {
    private static final String INTERACTIVE_MODE_COMMAND = "-i";

    /**
     * 选择界面模式的主方法
     *
     * @param args jar 文件运行时接受的参数列表
     */
    public static void main(String[] args) throws IOException {
        // 将输入参数包装为字符串列表
        StringArray argsList = StringArray.wrap(args);
        if (argsList.contains(INTERACTIVE_MODE_COMMAND)) {
            // 参数为 -i，则进入交互模式
            if (argsList.size() != 1) {
                // 先运行指令，再进入交互模式
                argsList.remove(INTERACTIVE_MODE_COMMAND);
                CommandMode.run(argsList.toArray(new String[0]));
            }

            InteractiveMode.run();
        } else {
            System.exit(CommandMode.run(args));
        }
    }
}
