package net.minecraft.launcher.authentication;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import net.minecraft.hopper.Util;
import net.minecraft.launcher.Http;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.authentication.BaseAuthenticationService;
import net.minecraft.launcher.authentication.GameProfile;
import net.minecraft.launcher.authentication.exceptions.AuthenticationException;
import net.minecraft.launcher.authentication.exceptions.InvalidCredentialsException;
import org.apache.commons.lang3.StringUtils;

public class LegacyAuthenticationService extends BaseAuthenticationService {

   private static final URL AUTHENTICATION_URL = Util.constantURL("https://login.minecraft.net");
   private static final int AUTHENTICATION_VERSION = 14;
   private static final int RESPONSE_PART_PROFILE_NAME = 2;
   private static final int RESPONSE_PART_SESSION_TOKEN = 3;
   private static final int RESPONSE_PART_PROFILE_ID = 4;
   private String sessionToken;


   public void logIn() throws AuthenticationException {
      if(StringUtils.isBlank(this.getUsername())) {
         throw new InvalidCredentialsException("Invalid username");
      } else if(StringUtils.isBlank(this.getPassword())) {
         throw new InvalidCredentialsException("Invalid password");
      } else {
         HashMap args = new HashMap();
         args.put("user", this.getUsername());
         args.put("password", this.getPassword());
         args.put("version", Integer.valueOf(14));

         String response;
         try {
            response = Http.performPost(AUTHENTICATION_URL, args, Launcher.getInstance().getProxy()).trim();
         } catch (IOException var7) {
            throw new AuthenticationException("Authentication server is not responding", var7);
         }

         String[] split = response.split(":");
         if(split.length == 5) {
            String profileId = split[4];
            String profileName = split[2];
            String sessionToken = split[3];
            if(!StringUtils.isBlank(profileId) && !StringUtils.isBlank(profileName) && !StringUtils.isBlank(sessionToken)) {
               this.setSelectedProfile(new GameProfile(profileId, profileName));
               this.sessionToken = sessionToken;
               this.fireAuthenticationChangedEvent();
            } else {
               throw new AuthenticationException("Unknown response from authentication server: " + response);
            }
         } else {
            throw new InvalidCredentialsException(response);
         }
      }
   }

   public void logOut() {
      super.logOut();
      this.sessionToken = null;
      this.fireAuthenticationChangedEvent();
   }

   public GameProfile[] getAvailableProfiles() {
      return this.getSelectedProfile() != null?new GameProfile[]{this.getSelectedProfile()}:new GameProfile[0];
   }

   public void selectGameProfile(GameProfile profile) throws AuthenticationException {
      throw new UnsupportedOperationException("Game profiles cannot be changed in the legacy authentication service");
   }

   public String getSessionToken() {
      return this.sessionToken;
   }

}
