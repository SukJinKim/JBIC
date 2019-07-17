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
	private String domain;
	private String projectKey;
	private boolean help;

	public static void main(String[] args) {
		JBICDemonstrator jbicDemonstrator = new JBICDemonstrator();
		try {
			jbicDemonstrator.run(args);
		} catch(ParseException e) {
			System.err.println("\nParsing failed.\n\tReason - " + e.getMessage());
		}catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InvalidDomainException e1) {
			System.err.println("\nDomain is invalid.\n");
		}
	}
	
	//Definition stage
	//TODO verbose mode 추가
	private Options createOptions() {
		Options options = new Options();
		
		options.addOption(Option.builder("d")
								.hasArg()
								.required()
								.longOpt("domain")
								.desc("Set domain in URL (ex. issues.apache.org)")
								.build());
		
		options.addOption(Option.builder("p")
								.hasArg()
								.required()
								.longOpt("projectKey")
								.desc("Set project key")
								.build());
		
		options.addOption(Option.builder("h")
								.longOpt("help")
								.desc("Help")
								.build());

		return options;
	}
	
	//TODO header에 description 추가
	private void printHelp(Options options){
		String header = "...Description about JiraBugIssueCrawler...\n\n"; 
		String footer = "\nPlease report issues at https://github.com/HGUISEL/JiraCrawler/issues\n\n";
		
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("JiraBugIssueCrawler", header, options, footer, true);
	}
	
	private boolean parseOptions(Options options, String[] args) {
		CommandLineParser parser = new DefaultParser();
		
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
			
			domain = cmd.getOptionValue('d');
			projectKey = cmd.getOptionValue('p');
			help = cmd.hasOption('h');
			
		} catch (ParseException e) {
			printHelp(options);
			return false;
		}
		
		return true;
	}
	
	private void run(String[] args) throws ParseException, IOException, InvalidDomainException {
		Options options = createOptions();
		
		if(parseOptions(options, args)){
			if (help){
				printHelp(options);
				return;
			}
			
			System.out.println("\n\tYou provided " + domain + " as the value of the optino d");
			System.out.println("\tYou provided " + projectKey + " as the value of the optino p");
			JiraBugIssueCrawler jiraBugIssueCrawler = new JiraBugIssueCrawler(domain, projectKey);
			jiraBugIssueCrawler.run();
			
		}
	}
}
