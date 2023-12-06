package com.lonicera.rpc.client;

import lombok.Getter;

@Getter
public final class ClientOptions {
  private SocketChannelOptions channelOptions;
  private ChannelPoolOptions poolOptions;

  public ClientOptions(){
    this(new SocketChannelOptions(), new ChannelPoolOptions());
  }

  public ClientOptions(SocketChannelOptions channelOptions, ChannelPoolOptions poolOptions) {
    this.channelOptions = channelOptions;
    this.poolOptions = poolOptions;
  }

}
