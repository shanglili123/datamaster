package com.datamaster.module.ai.service.gateway;

/** 异步调用前捕获真实调用身份；不改变登录身份，也不作为授权依据。 */
public final class AiInvocationContext {
    private static final ThreadLocal<AiInvocationGateway.Context> CURRENT = new ThreadLocal<>();
    private AiInvocationContext() { }
    public static AiInvocationGateway.Context current() { return CURRENT.get(); }
    public static Scope scope(AiInvocationGateway.Context context) { return new Scope(context); }
    public static final class Scope implements AutoCloseable {
        private final AiInvocationGateway.Context previous;
        private Scope(AiInvocationGateway.Context context) { previous = CURRENT.get(); CURRENT.set(context); }
        @Override public void close() { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
}
