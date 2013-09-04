package net.minecraft.launcher.updater.download;

import net.minecraft.launcher.updater.download.DownloadJob;

public interface DownloadListener {

   void onDownloadJobFinished(DownloadJob var1);

   void onDownloadJobProgressChanged(DownloadJob var1);
}
