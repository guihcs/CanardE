package irit.output;

import irit.output.Output;

import java.util.ArrayList;
import java.util.Map;

public class QueryOutput extends Output {
	
	private final String outputFolder;
	private final Map<String,String> CQANames;

	public QueryOutput(String source, String target, String outputQueryFolder, Map<String, String> cqaNames) {
		super(source, target);
		outputFolder = outputQueryFolder;
		CQANames = cqaNames;
	}

	public String toSubgraphForm(String queryContent, ArrayList<String> selectFocus){

		String ret = queryContent;
		if (selectFocus.size()>1){
			int i=0;
			for(String sf: selectFocus){
				ret=ret.replaceAll( "\\?answer"+i+" ",sf+" ");
				ret=ret.replaceAll( "\\?answer"+i+"\\.",sf+".");
				ret=ret.replaceAll( "\\?answer"+i+"}",sf+"}");
				i++;
			}
		}
		else{
			ret=ret.replaceAll( "\\?answer ",selectFocus.get(0)+" ");
			ret=ret.replaceAll( "\\?answer\\.",selectFocus.get(0)+".");
			ret=ret.replaceAll( "\\?answer}",selectFocus.get(0)+"}");
		}
		return ret;
	}

}
