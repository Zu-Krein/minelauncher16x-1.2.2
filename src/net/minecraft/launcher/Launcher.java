package net.minecraft.launcher;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import net.minecraft.launcher.GameLauncher;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.authentication.AuthenticationService;
import net.minecraft.launcher.authentication.exceptions.AuthenticationException;
import net.minecraft.launcher.authentication.exceptions.InvalidCredentialsException;
import net.minecraft.launcher.authentication.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.ui.LauncherPanel;
import net.minecraft.launcher.ui.popups.login.LogInPopup;
import net.minecraft.launcher.updater.LocalVersionList;
import net.minecraft.launcher.updater.RemoteVersionList;
import net.minecraft.launcher.updater.VersionManager;
import net.minecraft.launcher.updater.download.DownloadJob;

public class Launcher {

   private static Launcher instance;
   private static final List<String> delayedSysout = new ArrayList();
   private final VersionManager versionManager;
   private final JFrame frame;
   private final LauncherPanel launcherPanel;
   private final GameLauncher gameLauncher;
   private final File workingDirectory;
   private final Proxy proxy;
   private final PasswordAuthentication proxyAuth;
   private final String[] additionalArgs;
   private final Integer bootstrapVersion;
   private final ProfileManager profileManager;
   private UUID clientToken;


   public Launcher(JFrame frame, File workingDirectory, Proxy proxy, PasswordAuthentication proxyAuth, String[] args) {
      this(frame, workingDirectory, proxy, proxyAuth, args, Integer.valueOf(0));
   }

   public Launcher(JFrame frame, File workingDirectory, Proxy proxy, PasswordAuthentication proxyAuth, String[] args, Integer bootstrapVersion) {
      this.clientToken = UUID.randomUUID();
      this.bootstrapVersion = bootstrapVersion;
      instance = this;
      setLookAndFeel();
      this.proxy = proxy;
      this.proxyAuth = proxyAuth;
      this.additionalArgs = args;
      this.workingDirectory = workingDirectory;
      this.frame = frame;
      this.gameLauncher = new GameLauncher(this);
      this.profileManager = new ProfileManager(this);
      this.versionManager = new VersionManager(new LocalVersionList(workingDirectory), new RemoteVersionList(proxy));
      this.launcherPanel = new LauncherPanel(this);
      this.initializeFrame();
      Iterator i$ = delayedSysout.iterator();

      while(i$.hasNext()) {
         String line = (String)i$.next();
         this.launcherPanel.getTabPanel().getConsole().print(line + "\n");
      }

      if(bootstrapVersion.intValue() < 4) {
         this.showOutdatedNotice();
      } else {
         this.downloadResources();
         this.refreshVersionsAndProfiles();
         this.println("Launcher 1.2.2 (through bootstrap " + bootstrapVersion + ") started on " + OperatingSystem.getCurrentPlatform().getName() + "...");
         this.println("Current time is " + DateFormat.getDateTimeInstance(2, 2, Locale.US).format(new Date()));
         if(!OperatingSystem.getCurrentPlatform().isSupported()) {
            this.println("This operating system is unknown or unsupported, we cannot guarantee that the game will launch.");
         }

         this.println("System.getProperty(\'os.name\') == \'" + System.getProperty("os.name") + "\'");
         this.println("System.getProperty(\'os.version\') == \'" + System.getProperty("os.version") + "\'");
         this.println("System.getProperty(\'os.arch\') == \'" + System.getProperty("os.arch") + "\'");
         this.println("System.getProperty(\'java.version\') == \'" + System.getProperty("java.version") + "\'");
         this.println("System.getProperty(\'java.vendor\') == \'" + System.getProperty("java.vendor") + "\'");
         this.println("System.getProperty(\'sun.arch.data.model\') == \'" + System.getProperty("sun.arch.data.model") + "\'");
      }
   }

   private void showOutdatedNotice() {
      String error = "Sorry, but your launcher is outdated! Please redownload it at https://mojang.com/2013/06/minecraft-1-6-pre-release/";
      this.frame.getContentPane().removeAll();
      int result = JOptionPane.showOptionDialog(this.frame, error, "Outdated launcher", 0, 0, (Icon)null, LauncherConstants.BOOTSTRAP_OUT_OF_DATE_BUTTONS, LauncherConstants.BOOTSTRAP_OUT_OF_DATE_BUTTONS[0]);
      if(result == 0) {
         try {
            OperatingSystem.openLink(new URI("https://mojang.com/2013/06/minecraft-1-6-pre-release/"));
         } catch (URISyntaxException var4) {
            this.println("Couldn\'t open bootstrap download link. Please visit https://mojang.com/2013/06/minecraft-1-6-pre-release/ manually.", var4);
         }
      }

      this.closeLauncher();
   }

   private static void setLookAndFeel() {
      JFrame frame = new JFrame();

      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (Throwable var7) {
         try {
            getInstance().println("Your java failed to provide normal look and feel, trying the old fallback now");
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
         } catch (Throwable var6) {
            getInstance().println("Unexpected exception setting look and feel");
            var6.printStackTrace();
         }
      }

      JPanel panel = new JPanel();
      panel.setBorder(BorderFactory.createTitledBorder("test"));
      frame.add(panel);

      try {
         frame.pack();
      } catch (Throwable var5) {
         getInstance().println("Custom (broken) theme detected, falling back onto x-platform theme");

         try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
         } catch (Throwable var4) {
            getInstance().println("Unexpected exception setting look and feel", var4);
         }
      }

      frame.dispose();
   }

   private void downloadResources() {
      final DownloadJob job = new DownloadJob("Resources", true, this.gameLauncher);
      this.gameLauncher.addJob(job);
      this.versionManager.getExecutorService().submit(new Runnable() {
         public void run() {
            try {
               Launcher.this.versionManager.downloadResources(job);
               job.startDownloading(Launcher.this.versionManager.getExecutorService());
            } catch (IOException var2) {
               Launcher.instance.println("Unexpected exception queueing resource downloads", var2);
            }

         }
      });
   }

   public void refreshVersionsAndProfiles() {
      this.versionManager.getExecutorService().submit(new Runnable() {
         public void run() {
            try {
               Launcher.this.versionManager.refreshVersions();
            } catch (Throwable var3) {
               Launcher.instance.println("Unexpected exception refreshing version list", var3);
            }

            try {
               Launcher.this.profileManager.loadProfiles();
               Launcher.this.println("Loaded " + Launcher.this.profileManager.getProfiles().size() + " profile(s); selected \'" + Launcher.this.profileManager.getSelectedProfile().getName() + "\'");
            } catch (Throwable var2) {
               Launcher.instance.println("Unexpected exception refreshing profile list", var2);
            }

            Launcher.this.ensureLoggedIn();
         }
      });
   }

   public void ensureLoggedIn() {
      Profile selectedProfile = this.profileManager.getSelectedProfile();
      AuthenticationService auth = this.profileManager.getAuthDatabase().getByUUID(selectedProfile.getPlayerUUID());
      if(auth == null) {
         this.showLoginPrompt();
      } else if(!auth.isLoggedIn()) {
         if(auth.canLogIn()) {
            try {
               auth.logIn();

               try {
                  this.profileManager.saveProfiles();
               } catch (IOException var7) {
                  this.println("Couldn\'t save profiles after refreshing auth!", var7);
               }

               this.profileManager.fireRefreshEvent();
            } catch (AuthenticationException var8) {
               this.println((Throwable)var8);
               this.showLoginPrompt();
            }
         } else {
            this.showLoginPrompt();
         }
      } else if(!auth.canPlayOnline()) {
         try {
            this.println("Refreshing auth...");
            auth.logIn();

            try {
               this.profileManager.saveProfiles();
            } catch (IOException var4) {
               this.println("Couldn\'t save profiles after refreshing auth!", var4);
            }

            this.profileManager.fireRefreshEvent();
         } catch (InvalidCredentialsException var5) {
            this.println((Throwable)var5);
            this.showLoginPrompt();
         } catch (AuthenticationException var6) {
            this.println((Throwable)var6);
         }
      }

   }

   public void showLoginPrompt() {
      try {
         this.profileManager.saveProfiles();
      } catch (IOException var6) {
         this.println("Couldn\'t save profiles before logging in!", var6);
      }

      Iterator selectedProfile = this.profileManager.getProfiles().values().iterator();

      while(selectedProfile.hasNext()) {
         Profile profile = (Profile)selectedProfile.next();
         Map credentials = profile.getAuthentication();
         if(credentials != null) {
            YggdrasilAuthenticationService auth = new YggdrasilAuthenticationService();
            auth.loadFromStorage(credentials);
            if(auth.isLoggedIn()) {
               String uuid = auth.getSelectedProfile() == null?"demo-" + auth.getUsername():auth.getSelectedProfile().getId();
               if(this.profileManager.getAuthDatabase().getByUUID(uuid) == null) {
                  this.profileManager.getAuthDatabase().register(uuid, auth);
               }
            }

            profile.setAuthentication((Map)null);
         }
      }

      final Profile selectedProfile1 = this.profileManager.getSelectedProfile();
      LogInPopup.showLoginPrompt(this, new LogInPopup.Callback() {
         public void onLogIn(String uuid) {
            AuthenticationService auth = Launcher.this.profileManager.getAuthDatabase().getByUUID(uuid);
            selectedProfile1.setPlayerUUID(uuid);
            if(selectedProfile1.getName().equals("(Default)") && auth.getSelectedProfile() != null) {
               String e = auth.getSelectedProfile().getName();
               String profileName = auth.getSelectedProfile().getName();

               StringBuilder var10000;
               for(int count = 1; Launcher.this.profileManager.getProfiles().containsKey(profileName); profileName = var10000.append(count).toString()) {
                  var10000 = (new StringBuilder()).append(e).append(" ");
                  ++count;
               }

               Profile newProfile = new Profile(selectedProfile1);
               newProfile.setName(profileName);
               Launcher.this.profileManager.getProfiles().put(profileName, newProfile);
               Launcher.this.profileManager.getProfiles().remove("(Default)");
               Launcher.this.profileManager.setSelectedProfile(profileName);
            }

            try {
               Launcher.this.profileManager.saveProfiles();
            } catch (IOException var7) {
               Launcher.this.println("Couldn\'t save profiles after logging in!", var7);
            }

            if(uuid == null) {
               Launcher.this.closeLauncher();
            } else {
               Launcher.this.profileManager.fireRefreshEvent();
            }

            Launcher.this.launcherPanel.setCard("launcher", (JPanel)null);
         }
      });
   }

   public void closeLauncher() {
      this.frame.dispatchEvent(new WindowEvent(this.frame, 201));
   }

   protected void initializeFrame() {
      this.frame.getContentPane().removeAll();
      this.frame.setTitle("Minecraft Launcher 1.2.2");
      this.frame.setPreferredSize(new Dimension(900, 580));
      this.frame.setDefaultCloseOperation(2);
      this.frame.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            Launcher.this.frame.setVisible(false);
            Launcher.this.frame.dispose();
            Launcher.this.versionManager.getExecutorService().shutdown();
         }
      });

      try {
         InputStream in = Launcher.class.getResourceAsStream("/favicon.png");
         if(in != null) {
            this.frame.setIconImage(ImageIO.read(in));
         }
      } catch (IOException var2) {
         ;
      }

      this.frame.add(this.launcherPanel);
      this.frame.pack();
      this.frame.setVisible(true);
   }

   public VersionManager getVersionManager() {
      return this.versionManager;
   }

   public JFrame getFrame() {
      return this.frame;
   }

   public LauncherPanel getLauncherPanel() {
      return this.launcherPanel;
   }

   public GameLauncher getGameLauncher() {
      return this.gameLauncher;
   }

   public File getWorkingDirectory() {
      return this.workingDirectory;
   }

   public Proxy getProxy() {
      return this.proxy;
   }

   public PasswordAuthentication getProxyAuth() {
      return this.proxyAuth;
   }

   public String[] getAdditionalArgs() {
      return this.additionalArgs;
   }

   public void println(String line) {
      System.out.println(line);
      if(this.launcherPanel == null) {
         delayedSysout.add(line);
      } else {
         this.launcherPanel.getTabPanel().getConsole().print(line + "\n");
      }

   }

   public void println(String line, Throwable throwable) {
      this.println(line);
      this.println(throwable);
   }

   public void println(Throwable throwable) {
      StringWriter writer = null;
      PrintWriter printWriter = null;
      String result = throwable.toString();

      try {
         writer = new StringWriter();
         printWriter = new PrintWriter(writer);
         throwable.printStackTrace(printWriter);
         result = writer.toString();
      } finally {
         try {
            if(writer != null) {
               writer.close();
            }

            if(printWriter != null) {
               printWriter.close();
            }
         } catch (IOException var11) {
            ;
         }

      }

      this.println(result);
   }

   public int getBootstrapVersion() {
      return this.bootstrapVersion.intValue();
   }

   public static Launcher getInstance() {
      return instance;
   }

   public ProfileManager getProfileManager() {
      return this.profileManager;
   }

   public UUID getClientToken() {
      return this.clientToken;
   }

   public void setClientToken(UUID clientToken) {
      this.clientToken = clientToken;
   }

}
