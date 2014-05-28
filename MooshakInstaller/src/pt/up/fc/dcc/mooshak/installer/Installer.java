package pt.up.fc.dcc.mooshak.installer;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class Installer {
	
	static final int SHOW_ERROR_TIME	= 10*1000;
	static final String BULLET 			= "\u2022";
	
	Driver driver;
	Configurator configurator = new Configurator();
	
	public static void main(String[] args) {
		Driver driver = new CUIDriver();
		boolean checkRoot = true;
		
		for(String arg: args)
			switch(arg) {
			case "-cui":
				driver = new CUIDriver();
				break;
			case "-gui":
				driver = new GUIDriver();
				break;
			case "-norootcheck":
				checkRoot = false;
				break;
			default:
				throw new RuntimeException("Invalid arg:"+arg);
			}
		
		Installer installer = new Installer(driver,checkRoot);
				
		installer.install();
	}
	
	
	Installer(Driver driver,boolean checkRoot) {
		this.driver = driver;
		
		if( Utils.isWindows())
			fatalError("Installation on windows not supported");
			
		if(! Utils.isRoot() && checkRoot)
			fatalError("Must be root to run Mooshak's installer\n"+
						"or use the -norootcheck command line option"+
						" for testing installation");
				
		
		configurator.setErrorHandler(driver::error);
		driver.init(14);
		
		Runtime.getRuntime().addShutdownHook(new Thread(configurator::cleanup));
	}
	
	private void fatalError(String message) {
		driver.error(message);
		try {
			Thread.sleep(SHOW_ERROR_TIME);
		} catch (InterruptedException e) {}
		System.exit(1);
	}

	

	// Servlet Container installation
	
	private void install() {
		driver.startPanel(1);
		
		driver.say("Checking Mooshak versions avaiable for installation");

		driver.showProgress( p -> {
			List<String> versions = 
					configurator.downloadListing("version.base",p);
			driver.goPanel(() -> selectVersion(versions));
		});
		
		
	}
	
	private void selectVersion(List<String> versions) {
		Collections.sort(versions,Utils::compareVersions);
		Collections.reverse(versions);
		
		driver.startPanel(2);
		
		if(versions.size() == 0)
			fatalError("No Mooshak versions avaliable");
		else {
			driver.askList(
				"Select version",
				versions,
				versions.get(0),
				configurator::setVersion
			);
		}
		
		driver.endPanel(this::checkServletContainer);
		
	}

	private void checkServletContainer() {
		Path path = configurator.getInstalledServletContainerPath();
		 
		driver.startPanel(3);
		
		if(path == null) {
			driver.say("No servlet container found");
			
			driver.askBoolean (
					"Install a new servlet container? ",
					true,
					install -> { 
						if(install) 
							driver.nextPanel(this::downloadServletContainer);
						else 
							driver.nextPanel(this::searchServletContainer);
					} 
			);
			
		} else	{
			driver.say("Servlet container found at "+path);

			driver.askBoolean (
					"Install another servlet container?",
					false,
					install -> { 
						if(install)  
							driver.nextPanel(this::searchServletContainer);
						else {
							configurator.setServletContainerPath(path);
							driver.nextPanel(this::startServletContainer);
						}
					}
			);
		
		}
		driver.endPanel();
	}
	
	
	private void searchServletContainer() {
		driver.startPanel(4);
		
		driver.askPath("Enter servlet container path", 
				configurator.getPreferredServletContainerPath(),
				path -> {
					configurator.setServletContainerPath(path);
					if(configurator.hasServeletContainer(path))
						driver.nextPanel(this::startServletContainer);
				}
		);
		driver.endPanel(this::downloadServletContainer);
	}
	
	
	private void downloadServletContainer() {
		
		driver.startPanel(5);
		

		driver.say("Dowloading servlet container");
						
						
		driver.showProgress( p -> {
					Path zip;
					zip = configurator.downloadZip("container.download",p);
					driver.goPanel( () -> { 
						expandServletContainer(zip);
					});
		});		
	}
		
	
	private void expandServletContainer(Path zip) {
		Path path = configurator.getServletContainerPath();
		
		driver.startPanel(6);
		
		driver.say("Installing servlet container at "+path);
		
		driver.showProgress( p -> {
			configurator.expandZip(zip,path,false,p);
			configurator.setServletContainerVersion(zip);
			driver.goPanel(this::startServletContainer);
		});
	}
	
	
	private void startServletContainer() {
		boolean isRunning = configurator.isServletContainerRunning();
		
		driver.startPanel(7);
		
		driver.say("The servlet container is "+(isRunning?"":"NOT")+" running");
		
		driver.askBoolean (
				"Start servlet container?",
				! isRunning,
				start -> { 
					if(start)  
						configurator.startServletContainer();
				}
		);
		
		driver.endPanel(this::downloadMooshakWAR);
		
	}
	
	
	private void downloadMooshakWAR() {
		
		driver.startPanel(8);
		
		driver.say("Downloading Mooshak's WAR");
		
		driver.showProgress(p -> { 
			String url = configurator.getVersionedURL("version.war");
			Path zip = configurator.downloadZipFrom(url,p);
			driver.goPanel(() -> { expandMooshakWAR(zip); });
		} );
		
	}
	
	private void expandMooshakWAR(Path zip) {
		Path webapp = configurator.getMooshakWebAppFolder();
		driver.startPanel(9);
		
		driver.say("Expanding Mooshak's WAR on servlet container");
		
		driver.showProgress(p -> { 
			configurator.expandZip(zip,webapp,true,p);
			
			if(Utils.isRoot())
				configurator.grantPermissions();
			else
				driver.error("Could not grant root permissions to safeexec");
			
			driver.goPanel(this::content);
		});
	}
	
	
	// Content installation
	
	private void content() {
		Path path;
		
		configurator.loadProperties(); // try reading existing properties
		path = configurator.getExistingtHomeDirectory();
		
		driver.startPanel(10);
		
		if(path == null) {
			driver.say("No home directory found");
			
			driver.askBoolean (
					"Install a new home (or exit)",
					true,
					install -> { 
						if(install) 
							driver.nextPanel(this::installHomeDirectory);
						else 
							System.exit(0);
					} 
			);
			
		} else	{
			driver.say("Home directory found at "+path);

			driver.askBoolean (
					"Install another home directory?",
					false,
					install -> { 
						if(install)
							driver.nextPanel(this::installHomeDirectory);
						else	
							driver.nextPanel(this::downloadContent);
					}
			);
		
		}
		driver.endPanel(() -> {});
	}
	
	private void installHomeDirectory() {
		driver.startPanel(11);
		
		driver.askPath("Enter home directory path", 
				configurator.getPreferredHomeDirectory(),
				path -> {  
					driver.say("Installing home directory at "+path);
					configurator.setHomeDirectory(path);
					configurator.saveProperties();
				});

		driver.endPanel(this::downloadContent);
	
	}
	
	private void downloadContent() {
		
		driver.startPanel(12);
				
		driver.say("Downloading default data");

		driver.showProgress(p -> {
			String url = configurator.getVersionedURL("version.data");
			Path zip = configurator.downloadZipFrom(url,p);
			driver.goPanel(() -> { expandContent(zip); } );
		});
		
	}
	
	
	private void expandContent(Path zip) {
		Path homeDirectory = configurator.getHomeDirectory();
		
		driver.startPanel(13);
		
		driver.say("Expanding default data at "+homeDirectory);
		
		driver.showProgress(p -> {
			configurator.expandZip(zip,homeDirectory,true,p);
			driver.goPanel(this::conclude);
		});
		
	}


	private void conclude() {	
		boolean isRunning = configurator.isServletContainerRunning();
		
		driver.startPanel(14);
		
		configurator.saveProperties();
		
		driver.say("Mooshak was successfully installed"+
				   " with the following configurations");
		
		driver.say(getConfiguration());
		
		driver.say("The servlet container is "+(isRunning?"":"NOT")+" running");
		
		if(! Utils.isRoot()) 
			driver.say("You still need to grant root permissions to safeexec!!");
		else if(isRunning)
			driver.say("You can access this istallation at http://localhost:8080/Mooshak ");
					
		driver.endPanel();
		
		driver.conclude();
	}

	/**
	 * Produce an HTML formated string with relevant 
	 * @return
	 */
	private String getConfiguration() {
		Properties properties = configurator.getProperties();
		StringBuilder configurations = new StringBuilder();
		
		for(Object key: properties.keySet()) {
		
			String label = key.toString();
			String value = properties.getProperty(label);
			
			configurations.append(BULLET);
			configurations.append(' ');
			configurations.append(Utils.expandCamelCase(label));
			configurations.append(':');
			configurations.append(' ');
			configurations.append(value);
			configurations.append("\n");
		}
			
		return configurations.toString();
	}

	
	/**
	 * @return the driver
	 */
	public Driver getDriver() {
		return driver;
	}



	/**
	 * @param driver the driver to set
	 */
	public void setDriver(Driver driver) {
		this.driver = driver;

	}


	
	
}
