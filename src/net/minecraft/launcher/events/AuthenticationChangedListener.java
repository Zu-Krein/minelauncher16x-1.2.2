package net.minecraft.launcher.events;

import net.minecraft.launcher.authentication.AuthenticationService;

public interface AuthenticationChangedListener {

   void onAuthenticationChanged(AuthenticationService var1);

   boolean shouldReceiveEventsInUIThread();
}
