package cj.studio.gateway.socket.app;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cj.studio.ecm.resource.JarClassLoader;
import cj.ultimate.util.FileHelper;

public class FillClassLoader {

	public static ClassLoader fillShare(String sharedir) {
		List<File> jars=new ArrayList<>();
		FileHelper.scansSubAllJarFiles(new File(sharedir), jars);
		JarClassLoader cl=new JarClassLoader();
		for(File f:jars){
			cl.loadJar(f.getAbsolutePath());
		}
		return cl;
	}

}
