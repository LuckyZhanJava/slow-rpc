package com.lonicera.rpc.handler;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Server {
  private String host;
  private int port;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Server server = (Server) o;
    return port == server.port &&
        Objects.equals(host, server.host);
  }

  @Override
  public int hashCode() {
    return Objects.hash(host, port);
  }
}
