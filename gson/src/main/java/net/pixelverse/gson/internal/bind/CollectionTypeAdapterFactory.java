/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.pixelverse.gson.internal.bind;

import net.pixelverse.gson.Gson;
import net.pixelverse.gson.SuperGson;
import net.pixelverse.gson.TypeAdapter;
import net.pixelverse.gson.TypeAdapterFactory;
import net.pixelverse.gson.internal.$Gson$Types;
import net.pixelverse.gson.internal.ConstructorConstructor;
import net.pixelverse.gson.internal.ObjectConstructor;
import net.pixelverse.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Adapt a homogeneous collection of objects.
 */
public final class CollectionTypeAdapterFactory implements TypeAdapterFactory {
  private final ConstructorConstructor constructorConstructor;

  public CollectionTypeAdapterFactory(ConstructorConstructor constructorConstructor) {
    this.constructorConstructor = constructorConstructor;
  }

  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
    Type type = typeToken.getType();

    Class<? super T> rawType = typeToken.getRawType();
    if (!Collection.class.isAssignableFrom(rawType)) {
      return null;
    }

    TypeAdapter<?> elementTypeAdapter;
    ObjectConstructor<T> constructor;
    Type elementType;

    if (gson instanceof SuperGson) {
      elementType = $Gson$Types.getCollectionElementType(type, rawType);
      elementTypeAdapter = gson.getAdapter(TypeToken.get(elementType));
      constructor = constructorConstructor.get(typeToken);
    } else {
      elementType = $Gson$Types.getCollectionElementType(type, rawType);
      elementTypeAdapter = gson.getAdapter(TypeToken.get(elementType));
      constructor = constructorConstructor.get(typeToken);
    }

    @SuppressWarnings({"unchecked", "rawtypes"}) // create() doesn't define a type parameter
    TypeAdapter<T> result = new Adapter(gson, elementType, elementTypeAdapter, constructor);
    return result;
  }

  private static final class Adapter<E> extends TypeAdapter<Collection<E>> {
    private final Gson context;
    private final TypeAdapter<E> elementTypeAdapter;
    private final ObjectConstructor<? extends Collection<E>> constructor;

    public Adapter(Gson context, Type elementType,
        TypeAdapter<E> elementTypeAdapter,
        ObjectConstructor<? extends Collection<E>> constructor) {
      this.context = context;
      this.elementTypeAdapter =
          new TypeAdapterRuntimeTypeWrapper<E>(context, elementTypeAdapter, elementType);
      this.constructor = constructor;
    }

    @Override public Collection<E> read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }

      Collection<E> collection = constructor.construct();
      in.beginArray();
      while (in.hasNext()) {
        if (context instanceof SuperGson) {
          collection.add(((SuperGson) context).<E>fromJson(in, null));
        } else {
          E instance = elementTypeAdapter.read(in);
          collection.add(instance);
        }
      }
      in.endArray();
      return collection;
    }

    @Override public void write(JsonWriter out, Collection<E> collection) throws IOException {
      if (collection == null) {
        out.nullValue();
        return;
      }

      out.beginArray();
      for (E element : collection) {
        elementTypeAdapter.write(out, element);
      }
      out.endArray();
    }
  }
}
