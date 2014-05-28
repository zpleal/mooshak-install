package pt.up.fc.dcc.mooshak.installer;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import pt.up.fc.dcc.mooshak.installer.Driver.Progressable;

public class Utils {

	static final String PROPERTIES = "installer.properties";
	static ClassLoader loader = Thread.currentThread().getContextClassLoader();
	static Properties properties = new Properties();
	
	static {
		String here = Installer.class.getPackage().getName().replace(".","/");
		String file = here+File.separator+PROPERTIES;
			
		try(InputStream stream = loader.getResourceAsStream(file);) {
			properties.load(stream);
		} catch (IOException cause) {
			cause.printStackTrace(System.err);
			System.exit(1);
		}
	}
	
	
	/**
	 * Get installer property	
	 * @param name
	 * @return
	 */
	public static String getProperty(String name) {
		return properties.get(name).toString();
	}
	
	/**
	 * Get installer property as an array or values
	 * @param name
	 * @return
	 */
	public static String[] getPropertyValues(String name) {
		return properties.get(name).toString().split(";");
	}
	
	/**
	 * Checks if this JVM is running on windows
	 * @return {@code true} if this JVM runs on Windows;{@code false} otherwise
	 */
	public static boolean isWindows() {
		return System.getProperty("os.name","").startsWith("Windows");
	}
	
	/**
	 * Checks if a system property backspace=no was defined,
	 * typically with (-DhasBackspace=no)
	 * Otherwise is assumed that the standard output supports backspace.
	 * Eclipse console does not support backspace.
	 * @return {@code true} if output supports backspace; 
	 * {@code false} otherwise
	 */
	public static boolean hasBackspace() {
		return ! "no".equals(System.getProperty("backspace","yes"));
	}

	/**
	 * Checks if this JVM has root permission
	 * @return
	 */
	public static boolean isRoot() {
		return "root".equals(System.getProperty("user.name",""));
	}
	
	private final static int BUFFER_SIZE = 1<<12;
	
	/**
	 * Download the content of an URL to a file in the given path.
	 * This method sends a update notification for each part of the file read.
	 * Update notification may not be possible (if length of file is unknown) 
	 * and this is notified in the beginning.
	 * The number of parts of the file is one of the parameters
	 * @param address of file (URL) 
	 * @param suffix of temporary file
	 * @param initializer of the update process 
	 * 		(receives {@code true} if update is possible }
	 * @param updater of the part of file notification
	 * 		( receives the number of parts already download) 
	 * @param parts in which notification will occur
	 * @return path to the download file
	 */
	public static Path dowloadFrom(String address,String suffix,
			Progressable progressable) {
		Path tmp=null;
		
		try {	
		URL url = new URL(address);
		URLConnection con = url.openConnection();
		long length = con.getContentLength();
		byte[] buffer = new byte[BUFFER_SIZE];
		boolean isUpdatable = length > 0;
		int parts = progressable.getMaximum();
		
		tmp = Files.createTempFile("mooshak", suffix);
		progressable.updatable(isUpdatable);
		
		try(
				InputStream in = con.getInputStream();
				OutputStream out= Files.newOutputStream(tmp);
						) {
			int part = 0;
			int read = 0;
			while(true) {
				int len = in.read(buffer,0, BUFFER_SIZE);
				if(len < 0) 
					break;
				out.write(buffer, 0, len);
				read += len;
				int next = (int) (read * parts / length);
				if(isUpdatable && next > part) {
					progressable.update(part = next);
				}
			}
			if(isUpdatable)
				progressable.update(parts);
		}
		
		} catch(IOException cause) {
			progressable.taskError(cause.toString());
		}
		return tmp;
	}
	
	
	/**
	 * Extract ZIP file with a single directory to given path
	 * @param zipFile
	 * @param as
	 * @throws IOException
	 */
	public static void extractAs(ZipFile zipFile, Path as) throws IOException {
		Path root = extractTo(zipFile,as.getParent());
		if(root != null)
			Files.move(root, as);
	}
	
	/**
	 * Extract ZIP file to given location
	 * @param zipFile
	 * @param to
	 * @throws IOException
	 */
	public static Path extractTo(ZipFile zipFile, Path to) throws IOException { 
		Path root = null;
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		
		while(entries.hasMoreElements()) { 	  
		        ZipEntry entry = (ZipEntry)entries.nextElement();
		    	String name = entry.getName();
		        Path path = to.resolve(name);

		        if(entry.isDirectory()) {
		        	Files.createDirectories(path);
		        	if(name.matches("[^/]+/"))
		        		root = path;
		        } else {
		        	// make sure parent directories were created
		        	Files.createDirectories(path.getParent());
		        	Files.copy(zipFile.getInputStream(entry),
		        			path,REPLACE_EXISTING);
		        }
		}
		zipFile.close();
		return root;
	}
	

	
	public static Path extractTo(ZipFile zipFile, Path to,
			Progressable progressable) throws IOException { 
		Path root = null;
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
	
		int parts = progressable.getMaximum();
		int size = zipFile.size();
		int part = 0;
		int count = 0;
	
		progressable.updatable(true);
		
		while(entries.hasMoreElements()) { 	  
		        ZipEntry entry = (ZipEntry)entries.nextElement();
		    	String name = entry.getName();
		        Path path = to.resolve(name);

		        if(entry.isDirectory()) {
		        	Files.createDirectories(path);
		        	if(name.matches("[^/]+/"))
		        		root = path;
		        } else {
		        	// make sure parent directories were created
		        	Files.createDirectories(path.getParent());
		        	Files.copy(zipFile.getInputStream(entry),
		        			path,REPLACE_EXISTING);
		        }
		        if(++count > size*(part+1)/parts)
					progressable.update(++part);
		}
		progressable.update(++part);
		zipFile.close();
		return root;
	}
	
	private static Charset charset = Charset.forName("UTF-8");
	private static Pattern dirPattern = Pattern.compile("<tr><td valign=\"top\">"+
			"<img src=\"/icons/folder.png\" alt=\"\\[DIR\\]\">"+
			"</td><td><a href=\"([^/]+)/\">");
	/**
	 * Create a list of sub-directory from an Apache generated directory index
	 * formatted in HTML
	 *   
	 * @param path		to HTML file containing index
	 * @return			list of sub-directories
	 * @throws IOException
	 */
	public static List<String> parseListing(Path path) throws IOException {
		
		return Files
					.lines(path,charset)
					.filter(dirPattern.asPredicate())
					.map(line -> {	
						Matcher matcher = dirPattern.matcher(line);
						matcher.find();
						return matcher.group(1);})
					.collect(Collectors.toList());
	}
	
	/**
	 * Expand a camel case name to a title 
	 * with spaces before capitals in the middle of the name
	 * and always starting with a capital.
	 * 
	 * @param camel		name in either upper or lower camel case
	 * @return			Title with spaces
	 */
	public static String expandCamelCase(String camel) {
		StringBuilder text = new StringBuilder();
		boolean first = true;
		
		for(int pos=0; pos<camel.length(); pos++) {
			char c = camel.charAt(pos);
			if(first) {
				text.append(Character.toUpperCase(c));
				first = false;
			} else {
				if(Character.isUpperCase(c))
					text.append(' ');
				text.append(c);
			}
		}
		
		return text.toString();
	}
	
	/**
	 * Compare 2 version IDs, a string of dot separated numbers
	 * @param version1
	 * @param version2
	 * @return
	 */
	public static int compareVersions(String version1, String version2) {
		String[] parts1 = version1.split("\\.");
		String[] parts2 = version2.split("\\.");
		int pos = 0;
		
		do {
			if(parts1.length == pos) {
				if(parts2.length == pos)
					return 0;
				else
					return -1;
			} else {
				if(parts2.length == pos)
					return 1;
			}
			
			int value1 = Integer.parseInt(parts1[pos]);
			int value2 = Integer.parseInt(parts2[pos]);
			
			if(value1 == value2)
				pos++;
			else 
				return value1 - value2;
		} while(true);
	}
	
	
}
