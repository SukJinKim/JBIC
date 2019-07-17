package isel.jira;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

public class JiraBugIssueCrawler {
	private String domain;
	private String projectKey;
	
	private static final String DIR = "FILES" + File.separator;
	private static final int INITIAL_START = -500;
	private static final int INITIAL_END = 1;
	private static final int PERIOD = Integer.parseUnsignedInt("500");
	
	public JiraBugIssueCrawler(String domain, String projectKey) throws InvalidDomainException {
		super();
		this.domain = validateDomain(domain);
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
		Period period = new Period(INITIAL_START, INITIAL_END);
		JQLManager jqlManager = new JQLManager(this.projectKey);
		URLManager urlManager = new URLManager(this.domain);
		
		int fileCount = 1;
		
		String encodedJql = jqlManager.getEncodedJQL(jqlManager.getJQL1(period.getEnd()));
		String linkUrl = urlManager.getURL(encodedJql);
		Connection.Response response = getResponse(linkUrl);
		sendMessage1(period.getEnd());
		
		boolean flag1 = requestSucceed(response.statusCode()); 
		
		while(!flag1) {
			encodedJql = jqlManager.getEncodedJQL(jqlManager.getJQL2(period.getStart(), period.getEnd()));
			linkUrl = urlManager.getURL(encodedJql);
			response = getResponse(linkUrl);
			sendMessage2(period.getStart(), period.getEnd());
	
			boolean flag2 = requestSucceed(response.statusCode());
			
			boolean originalFlag2 = flag2;
			int originalStart = period.getStart();
			
			while(flag2 == originalFlag2) {
				originalStart = period.getStart();
				if(flag2) { 
					period.increasePeriod();
					System.out.println("\tIncreasing period...");
				}else {
					period.decreasePeriod();
					System.out.println("\tDecreasing period...");
				}
			
				encodedJql = jqlManager.getEncodedJQL(jqlManager.getJQL2(period.getStart(), period.getEnd()));
				linkUrl = urlManager.getURL(encodedJql);
				response = getResponse(linkUrl);
				sendMessage2(period.getStart(), period.getEnd());
				
				flag2 = requestSucceed(response.statusCode());
			}
			
			period.setStart(originalStart);
			
			encodedJql = jqlManager.getEncodedJQL(jqlManager.getJQL2(period.getStart(), period.getEnd()));
			linkUrl = urlManager.getURL(encodedJql);
			response = getResponse(linkUrl);
			sendMessage2(period.getStart(), period.getEnd());
			
			if(requestSucceed(response.statusCode())) {
				//파일을 만든다.
				String teamName = this.domain.substring(this.domain.indexOf('.') + 1, this.domain.lastIndexOf('.'));
				String savedFileName = DIR.concat(teamName + this.projectKey + fileCount).concat(".csv");
				fileCount++;
				storeCSVFile(response, savedFileName);
			}else {
				System.err.println("\n\t...Fatal Error..."); //TODO 이거 어떻게처리하지...?
			}
			
			period.movePeriod(PERIOD);
			
			encodedJql = jqlManager.getEncodedJQL(jqlManager.getJQL1(period.getEnd()));
			linkUrl = urlManager.getURL(encodedJql);
			
			response = getResponse(linkUrl);
			sendMessage1(period.getEnd());
			
			flag1 = requestSucceed(response.statusCode());
		}
		
		//파일 만들고 종료
		String teamName = this.domain.substring(this.domain.indexOf('.') + 1, this.domain.lastIndexOf('.'));
		String savedFileName = DIR.concat(teamName + this.projectKey + fileCount).concat(".csv");
		fileCount++;
		storeCSVFile(response, savedFileName);
	}
	
	//url2를 접근할 때 user에게 메시지 보내기
	private static void sendMessage2(int start, int end) {
		System.out.println("\n\tSearching bug issues from " + start + " days to " + end + " days");
	}
	
	//url1을 접근할 때 user에게 메시지 보내기
	private static void sendMessage1(int end) {
		System.out.println("\n\tSearching bug issues before " + end + " days");
	}
	
	private static String validateDomain(String domain) throws InvalidDomainException {
		String str = domain;
		Pattern p = Pattern.compile("(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z0-9][a-z0-9-]{0,61}[a-z0-9]");
		Matcher m = p.matcher(str);
		
		if(!m.find()) {
			throw new InvalidDomainException();
		}
		
		if(str.equals("issues.apache.org")) {//apache의 경우 뒤에 '/jira'가 붙음.
			str = str.concat("/jira");
		}
		
		return str;
	}
	
	private static void storeCSVFile(Connection.Response response, String savedFileName) throws IOException {
		String simpleFileName = savedFileName.substring(savedFileName.indexOf(File.separator)+1);
		System.out.println("\n\tFile " + simpleFileName +" is to be downloaded.");
		byte[] bytes = response.bodyAsBytes();
		File savedFile = new File(savedFileName);
		savedFile.getParentFile().mkdirs();
		FileUtils.writeByteArrayToFile(savedFile, bytes);
		System.out.println("\tFile " + simpleFileName +" has been downloaded.");
	}
	
	private static Connection.Response getResponse(String url) throws IOException{
		System.out.println("\nConnecting " + url + "...");
		return Jsoup.connect(url)
				.maxBodySize(0)
				.timeout(600000)
				.ignoreHttpErrors(true)
				.execute();
	}
	
	private static boolean requestSucceed(int statusCode) {
		return (statusCode / 100 == 2); //status code 2xx means that request has been succeeded.
	}
}
