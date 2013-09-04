package net.minecraft.launcher.authentication.yggdrasil;

import net.minecraft.launcher.authentication.GameProfile;
import net.minecraft.launcher.authentication.yggdrasil.YggdrasilAuthenticationService;

public class RefreshRequest {

   private String clientToken;
   private String accessToken;
   private GameProfile selectedProfile;


   public RefreshRequest(YggdrasilAuthenticationService authenticationService) {
      this(authenticationService, (GameProfile)null);
   }

   public RefreshRequest(YggdrasilAuthenticationService authenticationService, GameProfile profile) {
      this.clientToken = authenticationService.getClientToken();
      this.accessToken = authenticationService.getAccessToken();
      this.selectedProfile = profile;
   }
}
