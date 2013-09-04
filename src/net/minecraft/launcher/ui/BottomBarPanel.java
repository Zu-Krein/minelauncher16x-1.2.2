package net.minecraft.launcher.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.ui.bottombar.PlayButtonPanel;
import net.minecraft.launcher.ui.bottombar.PlayerInfoPanel;
import net.minecraft.launcher.ui.bottombar.ProfileSelectionPanel;

public class BottomBarPanel extends JPanel {

   private final Launcher launcher;
   private final ProfileSelectionPanel profileSelectionPanel;
   private final PlayerInfoPanel playerInfoPanel;
   private final PlayButtonPanel playButtonPanel;


   public BottomBarPanel(Launcher launcher) {
      this.launcher = launcher;
      byte border = 4;
      this.setBorder(new EmptyBorder(border, border, border, border));
      this.profileSelectionPanel = new ProfileSelectionPanel(launcher);
      this.playerInfoPanel = new PlayerInfoPanel(launcher);
      this.playButtonPanel = new PlayButtonPanel(launcher);
      this.createInterface();
   }

   protected void createInterface() {
      this.setLayout(new GridLayout(1, 3));
      this.add(this.wrapSidePanel(this.profileSelectionPanel, 17));
      this.add(this.playButtonPanel);
      this.add(this.wrapSidePanel(this.playerInfoPanel, 13));
   }

   protected JPanel wrapSidePanel(JPanel target, int side) {
      JPanel wrapper = new JPanel(new GridBagLayout());
      GridBagConstraints constraints = new GridBagConstraints();
      constraints.anchor = side;
      constraints.weightx = 1.0D;
      constraints.weighty = 1.0D;
      wrapper.add(target, constraints);
      return wrapper;
   }

   public Launcher getLauncher() {
      return this.launcher;
   }

   public ProfileSelectionPanel getProfileSelectionPanel() {
      return this.profileSelectionPanel;
   }

   public PlayerInfoPanel getPlayerInfoPanel() {
      return this.playerInfoPanel;
   }

   public PlayButtonPanel getPlayButtonPanel() {
      return this.playButtonPanel;
   }
}
