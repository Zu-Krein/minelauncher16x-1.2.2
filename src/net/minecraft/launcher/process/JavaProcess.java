package net.minecraft.launcher.process;

import java.util.List;
import net.minecraft.launcher.process.JavaProcessRunnable;
import net.minecraft.launcher.process.LimitedCapacityList;
import net.minecraft.launcher.process.ProcessMonitorThread;

public class JavaProcess {

   private static final int MAX_SYSOUT_LINES = 5;
   private final List<String> commands;
   private final Process process;
   private final LimitedCapacityList<String> sysOutLines = new LimitedCapacityList(String.class, 5);
   private JavaProcessRunnable onExit;
   private ProcessMonitorThread monitor = new ProcessMonitorThread(this);


   public JavaProcess(List<String> commands, Process process) {
      this.commands = commands;
      this.process = process;
      this.monitor.start();
   }

   public Process getRawProcess() {
      return this.process;
   }

   public List<String> getStartupCommands() {
      return this.commands;
   }

   public String getStartupCommand() {
      return this.process.toString();
   }

   public LimitedCapacityList<String> getSysOutLines() {
      return this.sysOutLines;
   }

   public boolean isRunning() {
      try {
         this.process.exitValue();
         return false;
      } catch (IllegalThreadStateException var2) {
         return true;
      }
   }

   public void setExitRunnable(JavaProcessRunnable runnable) {
      this.onExit = runnable;
   }

   public void safeSetExitRunnable(JavaProcessRunnable runnable) {
      this.setExitRunnable(runnable);
      if(!this.isRunning() && runnable != null) {
         runnable.onJavaProcessEnded(this);
      }

   }

   public JavaProcessRunnable getExitRunnable() {
      return this.onExit;
   }

   public int getExitCode() {
      try {
         return this.process.exitValue();
      } catch (IllegalThreadStateException var2) {
         var2.fillInStackTrace();
         throw var2;
      }
   }

   public String toString() {
      return "JavaProcess[commands=" + this.commands + ", isRunning=" + this.isRunning() + "]";
   }

   public void stop() {
      this.process.destroy();
   }
}
