package com.tianji.learning.utils;

// 线程技术，同一个线程 数据的传输
public class TableInfoContext {

    private static final ThreadLocal<String> TL = new ThreadLocal<>();

    public static void setInfo(String info) {
        TL.set(info);
    }

    public static String getInfo() {
        return TL.get();
    }

    public static void clear() {
        TL.remove();
    }
}
