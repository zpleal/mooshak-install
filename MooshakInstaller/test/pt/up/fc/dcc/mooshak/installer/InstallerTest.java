package pt.up.fc.dcc.mooshak.installer;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

public class InstallerTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void listProperties() {
		
		Properties properties = System.getProperties();
		
		properties.keySet().forEach(
				p -> { System.out.println(p+":"+properties.get(p));});
		
		
	}

}
