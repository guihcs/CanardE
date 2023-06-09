/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package irit.sparql.query.exception;

/**
 * @author murloc
 */
public class SparqlQueryMalFormedException extends Exception {
    final String message;

    public SparqlQueryMalFormedException(String message) {
        this.message = message;
    }

    public String toString() {
        return "The query is malformed : " + message;
    }

}
