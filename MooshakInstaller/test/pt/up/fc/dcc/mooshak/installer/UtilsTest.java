package pt.up.fc.dcc.mooshak.installer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static pt.up.fc.dcc.mooshak.installer.Utils.getProperty;
import static pt.up.fc.dcc.mooshak.installer.Utils.getPropertyValues;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pt.up.fc.dcc.mooshak.installer.Driver.Progressable;

public class UtilsTest {

	Path to = Paths.get("/usr/tmp/tomcat7");
	
	@Before
	public void setUp() throws Exception {
		cleanup(to);
	}
	
	@After
	public void tearDown() throws Exception {
		cleanup(to);
	}
	
	private void cleanup(Path path) throws IOException {
			Files.walkFileTree(path, new FileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir,
						BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file,
						BasicFileAttributes attrs) throws IOException {
					Files.deleteIfExists(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file,
						IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir,
						IOException exc) throws IOException {
					Files.deleteIfExists(dir);
					return FileVisitResult.CONTINUE;
				}
				
			});
	}

	@Test
	public void testGetProperty() {
		assertEquals("data",getProperty("content.root"));
	}
	
	@Test
	public void testGetPropertyValues() {
		assertEquals("/usr/share/tomcat7",getPropertyValues("container.locations")[0]);
		assertEquals(3,getPropertyValues("container.locations").length);
		assertEquals("/home/mooshak",getPropertyValues("home.locations")[0]);
	}

	class MyProgress implements Progressable {
		int maximum;
		List<Integer> parts = new ArrayList<>();
		
		MyProgress(int maximum) {
			this.maximum = maximum;
		}
		
		@Override
		public void updatable(boolean isUpdatable) {

		}
		
		@Override
		public void taskError(String message) {
			System.out.println(message);
		}
		
		@Override
		public int getMaximum() {
			return maximum;
		}

		@Override
		public void update(int part) {
			parts.add(part);
		}
	}
	
	@Test
	public void testExtractTo() throws IOException {
		String url = getProperty("container.url");
		MyProgress myProgress = new MyProgress(10);
		
		Path zipPath = Utils.dowloadFrom(url,".zip",myProgress);
		
		assertEquals(Arrays.asList(1,2,3,4,5,6,7,8,9,10),myProgress.parts);
		
		ZipFile zipFile = new ZipFile(zipPath.toFile());
		
		Path root = Utils.extractTo(zipFile, to.getParent());
		assertTrue(Files.isDirectory(root));
		
		Files.deleteIfExists(zipPath);
		cleanup(root);
	}
	
	@Test
	public void testExtractAs() throws IOException {
		String url = getProperty("container.url");
		MyProgress myProgress = new MyProgress(5);
		
		Path zipPath = Utils.dowloadFrom(url,".zip",myProgress);
		
		assertEquals(Arrays.asList(1,2,3,4,5),myProgress.parts);
		
		ZipFile zipFile = new ZipFile(zipPath.toFile());
		
		Utils.extractAs(zipFile, to);
		assertTrue(Files.isDirectory(to));

		Files.deleteIfExists(zipPath);
		cleanup(to);
	}
	
	
	@Test
	public void testParseListing() throws IOException {
		String url = getProperty("versions");
		MyProgress myProgress = new MyProgress(5);
		Path tmp = Utils.dowloadFrom(url,".html",myProgress);
		List<String> expected = Arrays.asList("2.0");
		
		assertEquals(expected,Utils.parseListing(tmp));
	}
	
	@Test
	public void testExpandCamelCase() {
		assertEquals("Hello World",Utils.expandCamelCase("helloWorld"));
		assertEquals("Home Directory",Utils.expandCamelCase("homeDirectory"));
		assertEquals("Version",Utils.expandCamelCase("version"));
		assertEquals("Version",Utils.expandCamelCase("Version"));
	}
	
	@Test
	public void testCompareversions() {
		
		assertTrue(Utils.compareVersions("2",   "2")     == 0);
		assertTrue(Utils.compareVersions("2",   "3")      < 0);
		assertTrue(Utils.compareVersions("2",   "2.")    == 0);
		assertTrue(Utils.compareVersions("2.",   "2")    == 0);
		assertTrue(Utils.compareVersions("2",   "3.")     < 0);
		assertTrue(Utils.compareVersions("2.",   "3")     < 0);
		assertTrue(Utils.compareVersions("3",   "2.")     > 0);
		assertTrue(Utils.compareVersions("3.",   "2")     > 0);

		assertTrue(Utils.compareVersions("2.0", "2.0")   == 0);
		assertTrue(Utils.compareVersions("2.1", "2.0")    > 0);
		assertTrue(Utils.compareVersions("2.1", "2.2")    < 0);
		assertTrue(Utils.compareVersions("3.1", "2.2")    > 0);
		assertTrue(Utils.compareVersions("3.1", "4.2")    < 0);
		assertTrue(Utils.compareVersions("2.1", "2.1.1")  < 0);
		assertTrue(Utils.compareVersions("2.1.1", "2.1")  > 0);
		
		List<String> versions = Arrays.asList(
				"2", "2.0.1", "2.3", "2.1", "2.3.1", "2.0.2", "2.2", "2.2.2");
		
		List<String> expected = Arrays.asList(
				"2.3.1", "2.3", "2.2.2", "2.2", "2.1", "2.0.2", "2.0.1", "2");
		
		assertFalse(expected.equals(versions));
		Collections.sort(versions,Utils::compareVersions);
		Collections.reverse(versions);
		assertEquals(expected,versions);
		
		
		
	}
}
