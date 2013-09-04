package net.minecraft.launcher.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.SwingUtilities;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.authentication.AuthenticationDatabase;
import net.minecraft.launcher.events.RefreshedProfilesListener;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.updater.DateTypeAdapter;
import net.minecraft.launcher.updater.FileTypeAdapter;
import net.minecraft.launcher.updater.LowerCaseEnumTypeAdapterFactory;
import org.apache.commons.io.FileUtils;

public class ProfileManager {

   public static final String DEFAULT_PROFILE_NAME = "(Default)";
   private final Launcher launcher;
   private final Gson gson;
   private final Map<String, Profile> profiles = new HashMap();
   private final File profileFile;
   private final List<RefreshedProfilesListener> refreshedProfilesListeners = Collections.synchronizedList(new ArrayList());
   private String selectedProfile;
   private AuthenticationDatabase authDatabase = new AuthenticationDatabase();


   public ProfileManager(Launcher launcher) {
      this.launcher = launcher;
      this.profileFile = new File(launcher.getWorkingDirectory(), "launcher_profiles.json");
      GsonBuilder builder = new GsonBuilder();
      builder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
      builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
      builder.registerTypeAdapter(File.class, new FileTypeAdapter());
      builder.registerTypeAdapter(AuthenticationDatabase.class, new AuthenticationDatabase.Serializer());
      builder.setPrettyPrinting();
      this.gson = builder.create();
   }

   public void saveProfiles() throws IOException {
      ProfileManager.RawProfileList rawProfileList = new ProfileManager.RawProfileList(null);
      rawProfileList.profiles = this.profiles;
      rawProfileList.selectedProfile = this.getSelectedProfile().getName();
      rawProfileList.clientToken = this.launcher.getClientToken();
      rawProfileList.authenticationDatabase = this.authDatabase;
      FileUtils.writeStringToFile(this.profileFile, this.gson.toJson((Object)rawProfileList));
   }

   public boolean loadProfiles() throws IOException {
      this.profiles.clear();
      this.selectedProfile = null;
      if(this.profileFile.isFile()) {
         ProfileManager.RawProfileList rawProfileList = (ProfileManager.RawProfileList)this.gson.fromJson(FileUtils.readFileToString(this.profileFile), ProfileManager.RawProfileList.class);
         this.profiles.putAll(rawProfileList.profiles);
         this.selectedProfile = rawProfileList.selectedProfile;
         this.authDatabase = rawProfileList.authenticationDatabase;
         this.launcher.setClientToken(rawProfileList.clientToken);
         this.fireRefreshEvent();
         return true;
      } else {
         this.fireRefreshEvent();
         return false;
      }
   }

   public void fireRefreshEvent() {
      final ArrayList listeners = new ArrayList(this.refreshedProfilesListeners);
      Iterator iterator = listeners.iterator();

      while(iterator.hasNext()) {
         RefreshedProfilesListener listener = (RefreshedProfilesListener)iterator.next();
         if(!listener.shouldReceiveEventsInUIThread()) {
            listener.onProfilesRefreshed(this);
            iterator.remove();
         }
      }

      if(!listeners.isEmpty()) {
         SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               Iterator i$ = listeners.iterator();

               while(i$.hasNext()) {
                  RefreshedProfilesListener listener = (RefreshedProfilesListener)i$.next();
                  listener.onProfilesRefreshed(ProfileManager.this);
               }

            }
         });
      }

   }

   public Profile getSelectedProfile() {
      if(this.selectedProfile == null || !this.profiles.containsKey(this.selectedProfile)) {
         if(this.profiles.get("(Default)") != null) {
            this.selectedProfile = "(Default)";
         } else if(this.profiles.size() > 0) {
            this.selectedProfile = ((Profile)this.profiles.values().iterator().next()).getName();
         } else {
            this.selectedProfile = "(Default)";
            this.profiles.put("(Default)", new Profile(this.selectedProfile));
         }
      }

      return (Profile)this.profiles.get(this.selectedProfile);
   }

   public Map<String, Profile> getProfiles() {
      return this.profiles;
   }

   public Launcher getLauncher() {
      return this.launcher;
   }

   public void addRefreshedProfilesListener(RefreshedProfilesListener listener) {
      this.refreshedProfilesListeners.add(listener);
   }

   public void setSelectedProfile(String selectedProfile) {
      boolean update = !this.selectedProfile.equals(selectedProfile);
      this.selectedProfile = selectedProfile;
      if(update) {
         this.fireRefreshEvent();
      }

   }

   public AuthenticationDatabase getAuthDatabase() {
      return this.authDatabase;
   }

   public void trimAuthDatabase() {
      HashSet uuids = new HashSet(this.authDatabase.getknownUUIDs());
      Iterator i$ = this.profiles.values().iterator();

      while(i$.hasNext()) {
         Profile uuid = (Profile)i$.next();
         uuids.remove(uuid.getPlayerUUID());
      }

      i$ = uuids.iterator();

      while(i$.hasNext()) {
         String uuid1 = (String)i$.next();
         this.authDatabase.removeUUID(uuid1);
      }

   }

   private static class RawProfileList {

      public Map<String, Profile> profiles;
      public String selectedProfile;
      public UUID clientToken;
      public AuthenticationDatabase authenticationDatabase;


      private RawProfileList() {
         this.profiles = new HashMap();
         this.clientToken = UUID.randomUUID();
         this.authenticationDatabase = new AuthenticationDatabase();
      }

      // $FF: synthetic method
      RawProfileList(Object x0) {
         this();
      }
   }
}
