package net.minecraft.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.hopper.Util;
import net.minecraft.launcher.Launcher;
import org.apache.commons.io.IOUtils;

public class Http {

   public static String buildQuery(Map<String, Object> query) {
      StringBuilder builder = new StringBuilder();
      Iterator i$ = query.entrySet().iterator();

      while(i$.hasNext()) {
         Entry entry = (Entry)i$.next();
         if(builder.length() > 0) {
            builder.append('&');
         }

         try {
            builder.append(URLEncoder.encode((String)entry.getKey(), "UTF-8"));
         } catch (UnsupportedEncodingException var6) {
            Launcher.getInstance().println("Unexpected exception building query", var6);
         }

         if(entry.getValue() != null) {
            builder.append('=');

            try {
               builder.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
            } catch (UnsupportedEncodingException var5) {
               Launcher.getInstance().println("Unexpected exception building query", var5);
            }
         }
      }

      return builder.toString();
   }

   public static String performPost(URL url, Map<String, Object> query, Proxy proxy) throws IOException {
      return Util.performPost(url, buildQuery(query), proxy, "application/x-www-form-urlencoded", false);
   }

   public static String performGet(URL url, Proxy proxy) throws IOException {
      HttpURLConnection connection = (HttpURLConnection)url.openConnection(proxy);
      connection.setConnectTimeout(15000);
      connection.setReadTimeout('\uea60');
      connection.setRequestMethod("GET");
      InputStream inputStream = connection.getInputStream();

      String var4;
      try {
         var4 = IOUtils.toString(inputStream);
      } finally {
         IOUtils.closeQuietly(inputStream);
      }

      return var4;
   }

   public static URL concatenateURL(URL url, String args) throws MalformedURLException {
      return url.getQuery() != null && url.getQuery().length() > 0?new URL(url.getProtocol(), url.getHost(), url.getFile() + "?" + args):new URL(url.getProtocol(), url.getHost(), url.getFile() + "&" + args);
   }
}
