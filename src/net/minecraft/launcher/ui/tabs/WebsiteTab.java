package net.minecraft.launcher.ui.tabs;

import java.awt.Color;
import java.awt.Insets;
import java.net.URL;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.OperatingSystem;

public class WebsiteTab extends JScrollPane {

   private final JTextPane blog = new JTextPane();
   private final Launcher launcher;


   public WebsiteTab(Launcher launcher) {
      this.launcher = launcher;
      this.blog.setEditable(false);
      this.blog.setMargin((Insets)null);
      this.blog.setBackground(Color.DARK_GRAY);
      this.blog.setContentType("text/html");
      this.blog.setText("<html><body><font color=\"#808080\"><br><br><br><br><br><br><br><center><h1>Loading page..</h1></center></font></body></html>");
      this.blog.addHyperlinkListener(new HyperlinkListener() {
         public void hyperlinkUpdate(HyperlinkEvent he) {
            if(he.getEventType() == EventType.ACTIVATED) {
               try {
                  OperatingSystem.openLink(he.getURL().toURI());
               } catch (Exception var3) {
                  Launcher.getInstance().println("Unexpected exception opening link " + he.getURL(), var3);
               }
            }

         }
      });
      this.setViewportView(this.blog);
   }

   public void setPage(final String url) {
      Thread thread = new Thread("Update website tab") {
         public void run() {
            try {
               WebsiteTab.this.blog.setPage(new URL(url));
            } catch (Exception var2) {
               Launcher.getInstance().println("Unexpected exception loading " + url, var2);
               WebsiteTab.this.blog.setText("<html><body><font color=\"#808080\"><br><br><br><br><br><br><br><center><h1>Failed to get page</h1><br>" + var2.toString() + "</center></font></body></html>");
            }

         }
      };
      thread.setDaemon(true);
      thread.start();
   }

   public Launcher getLauncher() {
      return this.launcher;
   }
}
