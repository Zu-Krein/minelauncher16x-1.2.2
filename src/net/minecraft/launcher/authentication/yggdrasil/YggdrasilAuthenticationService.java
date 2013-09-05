package net.minecraft.launcher.authentication.yggdrasil;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import net.minecraft.hopper.Util;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.authentication.BaseAuthenticationService;
import net.minecraft.launcher.authentication.GameProfile;
import net.minecraft.launcher.authentication.exceptions.AuthenticationException;
import net.minecraft.launcher.authentication.exceptions.InvalidCredentialsException;
import net.minecraft.launcher.authentication.exceptions.UserMigratedException;
import net.minecraft.launcher.authentication.yggdrasil.Agent;
import net.minecraft.launcher.authentication.yggdrasil.AuthenticationRequest;
import net.minecraft.launcher.authentication.yggdrasil.AuthenticationResponse;
import net.minecraft.launcher.authentication.yggdrasil.RefreshRequest;
import net.minecraft.launcher.authentication.yggdrasil.RefreshResponse;
import net.minecraft.launcher.authentication.yggdrasil.Response;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class YggdrasilAuthenticationService extends BaseAuthenticationService {

   private static final String BASE_URL = "https://authserver.mojang.com/";
   private static final URL ROUTE_AUTHENTICATE = Util.constantURL(LauncherConstants.ROUTE_AUTHENTICATE);
   private static final URL ROUTE_REFRESH = Util.constantURL(LauncherConstants.ROUTE_REFRESH);
   private static final URL ROUTE_VALIDATE = Util.constantURL("https://authserver.mojang.com/validate");
   private static final URL ROUTE_INVALIDATE = Util.constantURL("https://authserver.mojang.com/invalidate");
   private static final URL ROUTE_SIGNOUT = Util.constantURL("https://authserver.mojang.com/signout");
   private static final String STORAGE_KEY_ACCESS_TOKEN = "accessToken";
   private final Gson gson = new Gson();
   private final Agent agent;
   private GameProfile[] profiles;
   private String accessToken;
   private boolean isOnline;


   public YggdrasilAuthenticationService() {
      this.agent = Agent.MINECRAFT;
   }

   public boolean canLogIn() {
      return !this.canPlayOnline() && StringUtils.isNotBlank(this.getUsername()) && (StringUtils.isNotBlank(this.getPassword()) || StringUtils.isNotBlank(this.getAccessToken()));
   }

   public void logIn() throws AuthenticationException {
      if(StringUtils.isBlank(this.getUsername())) {
         throw new InvalidCredentialsException("Invalid username");
      } else {
         if(StringUtils.isNotBlank(this.getAccessToken())) {
            this.logInWithToken();
         } else {
            if(!StringUtils.isNotBlank(this.getPassword())) {
               throw new InvalidCredentialsException("Invalid password");
            }

            this.logInWithPassword();
         }

      }
   }

   protected void logInWithPassword() throws AuthenticationException {
      if(StringUtils.isBlank(this.getUsername())) {
         throw new InvalidCredentialsException("Invalid username");
      } else if(StringUtils.isBlank(this.getPassword())) {
         throw new InvalidCredentialsException("Invalid password");
      } else {
         Launcher.getInstance().println("Logging in with username & password");
         AuthenticationRequest request = new AuthenticationRequest(this, this.getPassword());
         AuthenticationResponse response = (AuthenticationResponse)this.makeRequest(ROUTE_AUTHENTICATE, request, AuthenticationResponse.class);
         if(!response.getClientToken().equals(this.getClientToken())) {
            throw new AuthenticationException("Server requested we change our client token. Don\'t know how to handle this!");
         } else {
            this.accessToken = response.getAccessToken();
            this.profiles = response.getAvailableProfiles();
            this.setSelectedProfile(response.getSelectedProfile());
            this.fireAuthenticationChangedEvent();
         }
      }
   }

   protected void logInWithToken() throws AuthenticationException {
      if(StringUtils.isBlank(this.getUsername())) {
         throw new InvalidCredentialsException("Invalid username");
      } else if(StringUtils.isBlank(this.getAccessToken())) {
         throw new InvalidCredentialsException("Invalid access token");
      } else {
         Launcher.getInstance().println("Logging in with access token");
         RefreshRequest request = new RefreshRequest(this);
         RefreshResponse response = (RefreshResponse)this.makeRequest(ROUTE_REFRESH, request, RefreshResponse.class);
         if(!response.getClientToken().equals(this.getClientToken())) {
            throw new AuthenticationException("Server requested we change our client token. Don\'t know how to handle this!");
         } else {
            this.accessToken = response.getAccessToken();
            this.profiles = response.getAvailableProfiles();
            this.setSelectedProfile(response.getSelectedProfile());
            this.fireAuthenticationChangedEvent();
         }
      }
   }

   protected <T extends Response> Response makeRequest(URL url, Object input, Class<T> classOfT) throws AuthenticationException {
      try {
         String e = Util.performPost(url, this.gson.toJson(input), Launcher.getInstance().getProxy(), "application/json", true);
         Response result = (Response)this.gson.fromJson(e, classOfT);
         if(result == null) {
            return null;
         } else if(StringUtils.isNotBlank(result.getError())) {
            if("UserMigratedException".equals(result.getCause())) {
               throw new UserMigratedException(result.getErrorMessage());
            } else if(result.getError().equals("ForbiddenOperationException")) {
               throw new InvalidCredentialsException(result.getErrorMessage());
            } else {
               throw new AuthenticationException(result.getErrorMessage());
            }
         } else {
            this.isOnline = true;
            return result;
         }
      } catch (IOException var6) {
         throw new AuthenticationException("Cannot contact authentication server", var6);
      } catch (IllegalStateException var7) {
         throw new AuthenticationException("Cannot contact authentication server", var7);
      } catch (JsonParseException var8) {
         throw new AuthenticationException("Cannot contact authentication server", var8);
      }
   }

   public void logOut() {
      super.logOut();
      this.accessToken = null;
      this.profiles = null;
      this.isOnline = false;
   }

   public GameProfile[] getAvailableProfiles() {
      return this.profiles;
   }

   public boolean isLoggedIn() {
      return StringUtils.isNotBlank(this.accessToken);
   }

   public boolean canPlayOnline() {
      return this.isLoggedIn() && this.getSelectedProfile() != null && this.isOnline;
   }

   public void selectGameProfile(GameProfile profile) throws AuthenticationException {
      if(!this.isLoggedIn()) {
         throw new AuthenticationException("Cannot change game profile whilst not logged in");
      } else if(this.getSelectedProfile() != null) {
         throw new AuthenticationException("Cannot change game profile. You must log out and back in.");
      } else if(profile != null && ArrayUtils.contains(this.profiles, profile)) {
         RefreshRequest request = new RefreshRequest(this, profile);
         RefreshResponse response = (RefreshResponse)this.makeRequest(ROUTE_REFRESH, request, RefreshResponse.class);
         if(!response.getClientToken().equals(this.getClientToken())) {
            throw new AuthenticationException("Server requested we change our client token. Don\'t know how to handle this!");
         } else {
            this.accessToken = response.getAccessToken();
            this.setSelectedProfile(response.getSelectedProfile());
            this.fireAuthenticationChangedEvent();
         }
      } else {
         throw new IllegalArgumentException("Invalid profile \'" + profile + "\'");
      }
   }

   public void loadFromStorage(Map<String, String> credentials) {
      super.loadFromStorage(credentials);
      this.accessToken = (String)credentials.get("accessToken");
   }

   public Map<String, String> saveForStorage() {
      Map result = super.saveForStorage();
      if(!this.shouldRememberMe()) {
         return result;
      } else {
         if(StringUtils.isNotBlank(this.getAccessToken())) {
            result.put("accessToken", this.getAccessToken());
         }

         return result;
      }
   }

   public String getSessionToken() {
      return this.isLoggedIn() && this.getSelectedProfile() != null && this.canPlayOnline()?String.format("token:%s:%s", new Object[]{this.getAccessToken(), this.getSelectedProfile().getId()}):null;
   }

   public String getAccessToken() {
      return this.accessToken;
   }

   public String getClientToken() {
      return Launcher.getInstance().getClientToken().toString();
   }

   public Agent getAgent() {
      return this.agent;
   }

   public String toString() {
      return "YggdrasilAuthenticationService{agent=" + this.agent + ", profiles=" + Arrays.toString(this.profiles) + ", selectedProfile=" + this.getSelectedProfile() + ", sessionToken=\'" + this.getSessionToken() + '\'' + ", username=\'" + this.getUsername() + '\'' + ", isLoggedIn=" + this.isLoggedIn() + ", canPlayOnline=" + this.canPlayOnline() + ", accessToken=\'" + this.accessToken + '\'' + ", clientToken=\'" + this.getClientToken() + '\'' + '}';
   }

}
