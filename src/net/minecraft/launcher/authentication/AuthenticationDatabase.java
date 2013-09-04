package net.minecraft.launcher.authentication;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.launcher.authentication.AuthenticationService;
import net.minecraft.launcher.authentication.GameProfile;
import net.minecraft.launcher.authentication.yggdrasil.YggdrasilAuthenticationService;

public class AuthenticationDatabase {

   public static final String DEMO_UUID_PREFIX = "demo-";
   private final Map<String, AuthenticationService> authById;


   public AuthenticationDatabase() {
      this(new HashMap());
   }

   public AuthenticationDatabase(Map<String, AuthenticationService> authById) {
      this.authById = authById;
   }

   public AuthenticationService getByName(String name) {
      if(name == null) {
         return null;
      } else {
         Iterator i$ = this.authById.entrySet().iterator();

         Entry entry;
         GameProfile profile;
         do {
            if(!i$.hasNext()) {
               return null;
            }

            entry = (Entry)i$.next();
            profile = ((AuthenticationService)entry.getValue()).getSelectedProfile();
            if(profile != null && profile.getName().equals(name)) {
               return (AuthenticationService)entry.getValue();
            }
         } while(profile != null || !getUserFromDemoUUID((String)entry.getKey()).equals(name));

         return (AuthenticationService)entry.getValue();
      }
   }

   public AuthenticationService getByUUID(String uuid) {
      return (AuthenticationService)this.authById.get(uuid);
   }

   public Collection<String> getKnownNames() {
      ArrayList names = new ArrayList();
      Iterator i$ = this.authById.entrySet().iterator();

      while(i$.hasNext()) {
         Entry entry = (Entry)i$.next();
         GameProfile profile = ((AuthenticationService)entry.getValue()).getSelectedProfile();
         if(profile != null) {
            names.add(profile.getName());
         } else {
            names.add(getUserFromDemoUUID((String)entry.getKey()));
         }
      }

      return names;
   }

   public void register(String uuid, AuthenticationService authentication) {
      this.authById.put(uuid, authentication);
   }

   public Set<String> getknownUUIDs() {
      return this.authById.keySet();
   }

   public void removeUUID(String uuid) {
      this.authById.remove(uuid);
   }

   public static String getUserFromDemoUUID(String uuid) {
      return uuid.startsWith("demo-") && uuid.length() > "demo-".length()?"Demo User " + uuid.substring("demo-".length()):"Demo User";
   }

   public static class Serializer implements JsonDeserializer<AuthenticationDatabase>, JsonSerializer<AuthenticationDatabase> {

      public AuthenticationDatabase deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
         TypeToken token = new TypeToken() {
         };
         HashMap services = new HashMap();
         Map credentials = (Map)context.deserialize(json, token.getType());
         Iterator i$ = credentials.entrySet().iterator();

         while(i$.hasNext()) {
            Entry entry = (Entry)i$.next();
            YggdrasilAuthenticationService service = new YggdrasilAuthenticationService();
            service.loadFromStorage((Map)entry.getValue());
            services.put(entry.getKey(), service);
         }

         return new AuthenticationDatabase(services);
      }

      public JsonElement serialize(AuthenticationDatabase src, Type typeOfSrc, JsonSerializationContext context) {
         Map services = src.authById;
         HashMap credentials = new HashMap();
         Iterator i$ = services.entrySet().iterator();

         while(i$.hasNext()) {
            Entry entry = (Entry)i$.next();
            credentials.put(entry.getKey(), ((AuthenticationService)entry.getValue()).saveForStorage());
         }

         return context.serialize(credentials);
      }
/*
      // $FF: synthetic method
      // $FF: bridge method
      public Object deserialize(JsonElement x0, Type x1, JsonDeserializationContext x2) throws JsonParseException {
         return this.deserialize(x0, x1, x2);
      }

      // $FF: synthetic method
      // $FF: bridge method
      public JsonElement serialize(Object x0, Type x1, JsonSerializationContext x2) {
         return this.serialize((AuthenticationDatabase)x0, x1, x2);
      }*/
   }
}
