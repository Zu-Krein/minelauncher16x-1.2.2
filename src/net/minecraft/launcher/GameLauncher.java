package net.minecraft.launcher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.authentication.AuthenticationService;
import net.minecraft.launcher.process.JavaProcess;
import net.minecraft.launcher.process.JavaProcessLauncher;
import net.minecraft.launcher.process.JavaProcessRunnable;
import net.minecraft.launcher.profile.LauncherVisibilityRule;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.ui.tabs.CrashReportTab;
import net.minecraft.launcher.updater.LocalVersionList;
import net.minecraft.launcher.updater.VersionList;
import net.minecraft.launcher.updater.VersionSyncInfo;
import net.minecraft.launcher.updater.download.DownloadJob;
import net.minecraft.launcher.updater.download.DownloadListener;
import net.minecraft.launcher.updater.download.Downloadable;
import net.minecraft.launcher.versions.CompleteVersion;
import net.minecraft.launcher.versions.ExtractRules;
import net.minecraft.launcher.versions.Library;
import net.minecraft.launcher.versions.Version;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.lang3.text.StrSubstitutor;

public class GameLauncher implements JavaProcessRunnable, DownloadListener {

   private final Object lock = new Object();
   private final Launcher launcher;
   private final List<DownloadJob> jobs = new ArrayList();
   private CompleteVersion version;
   private LauncherVisibilityRule visibilityRule;
   private boolean isWorking;
   private File nativeDir;


   public GameLauncher(Launcher launcher) {
      this.launcher = launcher;
   }

   private void setWorking(boolean working) {
      Object var2 = this.lock;
      synchronized(this.lock) {
         if(this.nativeDir != null) {
            Launcher.getInstance().println("Deleting " + this.nativeDir);
            if(this.nativeDir.isDirectory() && !FileUtils.deleteQuietly(this.nativeDir)) {
               Launcher.getInstance().println("Couldn\'t delete " + this.nativeDir + " - scheduling for deletion upon exit");

               try {
                  FileUtils.forceDeleteOnExit(this.nativeDir);
               } catch (Throwable var5) {
                  ;
               }
            } else {
               this.nativeDir = null;
            }
         }

         this.isWorking = working;
         SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               GameLauncher.this.launcher.getLauncherPanel().getBottomBar().getPlayButtonPanel().checkState();
            }
         });
      }
   }

   public boolean isWorking() {
      return this.isWorking;
   }

   public void playGame() {
      Object profile = this.lock;
      synchronized(this.lock) {
         if(this.isWorking) {
            this.launcher.println("Tried to play game but game is already starting!");
            return;
         }

         this.setWorking(true);
      }

      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            GameLauncher.this.launcher.getLauncherPanel().getTabPanel().showConsole();
         }
      });
      this.launcher.println("Getting syncinfo for selected version");
      Profile profile1 = this.launcher.getProfileManager().getSelectedProfile();
      String lastVersionId = profile1.getLastVersionId();
      VersionSyncInfo syncInfo = null;
      if(profile1.getLauncherVisibilityOnGameClose() == null) {
         this.visibilityRule = Profile.DEFAULT_LAUNCHER_VISIBILITY;
      } else {
         this.visibilityRule = profile1.getLauncherVisibilityOnGameClose();
      }

      if(lastVersionId != null) {
         syncInfo = this.launcher.getVersionManager().getVersionSyncInfo(lastVersionId);
      }

      if(syncInfo == null || syncInfo.getLatestVersion() == null) {
         syncInfo = (VersionSyncInfo)this.launcher.getVersionManager().getVersions(profile1.getVersionFilter()).get(0);
      }

      if(syncInfo == null) {
         Launcher.getInstance().println("Tried to launch a version without a version being selected...");
         this.setWorking(false);
      } else {
         Object var4 = this.lock;
         synchronized(this.lock) {
            this.launcher.println("Queueing library & version downloads");

            try {
               this.version = this.launcher.getVersionManager().getLatestCompleteVersion(syncInfo);
            } catch (IOException var10) {
               Launcher.getInstance().println("Couldn\'t get complete version info for " + syncInfo.getLatestVersion(), var10);
               this.setWorking(false);
               return;
            }

            if(syncInfo.getRemoteVersion() != null && syncInfo.getLatestSource() != VersionSyncInfo.VersionSource.REMOTE && !this.version.isSynced()) {
               try {
                  CompleteVersion e = this.launcher.getVersionManager().getRemoteVersionList().getCompleteVersion(syncInfo.getRemoteVersion());
                  this.launcher.getVersionManager().getLocalVersionList().removeVersion((Version)this.version);
                  this.launcher.getVersionManager().getLocalVersionList().addVersion(e);
                  ((LocalVersionList)this.launcher.getVersionManager().getLocalVersionList()).saveVersion(e);
                  this.version = e;
               } catch (IOException var9) {
                  Launcher.getInstance().println("Couldn\'t sync local and remote versions", var9);
               }

               this.version.setSynced(true);
            }

            if(!this.version.appliesToCurrentEnvironment()) {
               String e2 = this.version.getIncompatibilityReason();
               if(e2 == null) {
                  e2 = "This version is incompatible with your computer. Please try another one by going into Edit Profile and selecting one through the dropdown. Sorry!";
               }

               Launcher.getInstance().println("Version " + this.version.getId() + " is incompatible with current environment: " + e2);
               JOptionPane.showMessageDialog(this.launcher.getFrame(), e2, "Cannot play game", 0);
               this.setWorking(false);
            } else if(this.version.getMinimumLauncherVersion() > 7) {
               Launcher.getInstance().println("An update to your launcher is available and is required to play " + this.version.getId() + ". Please restart your launcher.");
               this.setWorking(false);
            } else {
               if(!syncInfo.isInstalled()) {
                  try {
                     VersionList e1 = this.launcher.getVersionManager().getLocalVersionList();
                     if(e1 instanceof LocalVersionList) {
                        ((LocalVersionList)e1).saveVersion(this.version);
                        Launcher.getInstance().println("Installed " + syncInfo.getLatestVersion());
                     }
                  } catch (IOException var8) {
                     Launcher.getInstance().println("Couldn\'t save version info to install " + syncInfo.getLatestVersion(), var8);
                     this.setWorking(false);
                     return;
                  }
               }

               try {
                  DownloadJob e3 = new DownloadJob("Version & Libraries", false, this);
                  this.addJob(e3);
                  this.launcher.getVersionManager().downloadVersion(syncInfo, e3);
                  e3.startDownloading(this.launcher.getVersionManager().getExecutorService());
               } catch (IOException var7) {
                  Launcher.getInstance().println("Couldn\'t get version info for " + syncInfo.getLatestVersion(), var7);
                  this.setWorking(false);
                  return;
               }

            }
         }
      }
   }

   protected void launchGame() {
      this.launcher.println("Launching game");
      Profile selectedProfile = this.launcher.getProfileManager().getSelectedProfile();
      if(this.version == null) {
         Launcher.getInstance().println("Aborting launch; version is null?");
      } else {
         this.cleanOldNatives();
         this.nativeDir = new File(this.launcher.getWorkingDirectory(), "versions/" + this.version.getId() + "/" + this.version.getId() + "-natives-" + System.nanoTime());
         if(!this.nativeDir.isDirectory()) {
            this.nativeDir.mkdirs();
         }

         this.launcher.println("Unpacking natives to " + this.nativeDir);

         try {
            this.unpackNatives(this.version, this.nativeDir);
         } catch (IOException var17) {
            Launcher.getInstance().println("Couldn\'t unpack natives!", var17);
            return;
         }

         File gameDirectory = selectedProfile.getGameDir() == null?this.launcher.getWorkingDirectory():selectedProfile.getGameDir();
         Launcher.getInstance().println("Launching in " + gameDirectory);
         if(!gameDirectory.exists()) {
            if(!gameDirectory.mkdirs()) {
               Launcher.getInstance().println("Aborting launch; couldn\'t create game directory");
               return;
            }
         } else if(!gameDirectory.isDirectory()) {
            Launcher.getInstance().println("Aborting launch; game directory is not actually a directory");
            return;
         }

         JavaProcessLauncher processLauncher = new JavaProcessLauncher(selectedProfile.getJavaPath(), new String[0]);
         processLauncher.directory(gameDirectory);
         File assetsDirectory = new File(this.launcher.getWorkingDirectory(), "assets");
         OperatingSystem os = OperatingSystem.getCurrentPlatform();
         if(os.equals(OperatingSystem.OSX)) {
            processLauncher.addCommands(new String[]{"-Xdock:icon=" + (new File(assetsDirectory, "icons/minecraft.icns")).getAbsolutePath(), "-Xdock:name=Minecraft"});
         } else if(os.equals(OperatingSystem.WINDOWS)) {
            processLauncher.addCommands(new String[]{"-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump"});
         }

         String profileArgs = selectedProfile.getJavaArgs();
         if(profileArgs != null) {
            processLauncher.addSplitCommands(profileArgs);
         } else {
            boolean auth = "32".equals(System.getProperty("sun.arch.data.model"));
            String args = auth?"-Xmx512M":"-Xmx1G";
            processLauncher.addSplitCommands(args);
         }

         processLauncher.addCommands(new String[]{"-Djava.library.path=" + this.nativeDir.getAbsolutePath()});
         processLauncher.addCommands(new String[]{"-cp", this.constructClassPath(this.version)});
         processLauncher.addCommands(new String[]{this.version.getMainClass()});
         AuthenticationService auth1 = this.launcher.getProfileManager().getAuthDatabase().getByUUID(selectedProfile.getPlayerUUID());
         String[] args1 = this.getMinecraftArguments(this.version, selectedProfile, gameDirectory, assetsDirectory, auth1);
         if(args1 != null) {
            processLauncher.addCommands(args1);
            Proxy proxy = this.launcher.getProxy();
            PasswordAuthentication proxyAuth = this.launcher.getProxyAuth();
            if(!proxy.equals(Proxy.NO_PROXY)) {
               InetSocketAddress e = (InetSocketAddress)proxy.address();
               processLauncher.addCommands(new String[]{"--proxyHost", e.getHostName()});
               processLauncher.addCommands(new String[]{"--proxyPort", Integer.toString(e.getPort())});
               if(proxyAuth != null) {
                  processLauncher.addCommands(new String[]{"--proxyUser", proxyAuth.getUserName()});
                  processLauncher.addCommands(new String[]{"--proxyPass", new String(proxyAuth.getPassword())});
               }
            }

            processLauncher.addCommands(this.launcher.getAdditionalArgs());
            if(auth1 == null || auth1.getSelectedProfile() == null) {
               processLauncher.addCommands(new String[]{"--demo"});
            }

            if(selectedProfile.getResolution() != null) {
               processLauncher.addCommands(new String[]{"--width", String.valueOf(selectedProfile.getResolution().getWidth())});
               processLauncher.addCommands(new String[]{"--height", String.valueOf(selectedProfile.getResolution().getHeight())});
            }

            try {
               List e1 = processLauncher.getFullCommands();
               StringBuilder full = new StringBuilder();
               boolean first = true;

               for(Iterator process = e1.iterator(); process.hasNext(); first = false) {
                  String part = (String)process.next();
                  if(!first) {
                     full.append(" ");
                  }

                  full.append(part);
               }

               Launcher.getInstance().println("Running " + full.toString());
               JavaProcess process1 = processLauncher.start();
               process1.safeSetExitRunnable(this);
               if(this.visibilityRule != LauncherVisibilityRule.DO_NOTHING) {
                  this.launcher.getFrame().setVisible(false);
               }

            } catch (IOException var16) {
               Launcher.getInstance().println("Couldn\'t launch game", var16);
               this.setWorking(false);
            }
         }
      }
   }

   private String[] getMinecraftArguments(CompleteVersion version, Profile selectedProfile, File gameDirectory, File assetsDirectory, AuthenticationService authentication) {
      if(version.getMinecraftArguments() == null) {
         Launcher.getInstance().println("Can\'t run version, missing minecraftArguments");
         this.setWorking(false);
         return null;
      } else {
         HashMap map = new HashMap();
         StrSubstitutor substitutor = new StrSubstitutor(map);
         String[] split = version.getMinecraftArguments().split(" ");
         map.put("auth_username", authentication.getUsername());
         map.put("auth_session", authentication.getSessionToken() == null && authentication.canPlayOnline()?"-":authentication.getSessionToken());
         if(authentication.getSelectedProfile() != null) {
            map.put("auth_player_name", authentication.getSelectedProfile().getName());
            map.put("auth_uuid", authentication.getSelectedProfile().getId());
         } else {
            map.put("auth_player_name", "Player");
            map.put("auth_uuid", (new UUID(0L, 0L)).toString());
         }

         map.put("profile_name", selectedProfile.getName());
         map.put("version_name", version.getId());
         map.put("game_directory", gameDirectory.getAbsolutePath());
         map.put("game_assets", assetsDirectory.getAbsolutePath());

         for(int i = 0; i < split.length; ++i) {
            split[i] = substitutor.replace(split[i]);
         }

         return split;
      }
   }

   /*
   private void cleanOldNatives() {
      File root = new File(this.launcher.getWorkingDirectory(), "versions/");
      this.launcher.println("Looking for old natives to clean up...");
      AgeFileFilter ageFilter = new AgeFileFilter(System.currentTimeMillis() - 3600L);
      File[] arr$ = root.listFiles(DirectoryFileFilter.DIRECTORY);
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         File version = arr$[i$];
         File[] arr$1 = version.listFiles(FileFilterUtils.and(new IOFileFilter[]{new PrefixFileFilter(version.getName() + "-natives-"), ageFilter}));
         int len$1 = arr$1.length;

         for(int i$1 = 0; i$1 < len$1; ++i$1) {
            File folder = arr$1[i$1];
            Launcher.getInstance().println("Deleting " + folder);
            FileUtils.deleteQuietly(folder);
         }
      }

   }
*/
      private void cleanOldNatives() {
      File root = new File(this.launcher.getWorkingDirectory(), "versions/");
      this.launcher.println("Looking for old natives to clean up...");
      AgeFileFilter ageFilter = new AgeFileFilter(System.currentTimeMillis() - 3600L);
      File[] arr$;
        arr$ = root.listFiles((FileFilter)DirectoryFileFilter.DIRECTORY);
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         File version = arr$[i$];
         File[] arr$1 = version.listFiles((FileFilter)FileFilterUtils.and(new IOFileFilter[]{new PrefixFileFilter(version.getName() + "-natives-"), ageFilter}));
         int len$1 = arr$1.length;

         for(int i$1 = 0; i$1 < len$1; ++i$1) {
            File folder = arr$1[i$1];
            Launcher.getInstance().println("Deleting " + folder);
            FileUtils.deleteQuietly(folder);
         }
      }

   }

   private void unpackNatives(CompleteVersion version, File targetDir) throws IOException {
      OperatingSystem os = OperatingSystem.getCurrentPlatform();
      Collection libraries = version.getRelevantLibraries();
      Iterator i$ = libraries.iterator();

      while(i$.hasNext()) {
         Library library = (Library)i$.next();
         Map nativesPerOs = library.getNatives();
         if(nativesPerOs != null && nativesPerOs.get(os) != null) {
            File file = new File(this.launcher.getWorkingDirectory(), "libraries/" + library.getArtifactPath((String)nativesPerOs.get(os)));
            ZipFile zip = new ZipFile(file);
            ExtractRules extractRules = library.getExtractRules();

            try {
               Enumeration entries = zip.entries();

               while(entries.hasMoreElements()) {
                  ZipEntry entry = (ZipEntry)entries.nextElement();
                  if(extractRules == null || extractRules.shouldExtract(entry.getName())) {
                     File targetFile = new File(targetDir, entry.getName());
                     if(targetFile.getParentFile() != null) {
                        targetFile.getParentFile().mkdirs();
                     }

                     if(!entry.isDirectory()) {
                        BufferedInputStream inputStream = new BufferedInputStream(zip.getInputStream(entry));
                        byte[] buffer = new byte[2048];
                        FileOutputStream outputStream = new FileOutputStream(targetFile);
                        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

                        int length;
                        try {
                           while((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
                              bufferedOutputStream.write(buffer, 0, length);
                           }
                        } finally {
                           Downloadable.closeSilently(bufferedOutputStream);
                           Downloadable.closeSilently(outputStream);
                           Downloadable.closeSilently(inputStream);
                        }
                     }
                  }
               }
            } finally {
               zip.close();
            }
         }
      }

   }

   private String constructClassPath(CompleteVersion version) {
      StringBuilder result = new StringBuilder();
      Collection classPath = version.getClassPath(OperatingSystem.getCurrentPlatform(), this.launcher.getWorkingDirectory());
      String separator = System.getProperty("path.separator");

      File file;
      for(Iterator i$ = classPath.iterator(); i$.hasNext(); result.append(file.getAbsolutePath())) {
         file = (File)i$.next();
         if(!file.isFile()) {
            throw new RuntimeException("Classpath file not found: " + file);
         }

         if(result.length() > 0) {
            result.append(separator);
         }
      }

      return result.toString();
   }

   public void onJavaProcessEnded(JavaProcess process) {
      int exitCode = process.getExitCode();
      if(exitCode == 0) {
         Launcher.getInstance().println("Game ended with no troubles detected (exit code " + exitCode + ")");
         if(this.visibilityRule == LauncherVisibilityRule.CLOSE_LAUNCHER) {
            SwingUtilities.invokeLater(new Runnable() {
               public void run() {
                  GameLauncher.this.launcher.println("Following visibility rule and exiting launcher as the game has ended");
                  GameLauncher.this.launcher.closeLauncher();
               }
            });
         } else if(this.visibilityRule == LauncherVisibilityRule.HIDE_LAUNCHER) {
            SwingUtilities.invokeLater(new Runnable() {
               public void run() {
                  GameLauncher.this.launcher.println("Following visibility rule and showing launcher as the game has ended");
                  GameLauncher.this.launcher.getFrame().setVisible(true);
               }
            });
         }
      } else {
         Launcher.getInstance().println("Game ended with bad state (exit code " + exitCode + ")");
         SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               GameLauncher.this.launcher.println("Ignoring visibility rule and showing launcher due to a game crash");
               GameLauncher.this.launcher.getFrame().setVisible(true);
            }
         });
         String errorText = null;
         String[] sysOut = (String[])process.getSysOutLines().getItems();

         for(int file = sysOut.length - 1; file >= 0; --file) {
            String inputStream = sysOut[file];
            String e = "#@!@#";
            int result = inputStream.lastIndexOf(e);
            if(result >= 0 && result < inputStream.length() - e.length() - 1) {
               errorText = inputStream.substring(result + e.length()).trim();
               break;
            }
         }

         if(errorText != null) {
            File var15 = new File(errorText);
            if(var15.isFile()) {
               Launcher.getInstance().println("Crash report detected, opening: " + errorText);
               FileInputStream var16 = null;

               try {
                  var16 = new FileInputStream(var15);
                  BufferedReader var17 = new BufferedReader(new InputStreamReader(var16));

                  String line;
                  StringBuilder var18;
                  for(var18 = new StringBuilder(); (line = var17.readLine()) != null; var18.append(line)) {
                     if(var18.length() > 0) {
                        var18.append("\n");
                     }
                  }

                  var17.close();
                  this.launcher.getLauncherPanel().getTabPanel().setCrashReport(new CrashReportTab(this.launcher, this.version, var15, var18.toString()));
               } catch (IOException var13) {
                  Launcher.getInstance().println("Couldn\'t open crash report", var13);
               } finally {
                  Downloadable.closeSilently(var16);
               }
            } else {
               Launcher.getInstance().println("Crash report detected, but unknown format: " + errorText);
            }
         }
      }

      this.setWorking(false);
   }

   public void onDownloadJobFinished(DownloadJob job) {
      this.updateProgressBar();
      Object var2 = this.lock;
      synchronized(this.lock) {
         if(job.getFailures() > 0) {
            this.launcher.println("Job \'" + job.getName() + "\' finished with " + job.getFailures() + " failure(s)!");
            this.setWorking(false);
         } else {
            this.launcher.println("Job \'" + job.getName() + "\' finished successfully");
            if(this.isWorking() && !this.hasRemainingJobs()) {
               try {
                  this.launchGame();
               } catch (Throwable var5) {
                  Launcher.getInstance().println("Fatal error launching game. Report this to http://mojang.atlassian.net please!", var5);
               }
            }
         }

      }
   }

   public void onDownloadJobProgressChanged(DownloadJob job) {
      this.updateProgressBar();
   }

   protected void updateProgressBar() {
      final float progress = this.getProgress();
      final boolean hasTasks = this.hasRemainingJobs();
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            GameLauncher.this.launcher.getLauncherPanel().getProgressBar().setVisible(hasTasks);
            GameLauncher.this.launcher.getLauncherPanel().getProgressBar().setValue((int)(progress * 100.0F));
         }
      });
   }

   protected float getProgress() {
      Object var1 = this.lock;
      synchronized(this.lock) {
         float max = 0.0F;
         float result = 0.0F;
         Iterator i$ = this.jobs.iterator();

         while(i$.hasNext()) {
            DownloadJob job = (DownloadJob)i$.next();
            float progress = job.getProgress();
            if(progress >= 0.0F) {
               result += progress;
               ++max;
            }
         }

         return result / max;
      }
   }

   public boolean hasRemainingJobs() {
      Object var1 = this.lock;
      synchronized(this.lock) {
         Iterator i$ = this.jobs.iterator();

         DownloadJob job;
         do {
            if(!i$.hasNext()) {
               return false;
            }

            job = (DownloadJob)i$.next();
         } while(job.isComplete());

         return true;
      }
   }

   public void addJob(DownloadJob job) {
      Object var2 = this.lock;
      synchronized(this.lock) {
         this.jobs.add(job);
      }
   }
}
