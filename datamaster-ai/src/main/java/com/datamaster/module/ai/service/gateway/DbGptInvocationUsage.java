package com.datamaster.module.ai.service.gateway;

/** DB-GPT客户端解析原始响应时报告usage；不存原始内容，不依赖输出文本估算。 */
public final class DbGptInvocationUsage {
    private static final ThreadLocal<InvocationUsage> CURRENT = new ThreadLocal<>();
    private DbGptInvocationUsage() { }
    public static void accept(String line) { InvocationUsage value = CURRENT.get(); if (value != null) value.acceptLine(line); }
    public static Scope scope() { return new Scope(); }
    public static final class Scope implements AutoCloseable {
        private final InvocationUsage previous = CURRENT.get();
        private final InvocationUsage value = new InvocationUsage();
        private Scope() { CURRENT.set(value); }
        public InvocationUsage usage() { return value; }
        @Override public void close() { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
}
