package com.lonicera.rpc.exception;

import lombok.Getter;

@Getter
public class RpcException extends RuntimeException {

  public static final int CONNECT_ERROR = 1;
  public static final int WRITE_TIMEOUT = 2;
  public static final int READ_TIMEOUT = 3;
  public static final int CLIENT_ERROR = 4;
  public static final int SERVER_ERROR = 5;
  public static final int SERVICE_NOT_FOUND = 6;

  private final int errorCode;

  public RpcException(int errorCode, String msg) {
    super(msg);
    this.errorCode = errorCode;
  }

  public RpcException(int errorCode, Throwable throwable) {
    super(throwable);
    this.errorCode = errorCode;
  }

}
