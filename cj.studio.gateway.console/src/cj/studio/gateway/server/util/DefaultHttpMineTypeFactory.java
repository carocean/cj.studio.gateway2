package cj.studio.gateway.server.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import cj.studio.ecm.EcmException;

public class DefaultHttpMineTypeFactory {
	static Properties props;
	public static void setHomeDir(String homeDir) {
		props=new Properties();
		String fn=String.format("%s%senv%sdefault_mime.properties", homeDir,File.separator,File.separator);
		File f=new File(fn);
		if(!f.exists()) {
			f.getParentFile().mkdirs();
			try {
				f.createNewFile();
			} catch (IOException e) {
				throw new EcmException(e);
			}
		}
		
		try {
			props.load(new FileReader(f));
		} catch (IOException e) {
			throw new EcmException(e);
		}
	}
	public static boolean containsMime(String extName) {
		return props.containsKey(extName);
	}

	public static String mime(String extName) {
		return props.getProperty(extName);
	}
	
}
