package net.minecraft.launcher.updater;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.launcher.updater.VersionList;
import org.apache.commons.io.IOUtils;

public abstract class FileBasedVersionList extends VersionList {

   protected String getContent(String path) throws IOException {
      return IOUtils.toString(this.getFileInputStream(path)).replaceAll("\\r\\n", "\r").replaceAll("\\r", "\n");
   }

   protected abstract InputStream getFileInputStream(String var1) throws FileNotFoundException;
}
