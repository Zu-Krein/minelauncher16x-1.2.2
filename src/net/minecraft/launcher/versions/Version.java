package net.minecraft.launcher.versions;

import java.util.Date;
import net.minecraft.launcher.versions.ReleaseType;

public interface Version {

   String getId();

   ReleaseType getType();

   void setType(ReleaseType var1);

   Date getUpdatedTime();

   void setUpdatedTime(Date var1);

   Date getReleaseTime();

   void setReleaseTime(Date var1);
}
