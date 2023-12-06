package com.lonicera.rpc.protocol;

public interface RpcResponse {
  String correlationId();
  byte code();
  byte[] resultBytes();
}
