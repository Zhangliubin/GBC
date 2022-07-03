package edu.sysu.pmglab.gbc.setup;

import edu.sysu.pmglab.easytools.MD5;
import edu.sysu.pmglab.easytools.StringUtils;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBRootCache;
import edu.sysu.pmglab.gbc.setup.command.GBCEntryPoint;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

/**
 * @Data        :2020/10/11
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :交互模式
 */

public class InteractiveMode {
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final String[] EXIT_SIGNAL = new String[]{"q", "quit", "exit"};

    /**
     * 优先级指令
     */
    static final HashMap<String, Runnable> priorityInstruction = new HashMap<>(4);
    static {
        priorityInstruction.put("clear", () -> System.out.print(StringUtils.copyN("\n", 20)));
        priorityInstruction.put("reset", () -> {
            GTBRootCache.clear();
            MD5.clear();
            System.gc();
        });
    }

    public static void run() throws IOException {
        // 初始化
        init();

        // 获取输入
        String input;

        // 循环读取用户输入
        while (!contain(EXIT_SIGNAL, input = receive(">>> "))) {
            if (!input.startsWith("#")) {
                String[] args = convertToStringArray(identifyMultiLineMode(input));

                if (args.length > 0) {
                    // 高优先级指令
                    Runnable runnable = priorityInstruction.get(args[0]);
                    if (runnable == null) {
                        GBCEntryPoint.submit(args);
                    } else {
                        runnable.run();
                    }
                } else {
                    // 没有捕获到命令，提示用户退出交互模式的方式
                    System.out.println("Use q, quit, exit or Ctrl-D (i.e. EOF) to exit");
                }
            }
        }

        // 退出程序
        System.exit(0);
    }

    /**
     * 初始化
     */
    private static void init() {
        System.out.println("Enter GBC Command-Line Interactive Environment!");
    }

    /**
     * 循环接受参数
     */
    private static String receive(String modifier) {
        System.out.print(modifier);
        return SCANNER.nextLine().replace("\t", " ").trim();
    }

    /**
     * 转为字符串数组
     */
    private static String[] convertToStringArray(String input) {
        return Arrays.stream(input.split(" ")).filter(command -> command.length() > 0).toArray(String[]::new);
    }

    /**
     * 换行符检测器
     */
    private static boolean containShift(String input) {
        // 包含换行符
        boolean onlyShift = (input.length() == 1) && ("\\".equals(input));

        // 末尾数据是  \\ 或 仅有 换行标记
        return input.endsWith(" \\") || onlyShift;
    }

    /**
     * 识别多行输入模式，若为多行输入模式，则持续接受数据，直到最后获得结果；否则，直接输出数据
     */
    private static String identifyMultiLineMode(String input) {
        if (containShift(input)) {
            StringBuilder inputLink = new StringBuilder();

            do {
                // 替换规则：仅有一个 \，则不进行拼接
                if (input.length() != 1) {
                    inputLink.append(input.replace(" \\", " "));
                }

                input = receive("> ");
            } while (containShift(input));

            // 最后，不包含换行符，则需要补充最后一次的数据
            inputLink.append(input);
            return inputLink.toString();
        } else {
            return input;
        }
    }

    /**
     * 检验目标 target 是否在容器中
     * @param container 检验容器
     * @param target 目标元素
     * @return 检验结果 boolean 值
     */
    private static boolean contain(String[] container, String target) {
        if (target == null || container == null) {
            return false;
        }

        for (String element : container) {
            if (target.equalsIgnoreCase(element)) {
                return true;
            }
        }

        return false;
    }
}
