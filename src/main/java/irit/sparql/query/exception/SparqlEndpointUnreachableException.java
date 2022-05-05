package irit.sparql.query.exception;

import java.io.Serial;

public class SparqlEndpointUnreachableException extends Exception
{
	/**
	 * 
	 */
	@Serial
    private static final long serialVersionUID = -6591977685812151888L;
        
        private final String message;
        
        public SparqlEndpointUnreachableException(Exception e)
        {
            message = e.getLocalizedMessage();
        }
        
	public String toString()
	{
		return "The endpoint you specified is unreachable : "+ message;
	}
}
