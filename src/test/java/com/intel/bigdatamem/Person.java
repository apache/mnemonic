package com.intel.bigdatamem;

import java.util.List;

/**
 *
 *
 */

@PersistentEntity
public abstract class Person<E> implements Durable, Comparable<Person<E>> {
	E element;
	
	@Override
	public void initializeAfterCreate() {
		System.out.println("Initializing After Created");
	}

	@Override
	public void initializeAfterRestore() {
		System.out.println("Initializing After Restored");
	}
	
	@Override
	public void setupGenericInfo(EntityFactoryProxy[] efproxies, GenericField.GType[] gftypes) {
		
	}
	
	public void testOutput() throws RetrievePersistentEntityError {
		System.out.printf("Person %s, Age: %d ( %s ) \n", getName(), getAge(), 
				null == getMother()? "No Recorded Mother" : "Has Recorded Mother");
	}
	
	public int compareTo(Person<E> anotherPerson) {
		int ret = 0;
		if (0 == ret) ret = getAge().compareTo(anotherPerson.getAge());
		if (0 == ret) ret = getName().compareTo(anotherPerson.getName());
		return ret;
	}

	@PersistentGetter
	abstract public Short getAge();
	@PersistentSetter
	abstract public void setAge(Short age);
	
	@PersistentGetter
	abstract public String getName() throws RetrievePersistentEntityError;
	@PersistentSetter
	abstract public void setName(String name, boolean destroy) throws OutOfPersistentMemory, RetrievePersistentEntityError;
	
	@PersistentGetter
	abstract public Person<E> getMother() throws RetrievePersistentEntityError;
	@PersistentSetter
	abstract public void setMother(Person<E> mother, boolean destroy) throws RetrievePersistentEntityError;
	
	@PersistentGetter
	abstract public Person<E> getFather() throws RetrievePersistentEntityError;
	@PersistentSetter
	abstract public void setFather(Person<E> mother, boolean destroy) throws RetrievePersistentEntityError;
}

