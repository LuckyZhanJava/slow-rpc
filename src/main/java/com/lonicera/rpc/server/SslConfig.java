package com.lonicera.rpc.server;

import java.io.InputStream;
import lombok.Getter;

@Getter
public class SslConfig {
  private InputStream keyCertChainInputStream;
  private InputStream keyInputStream;

  public SslConfig(InputStream keyCertChainInputStream, InputStream keyInputStream){
    this.keyCertChainInputStream = keyCertChainInputStream;
    this.keyInputStream = keyInputStream;
  }
}
