package net.minecraft.launcher.authentication;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.swing.SwingUtilities;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.authentication.AuthenticationService;
import net.minecraft.launcher.authentication.GameProfile;
import net.minecraft.launcher.events.AuthenticationChangedListener;
import org.apache.commons.lang3.StringUtils;

public abstract class BaseAuthenticationService implements AuthenticationService {

   private static final String LEGACY_LASTLOGIN_PASSWORD = "passwordfile";
   private static final int LEGACY_LASTLOGIN_SEED = 43287234;
   private final List<AuthenticationChangedListener> listeners = new ArrayList();
   private String username;
   private String password;
   private GameProfile selectedProfile;
   private boolean shouldRememberMe = true;


   public boolean canLogIn() {
      return !this.canPlayOnline() && StringUtils.isNotBlank(this.getUsername()) && StringUtils.isNotBlank(this.getPassword());
   }

   public void logOut() {
      this.password = null;
      this.setSelectedProfile((GameProfile)null);
   }

   public boolean isLoggedIn() {
      return this.getSelectedProfile() != null;
   }

   public boolean canPlayOnline() {
      return this.isLoggedIn() && this.getSelectedProfile() != null && this.getSessionToken() != null;
   }

   public void addAuthenticationChangedListener(AuthenticationChangedListener listener) {
      this.listeners.add(listener);
   }

   public void removeAuthenticationChangedListener(AuthenticationChangedListener listener) {
      this.listeners.remove(listener);
   }

   protected void fireAuthenticationChangedEvent() {
      final ArrayList listeners = new ArrayList(this.listeners);
      Iterator iterator = listeners.iterator();

      while(iterator.hasNext()) {
         AuthenticationChangedListener listener = (AuthenticationChangedListener)iterator.next();
         if(!listener.shouldReceiveEventsInUIThread()) {
            listener.onAuthenticationChanged(this);
            iterator.remove();
         }
      }

      if(!listeners.isEmpty()) {
         SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               Iterator i$ = listeners.iterator();

               while(i$.hasNext()) {
                  AuthenticationChangedListener listener = (AuthenticationChangedListener)i$.next();
                  listener.onAuthenticationChanged(BaseAuthenticationService.this);
               }

            }
         });
      }

   }

   public void setUsername(String username) {
      if(this.isLoggedIn() && this.canPlayOnline()) {
         throw new IllegalStateException("Cannot change username whilst logged in & online");
      } else {
         this.username = username;
      }
   }

   public void setPassword(String password) {
      if(this.isLoggedIn() && this.canPlayOnline() && StringUtils.isNotBlank(password)) {
         throw new IllegalStateException("Cannot set password whilst logged in & online");
      } else {
         this.password = password;
      }
   }

   public String getUsername() {
      return this.username;
   }

   protected String getPassword() {
      return this.password;
   }

   public void loadFromStorage(Map<String, String> credentials) {
      this.logOut();
      if(credentials.containsKey("rememberMe")) {
         this.setRememberMe(Boolean.getBoolean((String)credentials.get("rememberMe")));
      }

      this.setUsername((String)credentials.get("username"));
      if(credentials.containsKey("displayName") && credentials.containsKey("uuid")) {
         this.setSelectedProfile(new GameProfile((String)credentials.get("uuid"), (String)credentials.get("displayName")));
      }

   }

   public Map<String, String> saveForStorage() {
      HashMap result = new HashMap();
      if(!this.shouldRememberMe()) {
         result.put("rememberMe", Boolean.toString(false));
         return result;
      } else {
         if(this.getUsername() != null) {
            result.put("username", this.getUsername());
         }

         if(this.getSelectedProfile() != null) {
            result.put("displayName", this.getSelectedProfile().getName());
            result.put("uuid", this.getSelectedProfile().getId());
         }

         return result;
      }
   }

   public boolean shouldRememberMe() {
      return this.shouldRememberMe;
   }

   public void setRememberMe(boolean rememberMe) {
      this.shouldRememberMe = rememberMe;
   }

   protected void setSelectedProfile(GameProfile selectedProfile) {
      this.selectedProfile = selectedProfile;
   }

   public GameProfile getSelectedProfile() {
      return this.selectedProfile;
   }

   public String toString() {
      StringBuilder result = new StringBuilder();
      result.append(this.getClass().getSimpleName());
      result.append("{");
      if(this.isLoggedIn()) {
         result.append("Logged in as ");
         result.append(this.getUsername());
         if(this.getSelectedProfile() != null) {
            result.append(" / ");
            result.append(this.getSelectedProfile());
            result.append(" - ");
            if(this.canPlayOnline()) {
               result.append("Online with session token \'");
               result.append(this.getSessionToken());
               result.append("\'");
            } else {
               result.append("Offline");
            }
         }
      } else {
         result.append("Not logged in");
      }

      result.append("}");
      return result.toString();
   }

   public String guessPasswordFromSillyOldFormat(File file) {
      String[] details = getStoredDetails(file);
      return details != null && details[0].equals(this.getUsername())?details[1]:null;
   }

   public static String[] getStoredDetails(File lastLoginFile) {
      if(!lastLoginFile.isFile()) {
         return null;
      } else {
         try {
            Cipher e = getCipher(2, "passwordfile");
            DataInputStream dis;
            if(e != null) {
               dis = new DataInputStream(new CipherInputStream(new FileInputStream(lastLoginFile), e));
            } else {
               dis = new DataInputStream(new FileInputStream(lastLoginFile));
            }

            String username = dis.readUTF();
            String password = dis.readUTF();
            dis.close();
            return new String[]{username, password};
         } catch (Exception var5) {
            Launcher.getInstance().println("Couldn\'t load old lastlogin file", var5);
            return null;
         }
      }
   }

   private static Cipher getCipher(int mode, String password) throws Exception {
      Random random = new Random(43287234L);
      byte[] salt = new byte[8];
      random.nextBytes(salt);
      PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, 5);
      SecretKey pbeKey = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(new PBEKeySpec(password.toCharArray()));
      Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
      cipher.init(mode, pbeKey, pbeParamSpec);
      return cipher;
   }
}
