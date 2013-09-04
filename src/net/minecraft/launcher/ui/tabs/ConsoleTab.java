package net.minecraft.launcher.ui.tabs;

import java.awt.Font;
import java.awt.Insets;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import net.minecraft.launcher.Launcher;

public class ConsoleTab extends JScrollPane {

   private static final Font MONOSPACED = new Font("Monospaced", 0, 12);
   private final JTextPane console = new JTextPane();
   private final Launcher launcher;


   public ConsoleTab(Launcher launcher) {
      this.launcher = launcher;
      this.console.setFont(MONOSPACED);
      this.console.setEditable(false);
      this.console.setMargin((Insets)null);
      this.setViewportView(this.console);
   }

   public Launcher getLauncher() {
      return this.launcher;
   }

   public void print(final String line) {
      if(!SwingUtilities.isEventDispatchThread()) {
         SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               ConsoleTab.this.print(line);
            }
         });
      } else {
         Document document = this.console.getDocument();
         JScrollBar scrollBar = this.getVerticalScrollBar();
         boolean shouldScroll = false;
         if(this.getViewport().getView() == this.console) {
            shouldScroll = (double)scrollBar.getValue() + scrollBar.getSize().getHeight() + (double)(MONOSPACED.getSize() * 4) > (double)scrollBar.getMaximum();
         }

         try {
            document.insertString(document.getLength(), line, (AttributeSet)null);
         } catch (BadLocationException var6) {
            ;
         }

         if(shouldScroll) {
            scrollBar.setValue(Integer.MAX_VALUE);
         }

      }
   }

}
