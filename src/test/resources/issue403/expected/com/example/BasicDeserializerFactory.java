package com.example;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import java.util.concurrent.*;
import tools.jackson.databind.jsontype.TypeDeserializer;

@SuppressWarnings("serial")
public abstract class BasicDeserializerFactory extends DeserializerFactory
    implements java.io.Serializable {

  protected final DeserializerFactoryConfig _factoryConfig = null;

  public ValueDeserializer<?> createArrayDeserializer(
      DeserializationContext ctxt, ArrayType type, final BeanDescription beanDesc) {
    final DeserializationConfig config = ctxt.getConfig();
    JavaType elemType = type.getContentType();
    @SuppressWarnings("unchecked")
    ValueDeserializer<Object> contentDeser = (ValueDeserializer<Object>) elemType.getValueHandler();
    TypeDeserializer elemTypeDeser = (TypeDeserializer) elemType.getTypeHandler();
    if (elemTypeDeser == null) {
      elemTypeDeser = ctxt.findTypeDeserializer(elemType);
    }
    ValueDeserializer<?> deser =
        _findCustomArrayDeserializer(type, config, beanDesc, elemTypeDeser, contentDeser);
    if (deser == null) {
      if (contentDeser == null) {
        if (elemType.isPrimitive()) {
          deser = PrimitiveArrayDeserializers.forType(elemType.getRawClass());
        } else if (elemType.hasRawClass(String.class)) {
          deser = StringArrayDeserializer.instance;
        }
      }
      if (deser == null) {
        deser = new ObjectArrayDeserializer(type, contentDeser, elemTypeDeser);
      }
    }
    if (_factoryConfig.hasDeserializerModifiers()) {
      for (ValueDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
        deser = mod.modifyArrayDeserializer(config, type, beanDesc, deser);
      }
    }
    return deser;
  }

  protected ValueDeserializer<?> _findCustomArrayDeserializer(
      ArrayType type,
      DeserializationConfig config,
      BeanDescription beanDesc,
      TypeDeserializer elementTypeDeserializer,
      ValueDeserializer<?> elementDeserializer) {
    throw new java.lang.Error();
  }
}
