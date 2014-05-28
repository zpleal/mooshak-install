package pt.up.fc.dcc.mooshak.installer;

import static pt.up.fc.dcc.mooshak.installer.Utils.getProperty;
import static pt.up.fc.dcc.mooshak.installer.Utils.getPropertyValues;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import pt.up.fc.dcc.mooshak.installer.Driver.Progressable;



/**
 * Configurations of servlet container and home directory content
 * 
 *
 * @author José Paulo Leal <zp@dcc.fc.up.pt>
 */
public class Configurator {
	
	private static final String BASH = "/bin/bash";
	private static final String DEFAULT_MASTER_HOST = "127.0.0.1";
	private static final String DEFAULT_VERSION = "";
	
	
	private Consumer<String> errorHandler;
	
	private Path servletContainerPath = null;
	private Path homeDirectory = null;
	private String masterHost = DEFAULT_MASTER_HOST;
	private String version = DEFAULT_VERSION;
	
	private Properties properties = defaultProperties();

	private Runtime runtime = Runtime.getRuntime();
	
	// Servlet container methods
	
	public Path getInstalledServletContainerPath() {
		Path found = null;
		
		for(String location: 
				Utils.getPropertyValues("container.locations")) {
			Path path = Paths.get(location);
			if(Files.isReadable(path))
				found = path;
		}
		
		return found;
	}
	
	/**
	 * Get preferred location for servlet container (Tomcat)
	 * @return
	 */
	public Path getPreferredServletContainerPath() {
		return Paths.get(Utils.getPropertyValues("container.locations")[0]);
	}
	
	/**
	 * Checks if given paths holds a servlet container (Tomcat)
	 * @param path
	 * @return
	 */
	public boolean hasServeletContainer(Path path) {
		String webappsName = Utils.getProperty("container.webapps");
		Path webapps = path.resolve(webappsName);
		
		return Files.isDirectory(path) &&
				Files.isReadable(path) &&
				Files.isDirectory(webapps) &&
				Files.isReadable(webapps) &&
				Files.isWritable(path);
	}
	
	
	/**
	 * Return an existing home directory, by preference order
	 * @return An existing home directory or {@null} is none is available
	 */
	public Path getExistingtHomeDirectory() {
		Path existing = null;
		Path propertyHome = Paths.get(properties.getProperty("homeDirectory"));
		
		if(hasMooshakHome(homeDirectory))
			existing = homeDirectory;
		else if(hasMooshakHome(propertyHome))
			existing = propertyHome;
		else
			for(String candidate: getPropertyValues("home.locations")) {
				Path path = Paths.get(candidate);
				if(hasMooshakHome(path)) {
					existing=path;
					break;
				}
			}
		return existing;
	}
		
	/**
	 * If a home directory needs to be created this is the
	 * preferred name
	 * @return
	 */
	public Path getPreferredHomeDirectory() {
		
		return Paths.get(Utils.getPropertyValues("home.locations")[0]);
	}
	
	/**
	 * Check if given pathname contains a Mooshak home
	 * @param home
	 * @return
	 */
	private boolean hasMooshakHome(Path homePath) {
		if(homePath == null)
			return false;
		else {	
			Path dataPath = homePath.resolve(Utils.getProperty("home.root"));
			return 
				Files.isDirectory(homePath) && 
				Files.isReadable(homePath) 	&&
				Files.isDirectory(dataPath) &&
				Files.isReadable(dataPath);
		}
	}
	
	// property methods
	
	/**
	 * Default Mooshak properties
	 * @return
	 */
	public Properties defaultProperties() {
		properties = new Properties();
		
		properties.setProperty("homeDirectory",
			getPreferredHomeDirectory().toString());
		properties.setProperty("masterHostName", DEFAULT_MASTER_HOST);
		properties.setProperty("version", DEFAULT_VERSION);
		
		return properties;
	}
	
	/**
	 * Load property file from servlet container
	 */
	public void loadProperties() {
		Path path = getPropertyFile();
		
		if(Files.exists(path)) {
			
				try(InputStream stream = Files.newInputStream(path)) {
					properties.loadFromXML(stream);
				} catch (IOException e) {
				errorHandler.accept(e.toString());
			}
		} 
	}
	
	
	/**
	 * Make an XML property file.  Save it in servlet container
	 */
	public void saveProperties() {
		
		try(OutputStream stream = Files.newOutputStream(getPropertyFile())) {
			
			properties.storeToXML(stream, 
					getProperty("properties.comment"), 
					getProperty("properties.encoding"));
		} catch (IOException cause) {
			errorHandler.accept(cause.getLocalizedMessage());
		} 
		
	}
	
	//  Getters & Setters

	/**
	 * Get path to servlet container
	 * @return the serveletContainerPath
	 */
	public Path getServletContainerPath() {
		return servletContainerPath;
	}

	
	
	/**
	 * Set path to servlet container
	 * @param serveletContainerPath the serveletContainerPath to set
	 */
	public void setServletContainerPath(Path serveletContainerPath) {
		this.servletContainerPath = serveletContainerPath;
		properties.setProperty("servletContainer",
				serveletContainerPath.getFileName().toString());
	}
		
	
	static Pattern endsInZip = Pattern.compile("/([^/]*).zip");
	
	
	public void setServletContainerVersion(Path path) {
		String url = path.toString();
		Matcher matcher = endsInZip.matcher(url);
		String version = "??";
		
		if(matcher.find())
			version = matcher.group(1);
		
		properties.setProperty("servletContainerVersion",version);
	}	
	
	/**
	 * Get path to the home directory
	 * @return the homeDirectory
	 */
	public Path getHomeDirectory() {
		if(homeDirectory == null)
			homeDirectory = Paths.get(properties.getProperty("homeDirectory"));
		return homeDirectory;
	}

	/**
	 * Set path to the home directory
	 * @param homeDirectory the homeDirectory to set
	 */
	public void setHomeDirectory(Path homeDirectory) {
		this.homeDirectory = homeDirectory;
		properties.setProperty("homeDirectory", homeDirectory.toString());
	}

	
	/**
	 * Get Mooshak version selected for installation
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Set Mooshak version selected for installation
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
		properties.setProperty("version", version);
	}
	
	/**
	 * Get the master host for this installation
	 * @return the masterHost
	 */
	public String getMasterHost() {
		return masterHost;
	}

	/**
	 * Set the master host for this installation
	 * @param masterHost the masterHost to set
	 */
	public void setMasterHost(String masterHost) {
		this.masterHost = masterHost;
		properties.setProperty("masterHost", masterHost);
	}

	/**
	 * Get Mooshak installation properties
	 * @return the properties
	 * @throws IOException 
	 */
	public Properties getProperties()  {		
		return properties;		
	}
	
	/**
	 * Set Mooshak installation properties
	 * @param properties the properties to set
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}


	/**
	 * Get current consumer of strings that reports them as errors
	 * @return the errorHandler
	 */
	public Consumer<String> getErrorHandler() {
		return errorHandler;
	}

	/**
	 * Set a consumer of strings that reports them as errors
	 * @param errorHandler the errorHandler to set
	 */
	public void setErrorHandler(Consumer<String> errorHandler) {
		this.errorHandler = errorHandler;
	}

	
	/**
	 * Get a URL for WAR or data of a given version 
	 * @return
	 */
	public String getVersionedURL(String type) {
		String versions = Utils.getProperty("version.base");
		String version  = properties.getProperty("version");
		String file 	= Utils.getProperty(type);
		StringBuilder url = new StringBuilder();
		
		url.append(versions);
		if(!versions.endsWith("/"))
			url.append("/");
		url.append(version);
		if(!version.endsWith("/"))
			url.append("/");
		url.append(file);
		
		return url.toString();
	}
	
	// download methods
	

	/**
	 * Download a listing (HTTP server generated index) from remote server
	 * @param type			installation parameter holding directory URL
	 * @param progressable	for monitoring download
	 * @return
	 */
	public List<String> downloadListing(String type,Progressable progressable){
		String url = Utils.getProperty(type);
		Path path = Utils.dowloadFrom(url, ".zip", progressable);
		List<String> listing = null;
		
		cleanLater(path);
		try {
			listing = Utils.parseListing(path);
		} catch (IOException e) {
			errorHandler.accept(e.toString());
		} finally {
			cleanup();
		}
		
		return listing;
	}
	
	/**
	 * Download ZIP file from given URL
	 * @param url
	 * @param progressable
	 * @return
	 */
	public Path downloadZipFrom(String url,Progressable progressable)  {	
		Path path = null;
		
		path = Utils.dowloadFrom(url, ".zip", progressable);
		cleanLater(path);
		
		return path;
	}
	
	/**
	 * Download ZIP file of given type (URL in installer properties)
	 * @param type
	 * @param progressable
	 * @return
	 */
	public Path downloadZip(String type,Progressable progressable)  {	
		String url = Utils.getProperty(type);
		return downloadZipFrom(url,progressable);
	}
	
	/**
	 * Expand ZIP file to target directory, 
	 * making that directory first, if necessary.
	 * The progress of this operation is reported to progressable 
	 * @param zipSource
	 * @param target
	 * @param makeTarget
	 * @param progressable
	 */
	public void expandZip(Path zipSource,Path target,
			boolean makeTarget,Progressable progressable) {

		try {
			try {
				ZipFile zipFile = new ZipFile(zipSource.toFile());
				cleanLater(zipSource);
				if(makeTarget) {
					Files.createDirectories(target);
					Utils.extractTo(zipFile,target, progressable);
				} else {
					Path root = Utils.extractTo(zipFile,target.getParent(), 
							progressable);
					if(root != null)
						Files.move(root, target);
				}
				
			} finally {
				cleanup();
			}
		} catch (IOException e) {
			errorHandler.accept(e.toString());
		}	

	}
	
	/**
	 * Download servlet container to given path
	 * @param path
	 * @throws IOException
	 */
	@Deprecated
	public void downloadServletContainer(Progressable progressable)  {		
		String url = Utils.getProperty("container.url");
		
		try {
			Path tmp = Utils.dowloadFrom(url, ".zip", progressable);
			cleanLater(tmp);
			try {
				ZipFile zipFile = new ZipFile(tmp.toFile());
				Utils.extractAs(zipFile,servletContainerPath);
			} finally {
				cleanup();
			}
		} catch (IOException e) {
			errorHandler.accept(e.toString());
		}				
	}

	/**
	 * Download Mooshak's WAR file (a zip)
	 * @param progressable
	 */
	@Deprecated
	public void downloadMooshakWAR(Progressable progressable)  {
		String url = Utils.getProperty("webapp.url");
		
		try {
			Path tmp = Utils.dowloadFrom(url, ".zip", progressable);
			Path webapp = getMooshakWebAppFolder();
			
			cleanLater(tmp);
			try {
				ZipFile zipFile = new ZipFile(tmp.toFile());
				Files.createDirectories(webapp);
				Utils.extractTo(zipFile,webapp);
			} finally {
				cleanup();
			}
		} catch (IOException e) {
			errorHandler.accept(e.toString());
		}
		
		if(Utils.isRoot())
			grantPermissions();
		else
			errorHandler.accept("Could not grant root permissions to safeexec");
	}
	
	
	Set<Path> tempFiles = new HashSet<>();
	
	private void cleanLater(Path file) {
		tempFiles.add(file);
	}
	
	/**
	 * Cleanup temporary files
	 */
	public void cleanup() {
		
		tempFiles.forEach( path -> {
			try {
				Files.deleteIfExists(path);
			} catch (IOException e) {
				errorHandler.accept(e.toString());
			}
		});

	}
	
	
	/**
	 * Grant permissions to all files with name starting in safeexec
	 * in binary directory of the webapp 
	 */
	public void grantPermissions() {
		
		try {
			Files
			.list(getWebappBinFolder())
			.filter( t-> {return t.startsWith("safeexec");} )
			.forEach( p -> { try {
						runtime.exec("chmod u+s,o+x "+p.toString());
					} catch (Exception cause) {
						errorHandler.accept(cause.getLocalizedMessage());
					}});
		} catch (IOException cause) {
			errorHandler.accept(cause.toString());
		}
	}
	
	/**
	 * Helper class for testing download
	 * 
	 * @author José Paulo Leal <zp@dcc.fc.up.pt>
	 */
	private class DummyProgressable implements Progressable {
		static final int MAX = 10;
		
		int part = 0;
		boolean error = false;
				
		@Override
		public void updatable(boolean isUpdatable) {}
		@Override
		public int getMaximum() {
			return MAX;
		}

		@Override
		public void update(int part) {
			this.part = part;
		}

		@Override
		public void taskError(String message) {
			error=true;
		}
		
		boolean isComplete() {
			return part==MAX && ! error;
		}
	}
	
	/**
	 * Check if servlet container is running
	 * @return
	 */
	public boolean isServletContainerRunning() {
		String url = Utils.getProperty("container.url");
		DummyProgressable dummy = new DummyProgressable();
		
		Path html = Utils.dowloadFrom(url, ".html",dummy);
		cleanLater(html);
		
		return dummy.isComplete();
	}
	
	/**
	 * Start the servlet container 
	 */
	public void startServletContainer() {
		String system = Utils.isWindows() ? "window" : "linux";
		String startCommand  = Utils.getProperty("container.start."+system);
		Path startCommandPath = getServletContainerPath().resolve(startCommand);
		
		try {
			runtime.exec(BASH+" "+startCommandPath.toString());
		} catch (Exception cause) {
			errorHandler.accept(cause.getLocalizedMessage());
		};
	}
	
	/**
	 * Download remote content to the specified location
	 * @throws IOException
	 */
	public void downloadMooshakContent(Progressable progressable) {
		String url = Utils.getProperty("content.url");
		try {
			Path tmp = Utils.dowloadFrom(url, ".zip", progressable);
		
			try {
				ZipFile zipFile = new ZipFile(tmp.toFile());
				Utils.extractAs(zipFile,homeDirectory);
			} finally {
				Files.deleteIfExists(tmp);
			}
		} catch (IOException e) {
			errorHandler.accept(e.toString());
		}

	}	
	
	// Special locations

	/**
	 * Get webapps folder of servlet container
	 * @return path to webapps folder
	 */
	private Path getWebAppsFolder() {
		String name = getProperty("container.webapps");
		return servletContainerPath.resolve(name);
	}
	
	public Path getMooshakWebAppFolder() {
		String name = getProperty("webapp.name");
		return getWebAppsFolder().resolve(name);
	}
	
	private Path getWebappBinFolder() {
		String name = getProperty("webapp.bin");
		return getMooshakWebAppFolder().resolve(name);	
	}

	/**
	 * get Mooshak's property file
	 * @return
	 */
	private Path getPropertyFile() {
		
		String name = getProperty("properties.name");
		return getWebAppsFolder().resolve(name);		
	}
	
	
}
