package net.minecraft.launcher.ui.tabs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.events.RefreshedVersionsListener;
import net.minecraft.launcher.updater.VersionManager;
import net.minecraft.launcher.updater.VersionSyncInfo;
import net.minecraft.launcher.versions.CompleteVersion;
import net.minecraft.launcher.versions.Version;

public class VersionListTab extends JScrollPane implements RefreshedVersionsListener {

   private static final int COLUMN_NAME = 0;
   private static final int COLUMN_TYPE = 1;
   private static final int COLUMN_RELEASE_DATE = 2;
   private static final int COLUMN_UPDATE_DATE = 3;
   private static final int COLUMN_LIBRARIES = 4;
   private static final int COLUMN_STATUS = 5;
   private static final int NUM_COLUMNS = 6;
   private final Launcher launcher;
   private final VersionListTab.VersionTableModel dataModel = new VersionListTab.VersionTableModel((VersionListTab.NamelessClass1733217970)null);
   private final JTable table;


   public VersionListTab(Launcher launcher) {
      this.table = new JTable(this.dataModel);
      this.launcher = launcher;
      this.setViewportView(this.table);
      this.createInterface();
      launcher.getVersionManager().addRefreshedVersionsListener(this);
   }

   protected void createInterface() {
      this.table.setFillsViewportHeight(true);
   }

   public Launcher getLauncher() {
      return this.launcher;
   }

   public void onVersionsRefreshed(VersionManager manager) {
      this.dataModel.setVersions(manager.getLocalVersionList().getVersions());
   }

   public boolean shouldReceiveEventsInUIThread() {
      return true;
   }

   private class VersionTableModel extends AbstractTableModel {

      private final List<Version> versions;


      private VersionTableModel() {
         this.versions = new ArrayList();
      }

      public int getRowCount() {
         return this.versions.size();
      }

      public int getColumnCount() {
         return 6;
      }

      public Class<?> getColumnClass(int columnIndex) {
         return columnIndex != 3 && columnIndex != 2?String.class:Date.class;
      }

      public String getColumnName(int column) {
         switch(column) {
         case 0:
            return "Version name";
         case 1:
            return "Version type";
         case 2:
            return "Release Date";
         case 3:
            return "Last modified";
         case 4:
            return "Library count";
         case 5:
            return "Sync status";
         default:
            return super.getColumnName(column);
         }
      }

      public Object getValueAt(int rowIndex, int columnIndex) {
         Version version = (Version)this.versions.get(rowIndex);
         switch(columnIndex) {
         case 0:
            return version.getId();
         case 1:
            return version.getType().getName();
         case 2:
            return version.getReleaseTime();
         case 3:
            return version.getUpdatedTime();
         case 4:
            if(version instanceof CompleteVersion) {
               CompleteVersion syncInfo1 = (CompleteVersion)version;
               int total = syncInfo1.getLibraries().size();
               int relevant = syncInfo1.getRelevantLibraries().size();
               if(total == relevant) {
                  return Integer.valueOf(total);
               }

               return String.format("%d (%d relevant to %s)", new Object[]{Integer.valueOf(total), Integer.valueOf(relevant), OperatingSystem.getCurrentPlatform().getName()});
            }

            return "?";
         case 5:
            VersionSyncInfo syncInfo = VersionListTab.this.launcher.getVersionManager().getVersionSyncInfo(version);
            if(syncInfo.isOnRemote()) {
               if(syncInfo.isUpToDate()) {
                  return "Up to date with remote";
               }

               return "Update avail from remote";
            }

            return "Local only";
         default:
            return null;
         }
      }

      public void setVersions(Collection<Version> versions) {
         this.versions.clear();
         this.versions.addAll(versions);
         this.fireTableDataChanged();
      }

      // $FF: synthetic method
      VersionTableModel(VersionListTab.NamelessClass1733217970 x1) {
         this();
      }
   }

   // $FF: synthetic class
   static class NamelessClass1733217970 {
   }
}
