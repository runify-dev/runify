package com.run.common.run_command;

/**
 * 命令执行的最终结果（退出码 + 完整 stdout/stderr）。
 */
public record CommandResult(int exitCode, String stdout, String stderr) {
}