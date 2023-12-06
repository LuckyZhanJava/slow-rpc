package com.lonicera.rpc.server;


import java.util.HashMap;
import java.util.Map;

public class RpcContext {

  private static RpcContext CONTEXT = new RpcContext();

  private Map<String, Object> attachments = new HashMap<>();

  private static ThreadLocal<RpcContext> HOLDER_LOCAL = new ThreadLocal<RpcContext>() {
    @Override
    protected RpcContext initialValue() {
      return new RpcContext();
    }
  };

  public Map<String, Object> attachments() {
    return HOLDER_LOCAL.get().attachments;
  }

  void attachments(Map<String, Object> attachments) {
    HOLDER_LOCAL.get().attachments = attachments;
  }

  public static RpcContext getContext() {
    return CONTEXT;
  }

  public static void clearContext() {
    HOLDER_LOCAL.get().attachments = new HashMap<>();
  }
}
