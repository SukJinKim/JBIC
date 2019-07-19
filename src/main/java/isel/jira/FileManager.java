package isel.jira;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.jsoup.Connection;

public class FileManager {
	private String path;
	private String domain;
	private String projectKey;
	
	public FileManager(String path, String domain, String projectKey) {
		super();
		this.path = path;
		this.domain = domain;
		this.projectKey = projectKey;
	}
	
	public void storeCSVFile(Connection.Response response) throws IOException {
		Date date= new Date();
		Timestamp ts = new Timestamp(date.getTime());
		
		String teamName = this.domain.substring(this.domain.indexOf('.') + 1, this.domain.lastIndexOf('.')); //TeamName is between . marks in domain.
		String dir = this.path + File.separator + teamName + this.projectKey + File.separator;
		String savedFileName = dir + teamName + this.projectKey + ts + ".csv";
		String simpleFileName = savedFileName.substring(savedFileName.lastIndexOf(File.separator)+1);
		
		System.out.println("\n\tFile " + simpleFileName +" is to be downloaded in " + dir);
		byte[] bytes = response.bodyAsBytes();
		File savedFile = new File(savedFileName);
		savedFile.getParentFile().mkdirs();
		FileUtils.writeByteArrayToFile(savedFile, bytes);
		System.out.println("\tFile " + simpleFileName +" has been downloaded in " + dir);
	}
}
