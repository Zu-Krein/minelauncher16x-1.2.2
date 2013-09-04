package net.minecraft.launcher.versions;

import java.util.HashMap;
import java.util.Map;

public enum ReleaseType {

   SNAPSHOT("SNAPSHOT", 0, "snapshot", "Enable experimental development versions (\"snapshots\")"),
   RELEASE("RELEASE", 1, "release", (String)null),
   OLD_BETA("OLD_BETA", 2, "old-beta", "Allow use of old \"Beta\" minecraft versions (From 2010-2011)"),
   OLD_ALPHA("OLD_ALPHA", 3, "old-alpha", "Allow use of old \"Alpha\" minecraft versions (From 2010)");
   private static final String POPUP_DEV_VERSIONS = "Are you sure you want to enable development builds?\nThey are not guaranteed to be stable and may corrupt your world.\nYou are advised to run this in a separate directory or run regular backups.";
   private static final String POPUP_OLD_VERSIONS = "These versions are very out of date and may be unstable. Any bugs, crashes, missing features or\nother nasties you may find will never be fixed in these versions.\nIt is strongly recommended you play these in separate directories to avoid corruption.\nWe are not responsible for the damage to your nostalgia or your save files!";
   private static final Map<String, ReleaseType> lookup = new HashMap();
   private final String name;
   private final String description;
   // $FF: synthetic field
   private static final ReleaseType[] $VALUES = new ReleaseType[]{SNAPSHOT, RELEASE, OLD_BETA, OLD_ALPHA};


   private ReleaseType(String var1, int var2, String name, String description) {
      this.name = name;
      this.description = description;
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public String getPopupWarning() {
      return this.description == null?null:(this == SNAPSHOT?"Are you sure you want to enable development builds?\nThey are not guaranteed to be stable and may corrupt your world.\nYou are advised to run this in a separate directory or run regular backups.":(this == OLD_BETA?"These versions are very out of date and may be unstable. Any bugs, crashes, missing features or\nother nasties you may find will never be fixed in these versions.\nIt is strongly recommended you play these in separate directories to avoid corruption.\nWe are not responsible for the damage to your nostalgia or your save files!":(this == OLD_ALPHA?"These versions are very out of date and may be unstable. Any bugs, crashes, missing features or\nother nasties you may find will never be fixed in these versions.\nIt is strongly recommended you play these in separate directories to avoid corruption.\nWe are not responsible for the damage to your nostalgia or your save files!":null)));
   }

   public static ReleaseType getByName(String name) {
      return (ReleaseType)lookup.get(name);
   }

   static {
      ReleaseType[] arr$ = values();
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         ReleaseType type = arr$[i$];
         lookup.put(type.getName(), type);
      }

   }
}
