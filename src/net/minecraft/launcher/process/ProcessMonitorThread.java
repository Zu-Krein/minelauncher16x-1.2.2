package net.minecraft.launcher.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.process.JavaProcess;
import net.minecraft.launcher.process.JavaProcessRunnable;

public class ProcessMonitorThread extends Thread {

   private final JavaProcess process;


   public ProcessMonitorThread(JavaProcess process) {
      this.process = process;
   }

   public void run() {
      InputStreamReader reader = new InputStreamReader(this.process.getRawProcess().getInputStream());
      BufferedReader buf = new BufferedReader(reader);
      String line = null;

      while(this.process.isRunning()) {
         try {
            while((line = buf.readLine()) != null) {
               Launcher.getInstance().println("Client> " + line);
               this.process.getSysOutLines().add(line);
            }
         } catch (IOException var13) {
            Logger.getLogger(ProcessMonitorThread.class.getName()).log(Level.SEVERE, (String)null, var13);
         } finally {
            try {
               buf.close();
            } catch (IOException var12) {
               Logger.getLogger(ProcessMonitorThread.class.getName()).log(Level.SEVERE, (String)null, var12);
            }

         }
      }

      JavaProcessRunnable onExit = this.process.getExitRunnable();
      if(onExit != null) {
         onExit.onJavaProcessEnded(this.process);
      }

   }
}
