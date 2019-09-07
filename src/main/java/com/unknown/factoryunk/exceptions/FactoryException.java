package com.unknown.factoryunk.exceptions;

public class FactoryException extends RuntimeException {

    /**
     * General exception for factory problems
     * @param message to show
     */
    public FactoryException(String message) {
        super(message);
    }
}
