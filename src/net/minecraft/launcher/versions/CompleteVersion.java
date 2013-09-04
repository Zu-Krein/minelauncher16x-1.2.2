package net.minecraft.launcher.versions;

import java.io.File;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.updater.download.Downloadable;
import net.minecraft.launcher.versions.Library;
import net.minecraft.launcher.versions.ReleaseType;
import net.minecraft.launcher.versions.Rule;
import net.minecraft.launcher.versions.Version;

public class CompleteVersion implements Version {

   private String id;
   private Date time;
   private Date releaseTime;
   private ReleaseType type;
   private String minecraftArguments;
   private List<Library> libraries;
   private String mainClass;
   private int minimumLauncherVersion;
   private String incompatibilityReason;
   private List<Rule> rules;
   private volatile boolean synced;


   public CompleteVersion() {
      this.synced = false;
   }

   public CompleteVersion(String id, Date releaseTime, Date updateTime, ReleaseType type, String mainClass, String minecraftArguments) {
      this.synced = false;
      if(id != null && id.length() != 0) {
         if(releaseTime == null) {
            throw new IllegalArgumentException("Release time cannot be null");
         } else if(updateTime == null) {
            throw new IllegalArgumentException("Update time cannot be null");
         } else if(type == null) {
            throw new IllegalArgumentException("Release type cannot be null");
         } else if(mainClass != null && mainClass.length() != 0) {
            if(minecraftArguments == null) {
               throw new IllegalArgumentException("Process arguments cannot be null or empty");
            } else {
               this.id = id;
               this.releaseTime = releaseTime;
               this.time = updateTime;
               this.type = type;
               this.mainClass = mainClass;
               this.libraries = new ArrayList();
               this.minecraftArguments = minecraftArguments;
            }
         } else {
            throw new IllegalArgumentException("Main class cannot be null or empty");
         }
      } else {
         throw new IllegalArgumentException("ID cannot be null or empty");
      }
   }

   public CompleteVersion(CompleteVersion version) {
      this(version.getId(), version.getReleaseTime(), version.getUpdatedTime(), version.getType(), version.getMainClass(), version.getMinecraftArguments());
   }

   public CompleteVersion(Version version, String mainClass, String minecraftArguments) {
      this(version.getId(), version.getReleaseTime(), version.getUpdatedTime(), version.getType(), mainClass, minecraftArguments);
   }

   public String getId() {
      return this.id;
   }

   public ReleaseType getType() {
      return this.type;
   }

   public Date getUpdatedTime() {
      return this.time;
   }

   public Date getReleaseTime() {
      return this.releaseTime;
   }

   public Collection<Library> getLibraries() {
      return this.libraries;
   }

   public String getMainClass() {
      return this.mainClass;
   }

   public void setUpdatedTime(Date time) {
      if(time == null) {
         throw new IllegalArgumentException("Time cannot be null");
      } else {
         this.time = time;
      }
   }

   public void setReleaseTime(Date time) {
      if(time == null) {
         throw new IllegalArgumentException("Time cannot be null");
      } else {
         this.releaseTime = time;
      }
   }

   public void setType(ReleaseType type) {
      if(type == null) {
         throw new IllegalArgumentException("Release type cannot be null");
      } else {
         this.type = type;
      }
   }

   public void setMainClass(String mainClass) {
      if(mainClass != null && mainClass.length() != 0) {
         this.mainClass = mainClass;
      } else {
         throw new IllegalArgumentException("Main class cannot be null or empty");
      }
   }

   public Collection<Library> getRelevantLibraries() {
      ArrayList result = new ArrayList();
      Iterator i$ = this.libraries.iterator();

      while(i$.hasNext()) {
         Library library = (Library)i$.next();
         if(library.appliesToCurrentEnvironment()) {
            result.add(library);
         }
      }

      return result;
   }

   public Collection<File> getClassPath(OperatingSystem os, File base) {
      Collection libraries = this.getRelevantLibraries();
      ArrayList result = new ArrayList();
      Iterator i$ = libraries.iterator();

      while(i$.hasNext()) {
         Library library = (Library)i$.next();
         if(library.getNatives() == null) {
            result.add(new File(base, "libraries/" + library.getArtifactPath()));
         }
      }

      result.add(new File(base, "versions/" + this.getId() + "/" + this.getId() + ".jar"));
      return result;
   }

   public Collection<String> getExtractFiles(OperatingSystem os) {
      Collection libraries = this.getRelevantLibraries();
      ArrayList result = new ArrayList();
      Iterator i$ = libraries.iterator();

      while(i$.hasNext()) {
         Library library = (Library)i$.next();
         Map natives = library.getNatives();
         if(natives != null && natives.containsKey(os)) {
            result.add("libraries/" + library.getArtifactPath((String)natives.get(os)));
         }
      }

      return result;
   }

   public Set<String> getRequiredFiles(OperatingSystem os) {
      HashSet neededFiles = new HashSet();
      Iterator i$ = this.getRelevantLibraries().iterator();

      while(i$.hasNext()) {
         Library library = (Library)i$.next();
         if(library.getNatives() != null) {
            String natives = (String)library.getNatives().get(os);
            if(natives != null) {
               neededFiles.add("libraries/" + library.getArtifactPath(natives));
            }
         } else {
            neededFiles.add("libraries/" + library.getArtifactPath());
         }
      }

      return neededFiles;
   }

   public Set<Downloadable> getRequiredDownloadables(OperatingSystem os, Proxy proxy, File targetDirectory, boolean ignoreLocalFiles) throws MalformedURLException {
      HashSet neededFiles = new HashSet();
      Iterator i$ = this.getRelevantLibraries().iterator();

      while(i$.hasNext()) {
         Library library = (Library)i$.next();
         String file = null;
         if(library.getNatives() != null) {
            String url = (String)library.getNatives().get(os);
            if(url != null) {
               file = library.getArtifactPath(url);
            }
         } else {
            file = library.getArtifactPath();
         }

         if(file != null) {
            URL url1 = new URL(library.getDownloadUrl() + file);
            File local = new File(targetDirectory, "libraries/" + file);
            if(!local.isFile() || !library.hasCustomUrl()) {
               neededFiles.add(new Downloadable(proxy, url1, local, ignoreLocalFiles));
            }
         }
      }

      return neededFiles;
   }

   public String toString() {
      return "CompleteVersion{id=\'" + this.id + '\'' + ", time=" + this.time + ", type=" + this.type + ", libraries=" + this.libraries + ", mainClass=\'" + this.mainClass + '\'' + ", minimumLauncherVersion=" + this.minimumLauncherVersion + '}';
   }

   public String getMinecraftArguments() {
      return this.minecraftArguments;
   }

   public void setMinecraftArguments(String minecraftArguments) {
      if(minecraftArguments == null) {
         throw new IllegalArgumentException("Process arguments cannot be null or empty");
      } else {
         this.minecraftArguments = minecraftArguments;
      }
   }

   public int getMinimumLauncherVersion() {
      return this.minimumLauncherVersion;
   }

   public void setMinimumLauncherVersion(int minimumLauncherVersion) {
      this.minimumLauncherVersion = minimumLauncherVersion;
   }

   public boolean appliesToCurrentEnvironment() {
      if(this.rules == null) {
         return true;
      } else {
         Rule.Action lastAction = Rule.Action.DISALLOW;
         Iterator i$ = this.rules.iterator();

         while(i$.hasNext()) {
            Rule rule = (Rule)i$.next();
            Rule.Action action = rule.getAppliedAction();
            if(action != null) {
               lastAction = action;
            }
         }

         return lastAction == Rule.Action.ALLOW;
      }
   }

   public String getIncompatibilityReason() {
      return this.incompatibilityReason;
   }

   public boolean isSynced() {
      return this.synced;
   }

   public void setSynced(boolean synced) {
      this.synced = synced;
   }
}
