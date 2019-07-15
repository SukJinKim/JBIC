package isel.jira;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

public class JBIC {
	private String domain;
	private String projectKey;
	
	private static final String DIR = "files/";
	
	public JBIC(String domain, String projectKey) {
		super();
		this.domain = domain;
		this.projectKey = projectKey;
	}
	
	public String getDomain() {
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
	}
	public String getProjectKey() {
		return projectKey;
	}
	public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}
	
	public void run() throws IOException {
		int start = -100;
		int end = 1;
		
		String linkUrl = "https://" + this.domain + "/sr/jira.issueviews:searchrequest-csv-all-fields/temp/SearchRequest.csv?jqlQuery="; //+EncodedJQL
		
		Connection.Response response = Jsoup.connect(linkUrl)
				.maxBodySize(0)
				.timeout(600000)
				.ignoreHttpErrors(true)
				.execute();
		
		int statusCode = response.statusCode();
		byte[] bytes = response.bodyAsBytes();
		String savedFileName = DIR.concat(linkUrl.substring(linkUrl.indexOf("jqlQuery")).concat(".csv"));
		File savedFile = new File(savedFileName);
		savedFile.getParentFile().mkdirs();
		
		FileUtils.writeByteArrayToFile(savedFile, bytes);
		
		System.out.println("File " + savedFileName +" has been downloaded.");
	}
	
	
	//status를 받아서 request의 성공여부를 확인하는 method [requestSucceed(int statusCode)]
	
	//접근 불가일 때 period를 줄이는 method[decreasePeriod (int start, int end)]
	
	//접근 가능일 때 period를 늘리는 method [increasePeriod (int start, int end)]
	
	//JQL type1을 return하는 method [getJqlType(int end)] @overloading 이용
	//JQL type1 - project = <projectKey> AND statusCategory = Done AND issuetype = Bug And created <= startOfDay(<end>)
	
	//JQL type2를 return하는 method [getJqlType(int start, int end)] @overloading 이용
	//JQL type2 - project = <projectKey> AND statusCategory = Done AND issuetype = Bug And created <= startOfDay(<end>) AND created > startOfDay(<start>)
	
	//encoding한 JQL을 return하는 method[encodeJQL(String JQL)]
	

}
