package com.lonicera.rpc.client;

import io.netty.channel.ChannelOption;
import java.util.LinkedHashMap;
import java.util.Map;

public class SocketChannelOptions {
  private Map<ChannelOption<?>,Object> optionMap;

  public SocketChannelOptions(){
    optionMap = new LinkedHashMap<>();
  }

  public <T> SocketChannelOptions option(ChannelOption<T> option, T value){
    optionMap.put(option, value);
    return this;
  }

  public Map<ChannelOption<?>,Object> options(){
    return optionMap;
  }
}
