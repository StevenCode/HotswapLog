package com.github.steven;

public class Configure {
    private int javaPid;                    // 对方java进程号
    private String className;
    private String jarPath;
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

    public String getJarPath() {
        return jarPath;
    }

    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }

    public int getLogEnable() {
        return logEnable;
    }

    public void setLogEnable(int logEnable) {
        this.logEnable = logEnable;
    }
}


