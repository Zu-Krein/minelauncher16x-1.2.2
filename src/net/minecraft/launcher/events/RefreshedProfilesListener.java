package net.minecraft.launcher.events;

import net.minecraft.launcher.profile.ProfileManager;

public interface RefreshedProfilesListener {

   void onProfilesRefreshed(ProfileManager var1);

   boolean shouldReceiveEventsInUIThread();
}
