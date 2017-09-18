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
 * 
 * Gestionnaire de la durée de vie des objets dans le code.
 * Permet à n'importe quelle classe implémentant l'interface "Service"
 * d'appeller d'autres instances de services via son constructeur.
 * Une classe implémentant Service n'est instanciée que par la classe
 * "Container"
 * 
 * @author pf
 */
public class Injector
{
	// liste des services déjà instanciés
	private HashMap<String, Object> instanciedServices = new HashMap<String, Object>();

	private HashMap<Class<?>, Set<String>> grapheDep = new HashMap<Class<?>, Set<String>>();
	
	/**
	 * Sauvegarde le graphe de dépendances.
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
	 * Créé un object de la classe demandée, ou le récupère s'il a déjà été créé
	 * S'occupe automatiquement des dépendances
	 * Toutes les classes demandées doivent implémenter Service ; c'est juste
	 * une sécurité.
	 * 
	 * @param classe
	 * @return un objet de cette classe
	 * @throws InjectorException
	 * @throws InterruptedException
	 */
	public synchronized <S> S getService(Class<S> serviceTo) throws InjectorException
	{
		return getServiceRecursif(serviceTo, new Stack<String>());
	}

	@SuppressWarnings("unchecked")
	public synchronized <S> S getExistingService(Class<S> classe)
	{
		if(instanciedServices.containsKey(classe.getSimpleName()))
			return (S) instanciedServices.get(classe.getSimpleName());
		return null;
	}
	
	public synchronized <S> void addService(Class<S> classe, S object)
	{
		instanciedServices.put(classe.getSimpleName(), object);
	}
	
	public synchronized <S> void removeService(Class<S> classe)
	{
		instanciedServices.remove(classe.getSimpleName());
	}

	/**
	 * Méthode récursive qui fait tout le boulot
	 * 
	 * @param classe
	 * @return
	 * @throws InjectorException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("unchecked")
	private synchronized <S> S getServiceRecursif(Class<S> classe, Stack<String> stack, Object... extraParam) throws InjectorException
	{
		try
		{
			/**
			 * Si l'objet existe déjà et que c'est un Service, on le renvoie
			 */
			if(instanciedServices.containsKey(classe.getSimpleName()))
				return (S) instanciedServices.get(classe.getSimpleName());

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
			Constructor<S> constructeur;

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
				constructeur = (Constructor<S>) classe.getConstructors()[0];

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
			S s = constructeur.newInstance(paramObject);

			instanciedServices.put(classe.getSimpleName(), s);

			// Mise à jour de la pile
			stack.pop();

			return s;
		}
		catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException | InstantiationException e)
		{
			throw new InjectorException("Impossible instanciation of " + classe.getSimpleName()+" (trace : "+printStack(stack)+")", e);
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
