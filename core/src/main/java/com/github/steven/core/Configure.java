package com.github.steven.core;

public class Configure {
    private int javaPid;                    // 对方java进程号
    private String className;
    private int logEnable;

    public int getJavaPid() {
        return javaPid;
    }

    public void setJavaPid(int javaPid) {
        this.javaPid = javaPid;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getLogEnable() {
        return logEnable;
    }

    public void setLogEnable(int logEnable) {
        this.logEnable = logEnable;
    }
}


