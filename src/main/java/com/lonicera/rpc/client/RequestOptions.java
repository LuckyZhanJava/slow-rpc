package com.lonicera.rpc.client;

import lombok.Getter;

@Getter
public final class RequestOptions {

  private final int readTimeoutMillis;
  private final int writeTimeoutMillis;

  public RequestOptions() {
    this(60 * 1000, 60 * 1000);
  }

  public RequestOptions(int writeTimeoutMillis, int readTimeoutMillis) {
    this.writeTimeoutMillis = writeTimeoutMillis;
    this.readTimeoutMillis = readTimeoutMillis;
  }
}
