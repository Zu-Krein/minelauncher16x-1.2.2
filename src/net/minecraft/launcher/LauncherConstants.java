package net.minecraft.launcher;

import java.net.URI;
import java.net.URISyntaxException;

public class LauncherConstants {

   public static final String BASE_URL = "http://webmcr.caver.org/";
   public static final String LAUNCHER_VERSION = "Minecraft Launcher 1.2.2";
   public static final String ROUTE_AUTHENTICATE = BASE_URL + "authenticate";
   public static final String ROUTE_REFRESH = BASE_URL + "refresh";
   public static final String ROUTE_INVALIDATE = BASE_URL + "invalidate";
   public static final String SERVER_DOWNLOAD_URL = BASE_URL + "MineCraft/MinecraftDownload/";
   public static final String NEWS_URL = BASE_URL + "MineCraft/news.php";
   public static final URI URL_REGISTER = constantURI(BASE_URL+"forum/register.php");
   public static final URI URL_FORGOT_USERNAME = constantURI(BASE_URL);
   public static final URI URL_FORGOT_PASSWORD_MINECRAFT = constantURI(BASE_URL+"forum/login.php?action=forget");

   public static final String[] BOOTSTRAP_OUT_OF_DATE_BUTTONS = new String[]{"Go to URL", "Close"};
   public static final String[] CONFIRM_PROFILE_DELETION_OPTIONS = new String[]{"Delete profile", "Cancel"};


   public static URI constantURI(String input) {
      try {
         return new URI(input);
      } catch (URISyntaxException var2) {
         throw new Error(var2);
      }
   }

}
