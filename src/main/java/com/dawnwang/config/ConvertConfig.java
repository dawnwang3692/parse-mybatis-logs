package com.dawnwang.config;

public class ConvertConfig {
    // 日志文件目录
    private String logFilesDir;
    // 需要匹配的sql类型
    private String[] sqlType;
    // 目标表名
    private String targetTableName;
    // 匹配前缀的正则表达式
    private String prefixRegex;
    // 匹配SQL时间的正则表达式
    private String regexTime;

    public ConvertConfig(String logFilesDir, String[] sqlType, String targetTableName, String prefixRegex, String regexTime) {
        this.logFilesDir = logFilesDir;
        this.sqlType = sqlType;
        this.targetTableName = targetTableName;
        this.prefixRegex = prefixRegex;
        this.regexTime = regexTime;
    }

    public String getLogFilesDir() {
        return logFilesDir;
    }

    public void setLogFilesDir(String logFilesDir) {
        this.logFilesDir = logFilesDir;
    }

    public String[] getSqlType() {
        return sqlType;
    }

    public void setSqlType(String[] sqlType) {
        this.sqlType = sqlType;
    }

    public String getTargetTableName() {
        return targetTableName;
    }

    public void setTargetTableName(String targetTableName) {
        this.targetTableName = targetTableName;
    }

    public String getPrefixRegex() {
        return prefixRegex;
    }

    public void setPrefixRegex(String prefixRegex) {
        this.prefixRegex = prefixRegex;
    }

    public String getRegexTime() {
        return regexTime;
    }

    public void setRegexTime(String regexTime) {
        this.regexTime = regexTime;
    }
}
