package me.zeroeightsix.kami;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import net.bytebuddy.agent.ByteBuddyAgent;

/**
 * @author bon
 * 
 * with inspiration from Zyklon
 */

public class Main
{
	/**
	 * Main method. Adds the resource com.sun.tools to the classpath so we can mess with VirtualMachines, then attaches the dumper jar to the VM pid.
	 * 
	 * Pass through a class keyword into arguments to filter classes, so <code>pyro</code> if you wanted to dump pyro, <code>futureclient</code> if you wanted to dump future, and etc.
	 * This prevents the dumper from dumping literally every transformable class, including ones you don't want.
	 */
	public static void main(String[] args) {
		if(args.length > 1) {
			System.out.println("Maximum of 1 argument.");
			return;
		} else if(args.length < 1) {
			System.out.println("Provide a class keyword argument.");
			return;
		}
		try {
			Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			addURL.setAccessible(true);
			String tools = "file:///" + System.getenv("JAVA_HOME") + System.getProperty("file.separator") + "lib" + System.getProperty("file.separator") + "tools.jar";
			addURL.invoke(ClassLoader.getSystemClassLoader(), new URL(tools));
		} catch (Exception e) {
			e.printStackTrace();
		}
		Main.attach(args[0]);
	}
	
	/**
	 * Attaches the jar on your desktop called dumper.jar to the VM whos display name contains net.minecraft.launchwrapper.Launch. Runs the agentmain method.
	 * 
	 * @param argument A keyword or class prefix for the client you want to dump
	 */
	public static void attach(String argument) {
		String libs = System.getenv("JAVA_HOME") + System.getProperty("file.separator") + "jre" + System.getProperty("file.separator") + "bin";
		try {
			System.setProperty("java.library.path", libs);
			Field sysPaths = ClassLoader.class.getDeclaredField("sys_paths");
			sysPaths.setAccessible(true);
			sysPaths.set(null, null);
			for(VirtualMachineDescriptor vm : VirtualMachine.list()) {
				if(vm.displayName().contains("net.minecraft.launchwrapper.Launch")) {
					System.out.println("Found Minecraft with VM id " + vm.id());
					System.out.println("Dumper processing, please wait.");
					ByteBuddyAgent.attach(new File(System.getenv("USERPROFILE") + "\\Desktop\\dumper.jar"), vm.id(), argument);
					System.out.println("Dumper finished processing.");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
