package com.lonicera.rpc.client;

import com.lonicera.rpc.protocol.RpcRequest;
import com.lonicera.rpc.protocol.RpcResponse;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
class ChannelRpcRequest<REQ extends RpcRequest,RESP extends RpcResponse> {
  private REQ rpcRequest;
  private final CompletableFuture<RESP> responseFuture = new CompletableFuture<>();
  private int writeTimeoutMillis;
  private int readTimeoutMillis;
}
