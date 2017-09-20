/*
 * Copyright (C) 2013-2017 Pierre-François Gimenez
 * Distributed under the MIT License.
 */

package pfg.injector;

/**
 * Exception thrown by the dependency injector
 * 
 * @author Pierre-François Gimenez
 *
 */

public class InjectorException extends Exception
{
	private static final long serialVersionUID = -960091158805232282L;
	
	public InjectorException(String m)
	{
		super(m);
	}

	public InjectorException(String m, Throwable cause)
	{
		super(m, cause);
	}
}
