package pt.up.fc.dcc.mooshak.installer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {

	static Pattern endsInZip = Pattern.compile("/([^/]*).zip");
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Path path = Paths.get("http://mirrors.fe.up.pt/pub/apache/tomcat/tomcat-7/v7.0.53/bin/apache-tomcat-7.0.53.zip");
		
		Matcher matcher = endsInZip.matcher(path.toString());
		if(matcher.find())
			System.out.println(matcher.group(1));
	}

}
