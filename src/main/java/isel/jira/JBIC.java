package isel.jira;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.io.FileUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

public class JBIC {
	private String domain;
	private String projectKey;
	
	private static final String DIR = "FILES/";
	
	public JBIC(String domain, String projectKey) {
		super();
		if(domain.equals("issues.apache.org")) {//TODO Is it good software design..?
			domain = domain.concat("/jira");
		}
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
		int start = -500;
		int end = 1;
		int fileCount = 1;
		
		String jql1 = "project = " + this.projectKey + " AND issuetype = Bug AND resolution = fixed AND created <= startOfDay(" + end +")";
		String jql2 = "project = " + this.projectKey + " AND issuetype = Bug AND resolution = fixed AND created <= startOfDay(" + end +") AND created > startOfDay(" + start + ")";
		
		String linkUrl1 = "https://" + this.domain + "/sr/jira.issueviews:searchrequest-csv-all-fields/temp/SearchRequest.csv?jqlQuery=" + encodeJql(jql1);
		String linkUrl2 = "https://" + this.domain + "/sr/jira.issueviews:searchrequest-csv-all-fields/temp/SearchRequest.csv?jqlQuery=" + encodeJql(jql2);
		
		Connection.Response response = getResponse(linkUrl1);
		
		boolean flag1 = requestSucceed(response.statusCode()); 
		
		while(!flag1) {
			response = getResponse(linkUrl2);
			
			boolean flag2 = requestSucceed(response.statusCode());
			boolean originalFlag2 = flag2;
			
			while(flag2 == originalFlag2) {
				if(flag2) { 
				//flag2가 성공했다. = 아직 범위를 넓힐 수 있다.
					start = increasePeriod(start, end);
					System.out.println("\n\tSearching bug issues from " + start + " days to " + end + " days");
					System.out.println("\tIncreasing period...");
				}else {
					start = decreasePeriod(start, end);
					System.out.println("\n\tSearching bug issues from " + start + " days to " + end + " days");
					System.out.println("\tDecreasing period...");
				}
				////////////////
				jql1 = "project = " + this.projectKey + " AND issuetype = Bug AND resolution = fixed AND created <= startOfDay(" + end +")";
				jql2 = "project = " + this.projectKey + " AND issuetype = Bug AND resolution = fixed AND created <= startOfDay(" + end +") AND created > startOfDay(" + start + ")";
						
				linkUrl1 = "https://" + this.domain + "/sr/jira.issueviews:searchrequest-csv-all-fields/temp/SearchRequest.csv?jqlQuery=" + encodeJql(jql1);
				linkUrl2 = "https://" + this.domain + "/sr/jira.issueviews:searchrequest-csv-all-fields/temp/SearchRequest.csv?jqlQuery=" + encodeJql(jql2);
				/////////////////
				response = getResponse(linkUrl2);
				
				flag2 = requestSucceed(response.statusCode());
			}
			//originalFlag2가 true이면 flag2는 계속 증가,false이면 계속 감소했다는 뜻. 따라서 각각 한 단계 감소, 증가해야 함.
			start = recoverPeriod(originalFlag2, start, end);
			////////////////
			jql1 = "project = " + this.projectKey + " AND issuetype = Bug AND resolution = fixed AND created <= startOfDay(" + end +")";
			jql2 = "project = " + this.projectKey + " AND issuetype = Bug AND resolution = fixed AND created <= startOfDay(" + end +") AND created > startOfDay(" + start + ")";
					
			linkUrl1 = "https://" + this.domain + "/sr/jira.issueviews:searchrequest-csv-all-fields/temp/SearchRequest.csv?jqlQuery=" + encodeJql(jql1);
			linkUrl2 = "https://" + this.domain + "/sr/jira.issueviews:searchrequest-csv-all-fields/temp/SearchRequest.csv?jqlQuery=" + encodeJql(jql2);
			/////////////////
			
			response = getResponse(linkUrl2);
			if(requestSucceed(response.statusCode())) {
				//파일을 만든다.
				String teamName = this.domain.substring(this.domain.indexOf('.') + 1, this.domain.lastIndexOf('.'));
				String savedFileName = DIR.concat(teamName + this.projectKey + fileCount).concat(".csv");
				fileCount++;
				storeCSVFile(response, savedFileName);
			}else {
				System.err.println("\n\t...Fatal Error..."); //TODO 이거 어떻게처리하지...?
			}
			
			//start와 end 값 수정하고 jql1 던지기
			end = start;
			start = start - 100;
			////////////////
			jql1 = "project = " + this.projectKey + " AND issuetype = Bug AND resolution = fixed AND created <= startOfDay(" + end +")";
			jql2 = "project = " + this.projectKey + " AND issuetype = Bug AND resolution = fixed AND created <= startOfDay(" + end +") AND created > startOfDay(" + start + ")";
					
			linkUrl1 = "https://" + this.domain + "/sr/jira.issueviews:searchrequest-csv-all-fields/temp/SearchRequest.csv?jqlQuery=" + encodeJql(jql1);
			linkUrl2 = "https://" + this.domain + "/sr/jira.issueviews:searchrequest-csv-all-fields/temp/SearchRequest.csv?jqlQuery=" + encodeJql(jql2);
			/////////////////
			response = getResponse(linkUrl1);
			flag1 = requestSucceed(response.statusCode());
		}
		
		//파일 만들고 종료
		String teamName = this.domain.substring(this.domain.indexOf('.') + 1, this.domain.lastIndexOf('.'));
		String savedFileName = DIR.concat(teamName + this.projectKey + fileCount).concat(".csv");
		fileCount++;
		storeCSVFile(response, savedFileName);
	}
	
	private int recoverPeriod(boolean originalFlag2, int start, int end) {
		int period = Math.abs(start - end);
		int val;
		if(originalFlag2) {//true이면 한 단계 감소해야 함.
			val = end - period / 2;
		}else {
			val = end - period * 4/3;
		}
		return val;
	}

	//TODO File.separator 적용하기. response를 입력받으면 file을 만든다.
	private static void storeCSVFile(Connection.Response response, String savedFileName) throws IOException {
		String simpleFileName = savedFileName.substring(savedFileName.indexOf("/")+1);
		System.out.println("\n\tFile " + simpleFileName +" is to be downloaded.");
		byte[] bytes = response.bodyAsBytes();
		File savedFile = new File(savedFileName);
		savedFile.getParentFile().mkdirs();
		FileUtils.writeByteArrayToFile(savedFile, bytes);
		System.out.println("\tFile " + simpleFileName +" has been downloaded.");
	}
	
	//URL을 받으면 연결해서 response를 return하는 method
	private static Connection.Response getResponse(String url) throws IOException{
		System.out.println("\nConnecting " + url + "...");
		return Jsoup.connect(url)
				.maxBodySize(0)
				.timeout(600000)
				.ignoreHttpErrors(true)
				.execute();
	}
	
	//status를 받아서 request의 성공여부를 확인하는 method [requestSucceed(int statusCode)]
	private static boolean requestSucceed(int statusCode) {
		return (statusCode / 100 == 2); //status code 2xx means that request has been succeeded.
	}
	
	//접근 불가일 때 period를 줄이는 method[decreasePeriod (int start, int end)]
	private static int decreasePeriod(int start, int end) {
		int period = Math.abs(start-end);
		int val = end - period * 3/4;
		return val;
	}
	
	//접근 가능일 때 period를 늘리는 method [increasePeriod (int start, int end)]
	private static int increasePeriod(int start, int end) {
		int period = Math.abs(start-end);
		int val = end - period * 2;
		return val;
	}
	
	//encoding한 JQL을 return하는 method[encodeJQL(String JQL)]
	private static String encodeJql(String jql) throws UnsupportedEncodingException {
		return URLEncoder.encode(jql, "UTF-8");	
	}
}
