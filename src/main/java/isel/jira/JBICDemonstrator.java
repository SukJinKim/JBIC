package isel.jira;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class JBICDemonstrator {

	public static void main(String[] args) {
		Options options = new Options();
		Option domainOption = Option.builder("d")
						.hasArg()
						.required()
						.longOpt("domain")
						.desc("domain in URL (ex. issues.apache.org)")
						.build();
		
		Option projectKeyOption = Option.builder("p")
							.hasArg()
							.required()
							.longOpt("projectKey")
							.desc("project key")
							.build();
		
		Option helpOption = Option.builder("h")
						.longOpt("help")
						.build();
		
		options.addOption(domainOption);
		options.addOption(projectKeyOption);
		options.addOption(helpOption);String header = "...Description about JiraBugIssueCrawler...\n\n"; //TODO add description
		String footer = "\nPlease report issues at https://github.com/HGUISEL/JiraCrawler/issues\n\n";
		
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("JiraBugIssueCrawler", header, options, footer, true);
		
		CommandLineParser parser = new DefaultParser();
		
		try {
			CommandLine cmd = parser.parse(options, args);
			
			String domain = cmd.getOptionValue('d');
			String projectKey = cmd.getOptionValue('p');
			JiraBugIssueCrawler jiraBugIssueCrawler = new JiraBugIssueCrawler(domain, projectKey);
			jiraBugIssueCrawler.run();
			
		}catch (ParseException e) {
			System.err.println("\nParsing failed.\n\tReason - " + e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidDomainException e) {
			System.err.println("\nDomain is invalid.\n");
		}
	}

}
