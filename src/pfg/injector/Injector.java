/*
 * Copyright (C) 2013-2017 Pierre-François Gimenez
 * Distributed under the MIT License.
 */

package pfg.injector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

/**
 * Simple dependency injector.
 * 
 * @author Pierre-François Gimenez
 */
public class Injector
{
	// liste des services déjà instanciés
	private HashMap<Class<?>, Object> instanciedServices = new HashMap<Class<?>, Object>();
	private HashMap<Class<?>, Set<String>> grapheDep = new HashMap<Class<?>, Set<String>>();
	
	/**
	 * Save the dependency graph into a .dot file
	 * @param filename
	 */
	public void saveGraph(String filename)
	{
		try
		{
			FileWriter fw = new FileWriter(new File(filename));
			fw.write("digraph dependences {\n\n");

			for(Class<?> classe : grapheDep.keySet())
				fw.write(classe.getSimpleName() + ";\n");

			fw.write("\n");

			for(Class<?> classe : grapheDep.keySet())
			{
				Set<String> enf = grapheDep.get(classe);
				if(!enf.isEmpty())
				{
					fw.write(classe.getSimpleName() + " -> {");
					for(String e : enf)
						fw.write(e + " ");
					fw.write("};\n");
				}
			}
			fw.write("\n}\n");
			fw.close();
			
			System.out.println("Dependency graph saved");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Create (if necessary) an object of this class
	 * Deals with the dependency injection.
	 * 
	 * @param clazz
	 * @return an instance of this class
	 * @throws InjectorException
	 */
	public synchronized <S> S getService(Class<S> clazz) throws InjectorException
	{
		return getServiceRecursif(clazz, new Stack<String>());
	}

	/**
	 * Return the already built instance.
	 * If this instance doesn't exist, return null
	 * 
	 * @param clazz
	 * @return
	 */
	public synchronized <S> S getExistingService(Class<S> clazz)
	{
		if(instanciedServices.containsKey(clazz))
			return clazz.cast(instanciedServices.get(clazz));
		return null;
	}

	/**
	 * Add an object. It will be associated to its class object.getClass()
	 * @param object
	 */
	public synchronized <S> void addService(S object)
	{
		instanciedServices.put(object.getClass(), object);
	}

	/**
	 * Add an object with the specified class.
	 * Useful if this class isn't object.getClass() (for example if S is a super class of object.getClass())
	 * @param clazz
	 * @param object
	 */
	public synchronized <S> void addService(Class<S> clazz, S object)
	{
		instanciedServices.put(clazz, object);
	}
	
	/**
	 * Remove an instance from its class
	 * @param clazz
	 */
	public synchronized <S> void removeService(Class<S> clazz)
	{
		instanciedServices.remove(clazz);
	}

	/**
	 * Méthode récursive qui fait tout le boulot
	 * 
	 * @param classe
	 * @return
	 * @throws InjectorException
	 * @throws InterruptedException
	 */
	private synchronized <S> S getServiceRecursif(Class<S> classe, Stack<String> stack, Object... extraParam) throws InjectorException
	{
		try
		{
			/**
			 * Si l'objet existe déjà et que c'est un Service, on le renvoie
			 */
			if(instanciedServices.containsKey(classe))
				return classe.cast(instanciedServices.get(classe));

			/**
			 * Détection de dépendances circulaires
			 */
			if(stack.contains(classe.getSimpleName()))
			{
				// Dépendance circulaire détectée !
				String out = "A circular dependency has been detected : ";
				out += printStack(stack);
				throw new InjectorException(out);
			}

			// Pas de dépendance circulaire

			// On met à jour la pile
			stack.push(classe.getSimpleName());

			/**
			 * Récupération du constructeur et de ses paramètres
			 * On suppose qu'il n'y a chaque fois qu'un seul constructeur pour
			 * cette classe
			 */
			Constructor<?> constructeur;

			if(classe.getConstructors().length > 1)
			{
				try
				{
					// Plus d'un constructeur ? On prend celui par défaut
					constructeur = classe.getConstructor();
				}
				catch(Exception e)
				{
					throw new InjectorException(classe.getSimpleName() + " has several constructors and no default constructor !", e);
				}
			}
			else if(classe.getConstructors().length == 0)
			{
				String out = printStack(stack);
				throw new InjectorException(classe.getSimpleName() + " has no public constructor ! " + out);
			}
			else
				constructeur = classe.getConstructors()[0];

			Class<?>[] param = constructeur.getParameterTypes();

			/*
			 * Récupération du graphe de dépendances
			 */
			Set<String> enf = grapheDep.get(classe);
			if(enf == null)
			{
				enf = new HashSet<String>();
				grapheDep.put(classe, enf);
			}
			for(int i = 0; i < param.length - extraParam.length; i++)
			{
				String fils = param[i].getSimpleName();
				enf.add(fils);
			}

			/**
			 * On demande récursivement chacun de ses paramètres
			 * On complète automatiquement avec ceux déjà donnés
			 */
			Object[] paramObject = new Object[param.length];
			for(int i = 0; i < param.length - extraParam.length; i++)
				paramObject[i] = getServiceRecursif(param[i], stack);
			for(int i = 0; i < extraParam.length; i++)
				paramObject[i + param.length - extraParam.length] = extraParam[i];

			/**
			 * Instanciation et sauvegarde
			 */
			S s = classe.cast(constructeur.newInstance(paramObject));

			instanciedServices.put(classe, s);

			// Mise à jour de la pile
			stack.pop();

			return s;
		}
		catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException | InstantiationException e)
		{
			throw new InjectorException("Impossible instanciation of " + classe.getSimpleName()+" ("+printStack(stack)+")", e);
		}
	}

	private String printStack(Stack<String> stack)
	{
		String out = "Dependency stack : ";
		Iterator<String> iter = stack.iterator();
		while(iter.hasNext())
		{
			out += iter.next();
			if(iter.hasNext())
				out += " -> ";
		}
		return out;
	}
}
