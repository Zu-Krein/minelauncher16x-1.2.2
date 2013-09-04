package net.minecraft.launcher.authentication;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Map;
import net.minecraft.launcher.authentication.AuthenticationService;
import net.minecraft.launcher.authentication.yggdrasil.YggdrasilAuthenticationService;

public class AuthenticationSerializer implements JsonDeserializer<AuthenticationService>, JsonSerializer<AuthenticationService> {

   public AuthenticationService deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      YggdrasilAuthenticationService result = new YggdrasilAuthenticationService();
      if(json == null) {
         return result;
      } else {
         Map map = (Map)context.deserialize(json, Map.class);
         result.loadFromStorage(map);
         return result;
      }
   }

   public JsonElement serialize(AuthenticationService src, Type typeOfSrc, JsonSerializationContext context) {
      Map map = src.saveForStorage();
      return map != null && !map.isEmpty()?context.serialize(map):null;
   }
/*
   // $FF: synthetic method
   // $FF: bridge method
   public JsonElement serialize(Object x0, Type x1, JsonSerializationContext x2) {
      return this.serialize((AuthenticationService)x0, x1, x2);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public Object deserialize(JsonElement x0, Type x1, JsonDeserializationContext x2) throws JsonParseException {
      return this.deserialize(x0, x1, x2);
   }*/
}
