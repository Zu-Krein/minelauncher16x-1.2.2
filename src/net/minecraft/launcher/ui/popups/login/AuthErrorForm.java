package net.minecraft.launcher.ui.popups.login;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.minecraft.launcher.ui.popups.login.LogInPopup;

public class AuthErrorForm extends JPanel {

   private final LogInPopup popup;
   private final JLabel errorLabel = new JLabel();


   public AuthErrorForm(LogInPopup popup) {
      this.popup = popup;
      this.createInterface();
      this.clear();
   }

   protected void createInterface() {
      this.setBorder(new EmptyBorder(0, 0, 15, 0));
      this.errorLabel.setFont(this.errorLabel.getFont().deriveFont(1));
      this.add(this.errorLabel);
   }

   public void clear() {
      this.setVisible(false);
   }

   public void setVisible(boolean value) {
      super.setVisible(value);
      this.popup.repack();
   }

   public void displayError(final String ... lines) {
      if(SwingUtilities.isEventDispatchThread()) {
         String error = "";
         String[] arr$ = lines;
         int len$ = lines.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String line = arr$[i$];
            error = error + "<p>" + line + "</p>";
         }

         this.errorLabel.setText("<html><div style=\'text-align: center;\'>" + error + " </div></html>");
         this.setVisible(true);
      } else {
         SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               AuthErrorForm.this.displayError(lines);
            }
         });
      }

   }
}
