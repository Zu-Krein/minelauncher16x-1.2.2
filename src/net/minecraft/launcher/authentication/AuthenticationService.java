package net.minecraft.launcher.authentication;

import java.io.File;
import java.util.Map;
import net.minecraft.launcher.authentication.GameProfile;
import net.minecraft.launcher.authentication.exceptions.AuthenticationException;
import net.minecraft.launcher.events.AuthenticationChangedListener;

public interface AuthenticationService {

   String STORAGE_KEY_PROFILE_NAME = "displayName";
   String STORAGE_KEY_PROFILE_ID = "uuid";
   String STORAGE_KEY_USERNAME = "username";
   String STORAGE_KEY_REMEMBER_ME = "rememberMe";


   boolean canLogIn();

   void logIn() throws AuthenticationException;

   void logOut();

   boolean isLoggedIn();

   boolean canPlayOnline();

   GameProfile[] getAvailableProfiles();

   GameProfile getSelectedProfile();

   void selectGameProfile(GameProfile var1) throws AuthenticationException;

   void loadFromStorage(Map<String, String> var1);

   Map<String, String> saveForStorage();

   String getSessionToken();

   String getUsername();

   void setUsername(String var1);

   void setPassword(String var1);

   void addAuthenticationChangedListener(AuthenticationChangedListener var1);

   void removeAuthenticationChangedListener(AuthenticationChangedListener var1);

   String guessPasswordFromSillyOldFormat(File var1);

   void setRememberMe(boolean var1);

   boolean shouldRememberMe();
}
