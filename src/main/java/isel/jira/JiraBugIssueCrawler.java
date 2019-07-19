package isel.jira;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

public class JiraBugIssueCrawler {
	private String domain;
	private String projectKey;
	
	private static boolean invalidProjectKeyChecker = true;
	private static int disconnectionCausedByInvalidProjectKeyCount = 0;
	
	private static final String DIR = "FILES" + File.separator;
	private static final int INITIAL_START = -1000;
	private static final int INITIAL_END = 1;
	private static final int PERIOD = Integer.parseUnsignedInt("500");
	private static final int MAX_DISCONNECTION = 30; //TODO 수학적으로 공식을 구해서 더 멋있게 코딩할 수 있을 것 같은데...
	
	public JiraBugIssueCrawler(String domain, String projectKey) throws InvalidDomainException {
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
	
	public void run() throws IOException, InvalidProjectKeyException {
		Period period = new Period(INITIAL_START, INITIAL_END);
		JQLManager jqlManager = new JQLManager(this.projectKey);
		URLManager urlManager = new URLManager(this.domain);
		
		String encodedJql = jqlManager.getEncodedJQL(jqlManager.getJQL1(period.getEnd()));
		String linkUrl = urlManager.getURL(encodedJql);
		Connection.Response response = getResponse(linkUrl);
		sendMessage1(period.getEnd());
		
		boolean flag1 = requestSucceed(response.statusCode());  //flag1 is an indicator that checks whether a response was succeeded when approached by JQL1.
		
		while(!flag1) {
			encodedJql = jqlManager.getEncodedJQL(jqlManager.getJQL2(period.getStart(), period.getEnd()));
			linkUrl = urlManager.getURL(encodedJql);
			response = getResponse(linkUrl);
			sendMessage2(period.getStart(), period.getEnd());
	
			boolean flag2 = requestSucceed(response.statusCode()); //flag2 is an indicator that checks whether a response was succeeded when approached by JQL2.
			
			boolean originalFlag2 = flag2; //originalFlag2 is same as value of flag2 before increasing period or decreasing period.
			int originalStart = period.getStart(); //originalStart is same as value of start before increasing period or decreasing period.
			
			while(flag2 == originalFlag2) {
				originalStart = period.getStart();
				if(flag2) { 
					period.increasePeriod();
					System.out.println("\tIncreasing period...");
				}else {
					if(invalidProjectKeyChecker && disconnectionCausedByInvalidProjectKeyCount > MAX_DISCONNECTION) {
						throw new InvalidProjectKeyException();
					}
					period.decreasePeriod();
					System.out.println("\tDecreasing period...");
					disconnectionCausedByInvalidProjectKeyCount++;
				}
				
				encodedJql = jqlManager.getEncodedJQL(jqlManager.getJQL2(period.getStart(), period.getEnd()));
				linkUrl = urlManager.getURL(encodedJql);
				response = getResponse(linkUrl);
				sendMessage2(period.getStart(), period.getEnd());
				
				flag2 = requestSucceed(response.statusCode());
			}
			
			offInvalidProjectKeyChecking(); //From now, there is no possibilities that the user may have entered nonexistent project key.
			
			if(originalFlag2) { 
				period.setStart(originalStart); //recover original value of start only when period was increased.
			}
			
			encodedJql = jqlManager.getEncodedJQL(jqlManager.getJQL2(period.getStart(), period.getEnd()));
			linkUrl = urlManager.getURL(encodedJql);
			response = getResponse(linkUrl);
			sendMessage2(period.getStart(), period.getEnd());
			
			//store CSV file
			String teamName = this.domain.substring(this.domain.indexOf('.') + 1, this.domain.lastIndexOf('.'));
			Date date= new Date();
			Timestamp ts = new Timestamp(date.getTime());
			String savedFileName = DIR.concat(teamName + this.projectKey + ts).concat(".csv");
			storeCSVFile(response, savedFileName);
			
			period.movePeriod(PERIOD);
			
			encodedJql = jqlManager.getEncodedJQL(jqlManager.getJQL1(period.getEnd()));
			linkUrl = urlManager.getURL(encodedJql);
			
			response = getResponse(linkUrl);
			sendMessage1(period.getEnd());
			
			flag1 = requestSucceed(response.statusCode());
		}
		
		//store CSV file
		String teamName = this.domain.substring(this.domain.indexOf('.') + 1, this.domain.lastIndexOf('.'));
		Date date= new Date();
		Timestamp ts = new Timestamp(date.getTime());
		String savedFileName = DIR.concat(teamName + this.projectKey + ts).concat(".csv");
	
		storeCSVFile(response, savedFileName);
	}
	
	private void offInvalidProjectKeyChecking() {
		invalidProjectKeyChecker = false;
	}

	private static void sendMessage2(int start, int end) {
		System.out.println("\n\tSearching bug issues from " + start + " days to " + end + " days");
	}
	
	private static void sendMessage1(int end) {
		System.out.println("\n\tSearching bug issues before " + end + " days");
	}
	
	private static String validateDomain(String domain) throws InvalidDomainException {
		String str = domain;
		String domainRegex = "(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z0-9][a-z0-9-]{0,61}[a-z0-9]";
		
		if(!str.matches(domainRegex)) {
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
