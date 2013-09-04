package net.minecraft.launcher.updater;

import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.updater.FileBasedVersionList;
import net.minecraft.launcher.versions.CompleteVersion;
import net.minecraft.launcher.versions.ReleaseType;
import net.minecraft.launcher.versions.Version;

public class LocalVersionList extends FileBasedVersionList {

   private final File baseDirectory;
   private final File baseVersionsDir;


   public LocalVersionList(File baseDirectory) {
      if(baseDirectory != null && baseDirectory.isDirectory()) {
         this.baseDirectory = baseDirectory;
         this.baseVersionsDir = new File(this.baseDirectory, "versions");
         if(!this.baseVersionsDir.isDirectory()) {
            this.baseVersionsDir.mkdirs();
         }

      } else {
         throw new IllegalArgumentException("Base directory is not a folder!");
      }
   }

   protected InputStream getFileInputStream(String path) throws FileNotFoundException {
      return new FileInputStream(new File(this.baseDirectory, path));
   }

   public void refreshVersions() throws IOException {
      this.clearCache();
      File[] files = this.baseVersionsDir.listFiles();
      if(files != null) {
         File[] i$ = files;
         int version = files.length;

         for(int type = 0; type < version; ++type) {
            File directory = i$[type];
            String id = directory.getName();
            File jsonFile = new File(directory, id + ".json");
            if(directory.isDirectory() && jsonFile.exists()) {
               try {
                  String ex = "versions/" + id + "/" + id + ".json";
                  CompleteVersion version1 = (CompleteVersion)this.gson.fromJson(this.getContent(ex), CompleteVersion.class);
                  if(version1.getId().equals(id)) {
                     this.addVersion(version1);
                  } else if(Launcher.getInstance() != null) {
                     Launcher.getInstance().println("Ignoring: " + ex + "; it contains id: \'" + version1.getId() + "\' expected \'" + id + "\'");
                  }
               } catch (RuntimeException var10) {
                  if(Launcher.getInstance() == null) {
                     throw new JsonSyntaxException("Loading file: " + jsonFile.toString(), var10);
                  }

                  Launcher.getInstance().println("Couldn\'t load local version " + jsonFile.getAbsolutePath(), var10);
               }
            }
         }

         Iterator var11 = this.getVersions().iterator();

         while(var11.hasNext()) {
            Version var12 = (Version)var11.next();
            ReleaseType var13 = var12.getType();
            if(this.getLatestVersion(var13) == null || this.getLatestVersion(var13).getUpdatedTime().before(var12.getUpdatedTime())) {
               this.setLatestVersion(var12);
            }
         }

      }
   }

   public void saveVersionList() throws IOException {
      String text = this.serializeVersionList();
      PrintWriter writer = new PrintWriter(new File(this.baseVersionsDir, "versions.json"));
      writer.print(text);
      writer.close();
   }

   public void saveVersion(CompleteVersion version) throws IOException {
      String text = this.serializeVersion(version);
      File target = new File(this.baseVersionsDir, version.getId() + "/" + version.getId() + ".json");
      if(target.getParentFile() != null) {
         target.getParentFile().mkdirs();
      }

      PrintWriter writer = new PrintWriter(target);
      writer.print(text);
      writer.close();
   }

   public File getBaseDirectory() {
      return this.baseDirectory;
   }

   public boolean hasAllFiles(CompleteVersion version, OperatingSystem os) {
      Set files = version.getRequiredFiles(os);
      Iterator i$ = files.iterator();

      String file;
      do {
         if(!i$.hasNext()) {
            return true;
         }

         file = (String)i$.next();
      } while((new File(this.baseDirectory, file)).isFile());

      return false;
   }
}
