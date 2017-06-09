package com.nachtraben.lemonslice;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by NachtRaben on 4/20/2017.
 */
public abstract class JsonProperties implements CustomJsonIO {

    @Override
    public JsonElement write() {
        JsonObject jo = new JsonObject();
        for(Field field : getClass().getDeclaredFields()) {
            if(field.isAnnotationPresent(Property.class)) {
                try {
                    field.setAccessible(true);
                    Property property = field.getAnnotation(Property.class);
                    Type type = field.getType();
                    Object o = field.get(this);
                    jo.add(property.name(), ConfigurationUtils.GSON_P.toJsonTree(o, type));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return jo;
    }

    @Override
    public void read(JsonElement me) {
        if (me instanceof JsonObject) {
            JsonObject jo = me.getAsJsonObject();
            List<Field> fields = new ArrayList<>();
            Collections.addAll(fields, getClass().getDeclaredFields());
            Class s = getClass();
            while((s = s.getSuperclass()) != null && !s.equals(Object.class))
                Collections.addAll(fields, s.getDeclaredFields());
            for (Field field : fields) {
                if (field.isAnnotationPresent(Property.class)) {
                    try {
                        Property property = field.getAnnotation(Property.class);
                        Type fieldType = field.getGenericType();
                        Type annotType = property.type();
                        field.setAccessible(true);
                        if(annotType == Void.class) {
                            field.set(this, ConfigurationUtils.GSON_P.fromJson(jo.get(property.name()), fieldType));
                        } else {
                            field.set(this, ConfigurationUtils.GSON_P.fromJson(jo.get(property.name()), annotType));
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
