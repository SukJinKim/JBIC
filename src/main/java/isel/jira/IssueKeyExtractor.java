package isel.jira;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class IssueKeyExtractor {
	private String projectName;

	public IssueKeyExtractor(String projectName) {
		super();
		this.projectName = projectName;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	
	public String getIssueKey() {
		String url = "https://issues.apache.org/jira/projects/" + this.projectName + "/issues/";
		Document doc = null;
		String issueKey = null;
		
		try {
			doc = Jsoup.connect(url) .get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String elements = doc.select("*").toString();
		String regex = this.projectName + "-\\d+";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(elements);
		
		//TODO exception handling when matcher.find() is false.
		if(matcher.find()) {
			issueKey = matcher.group();
		}
		
		return issueKey;
	}
}