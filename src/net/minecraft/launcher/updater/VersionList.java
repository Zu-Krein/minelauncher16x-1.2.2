package net.minecraft.launcher.updater;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.updater.DateTypeAdapter;
import net.minecraft.launcher.updater.LowerCaseEnumTypeAdapterFactory;
import net.minecraft.launcher.versions.CompleteVersion;
import net.minecraft.launcher.versions.PartialVersion;
import net.minecraft.launcher.versions.ReleaseType;
import net.minecraft.launcher.versions.Version;

public abstract class VersionList {

   protected final Gson gson;
   private final Map<String, Version> versionsByName = new HashMap();
   private final List<Version> versions = new ArrayList();
   private final Map<ReleaseType, Version> latestVersions = new EnumMap(ReleaseType.class);


   public VersionList() {
      GsonBuilder builder = new GsonBuilder();
      builder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
      builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
      builder.enableComplexMapKeySerialization();
      builder.setPrettyPrinting();
      this.gson = builder.create();
   }

   public Collection<Version> getVersions() {
      return this.versions;
   }

   public Version getLatestVersion(ReleaseType type) {
      if(type == null) {
         throw new IllegalArgumentException("Type cannot be null");
      } else {
         return (Version)this.latestVersions.get(type);
      }
   }

   public Version getVersion(String name) {
      if(name != null && name.length() != 0) {
         return (Version)this.versionsByName.get(name);
      } else {
         throw new IllegalArgumentException("Name cannot be null or empty");
      }
   }

   public CompleteVersion getCompleteVersion(String name) throws IOException {
      if(name != null && name.length() != 0) {
         Version version = this.getVersion(name);
         if(version == null) {
            throw new IllegalArgumentException("Unknown version - cannot get complete version of null");
         } else {
            return this.getCompleteVersion(version);
         }
      } else {
         throw new IllegalArgumentException("Name cannot be null or empty");
      }
   }

   public CompleteVersion getCompleteVersion(Version version) throws IOException {
      if(version instanceof CompleteVersion) {
         return (CompleteVersion)version;
      } else if(version == null) {
         throw new IllegalArgumentException("Version cannot be null");
      } else {
         CompleteVersion complete = (CompleteVersion)this.gson.fromJson(this.getContent("versions/" + version.getId() + "/" + version.getId() + ".json"), CompleteVersion.class);
         ReleaseType type = version.getType();
         Collections.replaceAll(this.versions, version, complete);
         this.versionsByName.put(version.getId(), complete);
         if(this.latestVersions.get(type) == version) {
            this.latestVersions.put(type, complete);
         }

         return complete;
      }
   }

   protected void clearCache() {
      this.versionsByName.clear();
      this.versions.clear();
      this.latestVersions.clear();
   }

   public void refreshVersions() throws IOException {
      this.clearCache();
      VersionList.RawVersionList versionList = (VersionList.RawVersionList)this.gson.fromJson(this.getContent("versions/versions.json"), VersionList.RawVersionList.class);
      Iterator arr$ = versionList.getVersions().iterator();

      while(arr$.hasNext()) {
         PartialVersion len$ = (PartialVersion)arr$.next();
         this.versions.add(len$);
         this.versionsByName.put(len$.getId(), len$);
      }

      ReleaseType[] var6 = ReleaseType.values();
      int var7 = var6.length;

      for(int i$ = 0; i$ < var7; ++i$) {
         ReleaseType type = var6[i$];
         this.latestVersions.put(type, this.versionsByName.get(versionList.getLatestVersions().get(type)));
      }

   }

   public CompleteVersion addVersion(CompleteVersion version) {
      if(version.getId() == null) {
         throw new IllegalArgumentException("Cannot add blank version");
      } else if(this.getVersion(version.getId()) != null) {
         throw new IllegalArgumentException("Version \'" + version.getId() + "\' is already tracked");
      } else {
         this.versions.add(version);
         this.versionsByName.put(version.getId(), version);
         return version;
      }
   }

   public void removeVersion(String name) {
      if(name != null && name.length() != 0) {
         Version version = this.getVersion(name);
         if(version == null) {
            throw new IllegalArgumentException("Unknown version - cannot remove null");
         } else {
            this.removeVersion(version);
         }
      } else {
         throw new IllegalArgumentException("Name cannot be null or empty");
      }
   }

   public void removeVersion(Version version) {
      if(version == null) {
         throw new IllegalArgumentException("Cannot remove null version");
      } else {
         this.versions.remove(version);
         this.versionsByName.remove(version.getId());
         ReleaseType[] arr$ = ReleaseType.values();
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            ReleaseType type = arr$[i$];
            if(this.getLatestVersion(type) == version) {
               this.latestVersions.remove(type);
            }
         }

      }
   }

   public void setLatestVersion(Version version) {
      if(version == null) {
         throw new IllegalArgumentException("Cannot set latest version to null");
      } else {
         this.latestVersions.put(version.getType(), version);
      }
   }

   public void setLatestVersion(String name) {
      if(name != null && name.length() != 0) {
         Version version = this.getVersion(name);
         if(version == null) {
            throw new IllegalArgumentException("Unknown version - cannot set latest version to null");
         } else {
            this.setLatestVersion(version);
         }
      } else {
         throw new IllegalArgumentException("Name cannot be null or empty");
      }
   }

   public String serializeVersionList() {
      VersionList.RawVersionList list = new VersionList.RawVersionList((VersionList.NamelessClass1458021669)null);
      ReleaseType[] i$ = ReleaseType.values();
      int version = i$.length;

      for(int partial = 0; partial < version; ++partial) {
         ReleaseType type = i$[partial];
         Version latest = this.getLatestVersion(type);
         if(latest != null) {
            list.getLatestVersions().put(type, latest.getId());
         }
      }

      PartialVersion var9;
      for(Iterator var7 = this.getVersions().iterator(); var7.hasNext(); list.getVersions().add(var9)) {
         Version var8 = (Version)var7.next();
         var9 = null;
         if(var8 instanceof PartialVersion) {
            var9 = (PartialVersion)var8;
         } else {
            var9 = new PartialVersion(var8);
         }
      }

      return this.gson.toJson((Object)list);
   }

   public String serializeVersion(CompleteVersion version) {
      if(version == null) {
         throw new IllegalArgumentException("Cannot serialize null!");
      } else {
         return this.gson.toJson((Object)version);
      }
   }

   public abstract boolean hasAllFiles(CompleteVersion var1, OperatingSystem var2);

   protected abstract String getContent(String var1) throws IOException;

   // $FF: synthetic class
   static class NamelessClass1458021669 {
   }

   private static class RawVersionList {

      private List<PartialVersion> versions;
      private Map<ReleaseType, String> latest;


      private RawVersionList() {
         this.versions = new ArrayList();
         this.latest = new EnumMap(ReleaseType.class);
      }

      public List<PartialVersion> getVersions() {
         return this.versions;
      }

      public Map<ReleaseType, String> getLatestVersions() {
         return this.latest;
      }

      // $FF: synthetic method
      RawVersionList(VersionList.NamelessClass1458021669 x0) {
         this();
      }
   }
}
