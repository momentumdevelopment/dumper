package me.zeroeightsix.kami;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author bon
 */

public class Main
{
	protected static Map<String, byte[]> CLASS_MAP;
	/**
	 * The agentmain method is an alternative main-method that gets called when ByteBuddyAgent attaches the agent jar to the VM.
	 * <code>args</code> is the same argument passed through injector.jar, used to specify the class names to filter.
	 * 
	 * When it is called, an instance of {@link java.lang.instrument.Instrumentation} is passed through.
	 * <code>Instrumentation</code> can be used to get every loaded class in the VM, as well as apply transformers to those classes.
	 */
	public static void agentmain(String args, Instrumentation inst) {
		Main.CLASS_MAP = new HashMap<>();
		try {
			Arrays.stream(inst.getAllLoadedClasses()).forEach(clazz -> {
				try {
					if(clazz.getName().contains(args)) {
						/**
						 * OpenJDK 7/8 has an issue where lambdas cannot be retransformed (https://bugs.openjdk.java.net/browse/JDK-8145964) so we have to skip these.
						 * They are still written to the file though.
						 */
						if(clazz.getName().contains("$$Lambda$")) return;
						if(inst.isModifiableClass(clazz)) {
							Transformer transformer = new Transformer();
							inst.addTransformer(transformer, true);
							inst.retransformClasses(clazz);
							inst.removeTransformer(transformer);
							System.out.println("Successfully dumped " + clazz.getName() + ".");
						}
					}
				} catch (SecurityException | IllegalArgumentException | UnmodifiableClassException e) {
					e.printStackTrace();
				}
			});
			write(Main.CLASS_MAP);
		} catch (IllegalArgumentException | SecurityException | IOException e) {
			e.printStackTrace();
		}
    }
	
	/**
	 * Writes a classmap of a class name and its bytes to a file on your desktop.
	 * 
	 * @param classMap The {@link java.util.Map} classmap to be written.
	 */
	public static void write(Map<String, byte[]> classMap) throws IOException {
		File file = new File(System.getenv("USERPROFILE") + "\\Desktop\\kami-b10-release.jar");
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
		classMap.forEach((name, byteArray) -> {
			try {
				ZipEntry zipEntry = new ZipEntry(name);
				zos.putNextEntry(zipEntry);
				zos.write(byteArray);
				zos.closeEntry();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		zos.close();
	}
	
	private static class Transformer implements ClassFileTransformer
	{
		/**
		 * When invoking {@link java.lang.instrument.Instrumentation#retransformClasses(Class...)} on a class, the <code>transform</code> method is called.
		 * When <code>transform</code> is called, one param passed through is the <code>classfileBuffer</code>, which can be saved and written to a file.
		 */
		@Override
		public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
			Main.CLASS_MAP.put(classBeingRedefined.getName().replace(".", "/").concat(".class"), classfileBuffer);
			return classfileBuffer;
		}
	}
}