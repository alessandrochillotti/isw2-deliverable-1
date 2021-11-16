package it.uniroma2.alessandrochillotti.isw2.deliverable_1;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class IssueAnalyzer {

	private static final Logger LOGGER = Logger.getLogger("Outcome");
	private static final String PROJECT_NAME = "STDCXX";
	private static final int WINDOW_SIZE = 50;
	private static final String FILE_NAME = "record.csv";
	private static final String DATE_FORMAT = "yyyy-MM-dd";
	
	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		
		while ((cp = rd.read()) != -1) sb.append((char) cp);
		
		return sb.toString();
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		
		try (
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			)
			{String jsonText = readAll(rd);
			
			return new JSONObject(jsonText);
		}
	}

	/*
	 *	This method return a record that contains, for each <year,month>, the number of issue
	 *	solved in that period. 
	 */
	public Map<String, Integer> prepareContent(LocalDate[] dates) {

		Map<String, Integer> content = new LinkedHashMap<>();
		DateManager dateMan = new DateManager();
		
		LocalDate mostRecentDate = dateMan.mostRecentDate(dates, true);
		LocalDate iterableDate = dateMan.oldestDate(dates, true);

		// Prepare map with all keys of "yyyy-mm" from oldestDate to mostRecentDate
		while(mostRecentDate.getMonthValue() != iterableDate.getMonthValue() || mostRecentDate.getYear()  != iterableDate.getYear()) {
			content.put(iterableDate.toString().substring(0,7), 0);
			iterableDate = dateMan.addOneMonth(iterableDate);
		} 
		content.put(mostRecentDate.toString().substring(0,7), 0);

		for (int i = 0; i < dates.length; i++) {
			content.replace(dates[i].toString().substring(0, 7), content.get(dates[i].toString().substring(0, 7)) + 1);
		}
		
		return content;

	}

	/*
	 * 	This method allow to write into file csv the result of query JIRA.
	 */
	public void writeFile(Map<String, Integer> result) {
		
		try 
		(
			FileWriter fileWriter = new FileWriter(FILE_NAME);
		){
			fileWriter.append("Date, #Issue");
			fileWriter.append("\n");
			for (Map.Entry<String, Integer> entry : result.entrySet()) {
				fileWriter.append(entry.getKey());
				fileWriter.append(",");
				fileWriter.append(entry.getValue().toString());
				fileWriter.append("\n");
			}
		} catch (Exception e) {
			 e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws IOException, JSONException {
		Integer j = 0;
		Integer i = 0;
		Integer total = 0;
		
		IssueAnalyzer issueAnalyzer = new IssueAnalyzer();
		LocalDate[] dates = null;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
		
		do {
			// Only gets a max of WINDOW_SIZE at a time, so must do this multiple times if bugs > WINDOW_SIZE
			j = i + WINDOW_SIZE;

			// Prepare URL of query for fixed tickets
			String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22" + PROJECT_NAME
					+ "%22AND%22resolution%22=%22fixed%22%20ORDER%20BY%20\"resolutiondate\"%20ASC&fields=resolutiondate,created&startAt=" + i.toString()
					+ "&maxResults=" + j.toString();
			
			JSONObject json = readJsonFromUrl(url);
			JSONArray issues = json.getJSONArray("issues");

			total = json.getInt("total");
			if (j == WINDOW_SIZE) dates = new LocalDate[total];
			
			for (; i < total && i < j; i++) {
				String datetime = issues.getJSONObject(i % WINDOW_SIZE).getJSONObject("fields").getString("resolutiondate");

				// Create array of LocalDate ( if for bug sonar )
				if(dates != null) dates[i] = LocalDate.parse(datetime.substring(0, 10), formatter);
				
			}
				
		} while (i < total);
		
		issueAnalyzer.writeFile(issueAnalyzer.prepareContent(dates));
		
		LOGGER.info("Success!");
	}
	
}