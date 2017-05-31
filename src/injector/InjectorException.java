/*
 * Copyright (C) 2013-2017 Pierre-François Gimenez
 */

package injector;

/**
 * Exception thrown by the dependency injector
 * 
 * @author pf
 *
 */

public class InjectorException extends Exception
{

	private static final long serialVersionUID = -960091158805232282L;

	public InjectorException()
	{
		super();
	}

	public InjectorException(String m)
	{
		super(m);
	}

}
