package irit.sparql.files;

import irit.sparql.exceptions.NotAFolderException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class FolderManager {
	final File folder;
	Map<String, String> queries;
	Map<String, QueryTemplate> queryTemplates;
	
	public FolderManager(String path) throws NotAFolderException{
		folder = new File(path);
		if(!folder.isDirectory()){
			throw new NotAFolderException(path+" is not a folder");
		}
		queries = new HashMap<>();
		queryTemplates = new HashMap<>();
	}
	
	public void loadQueries(){
		for (File fileEntry : folder.listFiles()) {
	        if (!fileEntry.isDirectory() && fileEntry.getName().endsWith(".sparql")) {
	        	try {
		        	String query = new String(Files.readAllBytes(Paths.get(fileEntry.getPath())));
		        	if(query.replaceAll("\n", " ").matches("^.*\\{\\{ ?([A-Za-z\\d]+) ?}}.*$")){
		        		queryTemplates.put(fileEntry.getName().split("\\.")[0], new QueryTemplate(query));
		        	} else {
		        		queries.put(fileEntry.getName().split("\\.")[0], query);
		        	}
				} catch (IOException e) {
					e.printStackTrace();
				}
	        }
	    }
	}

	public Map<String, QueryTemplate> getTemplateQueries(){
		return queryTemplates;
	}
}
