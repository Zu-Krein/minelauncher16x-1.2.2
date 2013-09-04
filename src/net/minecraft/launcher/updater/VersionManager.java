package net.minecraft.launcher.updater;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.events.RefreshedVersionsListener;
import net.minecraft.launcher.updater.ExceptionalThreadPoolExecutor;
import net.minecraft.launcher.updater.LocalVersionList;
import net.minecraft.launcher.updater.RemoteVersionList;
import net.minecraft.launcher.updater.VersionFilter;
import net.minecraft.launcher.updater.VersionList;
import net.minecraft.launcher.updater.VersionSyncInfo;
import net.minecraft.launcher.updater.download.DownloadJob;
import net.minecraft.launcher.updater.download.Downloadable;
import net.minecraft.launcher.versions.CompleteVersion;
import net.minecraft.launcher.versions.ReleaseType;
import net.minecraft.launcher.versions.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class VersionManager {

   private final VersionList localVersionList;
   private final VersionList remoteVersionList;
   private final ThreadPoolExecutor executorService = new ExceptionalThreadPoolExecutor(8);
   private final List<RefreshedVersionsListener> refreshedVersionsListeners = Collections.synchronizedList(new ArrayList());
   private final Object refreshLock = new Object();
   private boolean isRefreshing;


   public VersionManager(VersionList localVersionList, VersionList remoteVersionList) {
      this.localVersionList = localVersionList;
      this.remoteVersionList = remoteVersionList;
   }

   public void refreshVersions() throws IOException {
      Object listeners = this.refreshLock;
      synchronized(this.refreshLock) {
         this.isRefreshing = true;
      }

      try {
         Launcher.getInstance().println("Refreshing local version list...");
         this.localVersionList.refreshVersions();
         Launcher.getInstance().println("Refreshing remote version list...");
         this.remoteVersionList.refreshVersions();
      } catch (IOException var7) {
         Object iterator = this.refreshLock;
         synchronized(this.refreshLock) {
            this.isRefreshing = false;
         }

         throw var7;
      }

      Launcher.getInstance().println("Refresh complete.");
      listeners = this.refreshLock;
      synchronized(this.refreshLock) {
         this.isRefreshing = false;
      }

      final ArrayList listeners1 = new ArrayList(this.refreshedVersionsListeners);
      Iterator iterator1 = listeners1.iterator();

      while(iterator1.hasNext()) {
         RefreshedVersionsListener listener = (RefreshedVersionsListener)iterator1.next();
         if(!listener.shouldReceiveEventsInUIThread()) {
            listener.onVersionsRefreshed(this);
            iterator1.remove();
         }
      }

      if(!listeners1.isEmpty()) {
         SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               Iterator i$ = listeners1.iterator();

               while(i$.hasNext()) {
                  RefreshedVersionsListener listener = (RefreshedVersionsListener)i$.next();
                  listener.onVersionsRefreshed(VersionManager.this);
               }

            }
         });
      }

   }

   public List<VersionSyncInfo> getVersions() {
      return this.getVersions((VersionFilter)null);
   }

   public List<VersionSyncInfo> getVersions(VersionFilter filter) {
      Object result = this.refreshLock;
      synchronized(this.refreshLock) {
         if(this.isRefreshing) {
            return new ArrayList();
         }
      }

      ArrayList var10 = new ArrayList();
      HashMap lookup = new HashMap();
      EnumMap counts = new EnumMap(ReleaseType.class);
      ReleaseType[] i$ = ReleaseType.values();
      int version = i$.length;

      for(int syncInfo = 0; syncInfo < version; ++syncInfo) {
         ReleaseType type = i$[syncInfo];
         counts.put(type, Integer.valueOf(0));
      }

      Iterator var11 = this.localVersionList.getVersions().iterator();

      Version var12;
      VersionSyncInfo var13;
      while(var11.hasNext()) {
         var12 = (Version)var11.next();
         if(var12.getType() != null && var12.getUpdatedTime() != null && (filter == null || filter.getTypes().contains(var12.getType()) && ((Integer)counts.get(var12.getType())).intValue() < filter.getMaxCount())) {
            var13 = this.getVersionSyncInfo(var12, this.remoteVersionList.getVersion(var12.getId()));
            lookup.put(var12.getId(), var13);
            var10.add(var13);
         }
      }

      var11 = this.remoteVersionList.getVersions().iterator();

      while(var11.hasNext()) {
         var12 = (Version)var11.next();
         if(var12.getType() != null && var12.getUpdatedTime() != null && !lookup.containsKey(var12.getId()) && (filter == null || filter.getTypes().contains(var12.getType()) && ((Integer)counts.get(var12.getType())).intValue() < filter.getMaxCount())) {
            var13 = this.getVersionSyncInfo(this.localVersionList.getVersion(var12.getId()), var12);
            lookup.put(var12.getId(), var13);
            var10.add(var13);
            if(filter != null) {
               counts.put(var12.getType(), Integer.valueOf(((Integer)counts.get(var12.getType())).intValue() + 1));
            }
         }
      }

      if(var10.isEmpty()) {
         var11 = this.localVersionList.getVersions().iterator();

         while(var11.hasNext()) {
            var12 = (Version)var11.next();
            if(var12.getType() != null && var12.getUpdatedTime() != null) {
               var13 = this.getVersionSyncInfo(var12, this.remoteVersionList.getVersion(var12.getId()));
               lookup.put(var12.getId(), var13);
               var10.add(var13);
               break;
            }
         }
      }

      Collections.sort(var10, new Comparator() {
         public int compare(VersionSyncInfo a, VersionSyncInfo b) {
            Version aVer = a.getLatestVersion();
            Version bVer = b.getLatestVersion();
            return aVer.getReleaseTime() != null && bVer.getReleaseTime() != null?bVer.getReleaseTime().compareTo(aVer.getReleaseTime()):bVer.getUpdatedTime().compareTo(aVer.getUpdatedTime());
         }
         // $FF: synthetic method
         // $FF: bridge method
         public int compare(Object x0, Object x1) {
            return this.compare((VersionSyncInfo)x0, (VersionSyncInfo)x1);
         }
      });
      return var10;
   }

   public VersionSyncInfo getVersionSyncInfo(Version version) {
      return this.getVersionSyncInfo(version.getId());
   }

   public VersionSyncInfo getVersionSyncInfo(String name) {
      return this.getVersionSyncInfo(this.localVersionList.getVersion(name), this.remoteVersionList.getVersion(name));
   }

   public VersionSyncInfo getVersionSyncInfo(Version localVersion, Version remoteVersion) {
      boolean installed = localVersion != null;
      boolean upToDate = installed;
      if(installed && remoteVersion != null) {
         upToDate = !remoteVersion.getUpdatedTime().after(localVersion.getUpdatedTime());
      }

      if(localVersion instanceof CompleteVersion) {
         upToDate &= this.localVersionList.hasAllFiles((CompleteVersion)localVersion, OperatingSystem.getCurrentPlatform());
      }

      return new VersionSyncInfo(localVersion, remoteVersion, installed, upToDate);
   }

   public List<VersionSyncInfo> getInstalledVersions() {
      ArrayList result = new ArrayList();
      Iterator i$ = this.localVersionList.getVersions().iterator();

      while(i$.hasNext()) {
         Version version = (Version)i$.next();
         if(version.getType() != null && version.getUpdatedTime() != null) {
            VersionSyncInfo syncInfo = this.getVersionSyncInfo(version, this.remoteVersionList.getVersion(version.getId()));
            result.add(syncInfo);
         }
      }

      return result;
   }

   public VersionList getRemoteVersionList() {
      return this.remoteVersionList;
   }

   public VersionList getLocalVersionList() {
      return this.localVersionList;
   }

   public CompleteVersion getLatestCompleteVersion(VersionSyncInfo syncInfo) throws IOException {
      if(syncInfo.getLatestSource() == VersionSyncInfo.VersionSource.REMOTE) {
         CompleteVersion result = null;
         IOException exception = null;

         try {
            result = this.remoteVersionList.getCompleteVersion(syncInfo.getLatestVersion());
         } catch (IOException var7) {
            exception = var7;

            try {
               result = this.localVersionList.getCompleteVersion(syncInfo.getLatestVersion());
            } catch (IOException var6) {
               ;
            }
         }

         if(result != null) {
            return result;
         } else {
            throw exception;
         }
      } else {
         return this.localVersionList.getCompleteVersion(syncInfo.getLatestVersion());
      }
   }

   public DownloadJob downloadVersion(VersionSyncInfo syncInfo, DownloadJob job) throws IOException {
      if(!(this.localVersionList instanceof LocalVersionList)) {
         throw new IllegalArgumentException("Cannot download if local repo isn\'t a LocalVersionList");
      } else if(!(this.remoteVersionList instanceof RemoteVersionList)) {
         throw new IllegalArgumentException("Cannot download if local repo isn\'t a RemoteVersionList");
      } else {
         CompleteVersion version = this.getLatestCompleteVersion(syncInfo);
         File baseDirectory = ((LocalVersionList)this.localVersionList).getBaseDirectory();
         Proxy proxy = ((RemoteVersionList)this.remoteVersionList).getProxy();
         job.addDownloadables((Collection)version.getRequiredDownloadables(OperatingSystem.getCurrentPlatform(), proxy, baseDirectory, false));
         String jarFile = "versions/" + version.getId() + "/" + version.getId() + ".jar";
         job.addDownloadables(new Downloadable[]{new Downloadable(proxy, new URL("https://s3.amazonaws.com/Minecraft.Download/" + jarFile), new File(baseDirectory, jarFile), false)});
         return job;
      }
   }

   public DownloadJob downloadResources(DownloadJob job) throws IOException {
      File baseDirectory = ((LocalVersionList)this.localVersionList).getBaseDirectory();
      job.addDownloadables((Collection)this.getResourceFiles(((RemoteVersionList)this.remoteVersionList).getProxy(), baseDirectory));
      return job;
   }

   private Set<Downloadable> getResourceFiles(Proxy proxy, File baseDirectory) {
      HashSet result = new HashSet();

      try {
         URL ex = new URL("https://s3.amazonaws.com/Minecraft.Resources/");
         DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
         DocumentBuilder db = dbf.newDocumentBuilder();
         Document doc = db.parse(ex.openConnection(proxy).getInputStream());
         NodeList nodeLst = doc.getElementsByTagName("Contents");
         long start = System.nanoTime();

         for(int end = 0; end < nodeLst.getLength(); ++end) {
            Node node = nodeLst.item(end);
            if(node.getNodeType() == 1) {
               Element delta = (Element)node;
               String key = delta.getElementsByTagName("Key").item(0).getChildNodes().item(0).getNodeValue();
               String etag = delta.getElementsByTagName("ETag") != null?delta.getElementsByTagName("ETag").item(0).getChildNodes().item(0).getNodeValue():"-";
               long size = Long.parseLong(delta.getElementsByTagName("Size").item(0).getChildNodes().item(0).getNodeValue());
               if(size > 0L) {
                  File file = new File(baseDirectory, "assets/" + key);
                  if(etag.length() > 1) {
                     etag = Downloadable.getEtag(etag);
                     if(file.isFile() && file.length() == size) {
                        String downloadable = Downloadable.getMD5(file);
                        if(downloadable.equals(etag)) {
                           continue;
                        }
                     }
                  }

                  Downloadable var23 = new Downloadable(proxy, new URL("https://s3.amazonaws.com/Minecraft.Resources/" + key), file, false);
                  var23.setExpectedSize(size);
                  result.add(var23);
               }
            }
         }

         long var21 = System.nanoTime();
         long var22 = var21 - start;
         Launcher.getInstance().println("Delta time to compare resources: " + var22 / 1000000L + " ms ");
      } catch (Exception var20) {
         Launcher.getInstance().println("Couldn\'t download resources", var20);
      }

      return result;
   }

   public ThreadPoolExecutor getExecutorService() {
      return this.executorService;
   }

   public void addRefreshedVersionsListener(RefreshedVersionsListener listener) {
      this.refreshedVersionsListeners.add(listener);
   }

   public void removeRefreshedVersionsListener(RefreshedVersionsListener listener) {
      this.refreshedVersionsListeners.remove(listener);
   }
}
