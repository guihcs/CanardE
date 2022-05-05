package irit.sparql.exceptions;

import java.io.Serial;

public class IncompleteSubstitutionException extends Exception {

	@Serial
	private static final long serialVersionUID = -3430981554411926194L;
	
	public IncompleteSubstitutionException(String m){
		super(m);
	}
}
