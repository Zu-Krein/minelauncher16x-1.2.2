package net.minecraft.launcher.ui.popups.login;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JPopupMenu.Separator;
import net.minecraft.launcher.authentication.AuthenticationDatabase;
import net.minecraft.launcher.authentication.AuthenticationService;
import net.minecraft.launcher.authentication.exceptions.AuthenticationException;
import net.minecraft.launcher.ui.popups.login.LogInPopup;

public class ExistingUserListForm extends JPanel implements ActionListener {

   private final LogInPopup popup;
   private final JComboBox userDropdown = new JComboBox();
   private final AuthenticationDatabase authDatabase;
   private final JButton playButton = new JButton("Play");


   public ExistingUserListForm(LogInPopup popup) {
      this.popup = popup;
      this.authDatabase = popup.getLauncher().getProfileManager().getAuthDatabase();
      this.fillUsers();
      this.createInterface();
      this.playButton.addActionListener(this);
   }

   private void fillUsers() {
      Iterator i$ = this.authDatabase.getKnownNames().iterator();

      while(i$.hasNext()) {
         String user = (String)i$.next();
         this.userDropdown.addItem(user);
      }

   }

   protected void createInterface() {
      this.setLayout(new GridBagLayout());
      GridBagConstraints constraints = new GridBagConstraints();
      constraints.fill = 2;
      constraints.gridx = 0;
      constraints.gridy = -1;
      constraints.gridwidth = 2;
      constraints.weightx = 1.0D;
      this.add(Box.createGlue());
      String currentUser = this.authDatabase.getKnownNames().size() == 1?(String)this.authDatabase.getKnownNames().iterator().next():this.authDatabase.getKnownNames().size() + " different users";
      String thisOrThese = this.authDatabase.getKnownNames().size() == 1?"this account":"one of these accounts";
      this.add(new JLabel("You\'re already logged in as " + currentUser + "."), constraints);
      this.add(new JLabel("You may use " + thisOrThese + " and skip authentication."), constraints);
      this.add(Box.createVerticalStrut(5), constraints);
      JLabel usernameLabel = new JLabel("Existing User:");
      Font labelFont = usernameLabel.getFont().deriveFont(1);
      usernameLabel.setFont(labelFont);
      this.add(usernameLabel, constraints);
      constraints.gridwidth = 1;
      this.add(this.userDropdown, constraints);
      constraints.gridx = 1;
      constraints.gridy = 5;
      constraints.weightx = 0.0D;
      constraints.insets = new Insets(0, 5, 0, 0);
      this.add(this.playButton, constraints);
      constraints.insets = new Insets(0, 0, 0, 0);
      constraints.weightx = 1.0D;
      constraints.gridx = 0;
      constraints.gridy = -1;
      constraints.gridwidth = 2;
      this.add(Box.createVerticalStrut(5), constraints);
      this.add(new JLabel("Alternatively, log in with a new account below:"), constraints);
      this.add(new Separator(), constraints);
   }

   public void actionPerformed(ActionEvent e) {
      if(e.getSource() == this.playButton) {
         this.popup.setCanLogIn(false);
         this.popup.getLauncher().getVersionManager().getExecutorService().execute(new Runnable() {
            public void run() {
               Object selected = ExistingUserListForm.this.userDropdown.getSelectedItem();
               AuthenticationService auth;
               String uuid;
               if(selected != null && selected instanceof String) {
                  auth = ExistingUserListForm.this.authDatabase.getByName((String)selected);
                  if(auth.getSelectedProfile() == null) {
                     uuid = "demo-" + auth.getUsername();
                  } else {
                     uuid = auth.getSelectedProfile().getId();
                  }
               } else {
                  auth = null;
                  uuid = null;
               }

               if(auth != null && uuid != null) {
                  try {
                     auth.logIn();
                     ExistingUserListForm.this.popup.setLoggedIn(uuid);
                  } catch (AuthenticationException var5) {
                     ExistingUserListForm.this.popup.getErrorForm().displayError(new String[]{"We couldn\'t log you back in as " + selected + ".", "Please try to log in again."});
                     ExistingUserListForm.this.userDropdown.removeItem(selected);
                     if(ExistingUserListForm.this.userDropdown.getItemCount() == 0) {
                        SwingUtilities.invokeLater(new Runnable() {
                           public void run() {
                              ExistingUserListForm.this.popup.remove(ExistingUserListForm.this);
                           }
                        });
                     }

                     ExistingUserListForm.this.popup.setCanLogIn(true);
                  }
               } else {
                  ExistingUserListForm.this.popup.setCanLogIn(true);
               }

            }
         });
      }

   }
}
