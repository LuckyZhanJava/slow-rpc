package com.lonicera.rpc.protocol.http;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lonicera.rpc.protocol.ObjectCodec;
import java.io.IOException;
import java.lang.reflect.Type;

public class JacksonObjectMapper {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static {
    OBJECT_MAPPER.findAndRegisterModules();
  }

  public static ObjectCodec objectMapperCodec() {
    return new JacksonObjectCodec();
  }

  private static class JacksonObjectCodec implements ObjectCodec {

    @Override
    public Object decodeValue(byte[] bytes, Type type) throws IOException {
      Object arg = OBJECT_MAPPER.readValue(bytes, new TypeReference<Object>() {
        @Override
        public Type getType() {
          return type;
        }
      });
      return arg;
    }

    @Override
    public Object[] decodeValues(byte[] bytes, Type[] types) throws IOException {
      JsonNode jsonNode = OBJECT_MAPPER.readTree(bytes);
      if (jsonNode instanceof ArrayNode) {
        ArrayNode arrayNode = (ArrayNode) jsonNode;
        int nodeCount = arrayNode.size();
        if (nodeCount == types.length) {
          Object[] args = new Object[types.length];
          for (int i = 0; i < nodeCount; i++) {
            Type targetType = types[i];
            Object arg = OBJECT_MAPPER
                .readValue(arrayNode.get(i).toString(), new TypeReference<Object>() {
                  @Override
                  public Type getType() {
                    return targetType;
                  }
                });
            args[i] = arg;
          }
          return args;
        }
      }
      throw new JsonParseException("not expect format");
    }

    @Override
    public byte[] encodeValue(Object value) throws JsonProcessingException {
      return OBJECT_MAPPER.writeValueAsBytes(value);
    }

    @Override
    public String contentType() {
      return "application/json";
    }
  }
}
