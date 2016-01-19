bdmem: Big Data Memory Library
================================

A durable hybrid memory resources management library. It is featured with in-place durable Java object programming model.

### Features:

* In-place data storage on local non-volatile memory
* In-place generic Java object persistence
* Lazily data object loading
* Any map-able device could be used as a non-volatile memory resource
* Reclaim allocated memory when it is no longer used
* Hierarchical cache pool for massive data caching
* A set of persistent data structures is provided by bdmemgeneric project
* Minimize memory footprint on Java heap
* Reduce GC Overhead as following data shown (collected from Apache Spark experiments)

![BDML_GC_Footprint](http://bigdata-memory.github.io/images/BDML_GC_impact.png)

### How to use it ?

#### Define a durable class:

```java
/**
 * a durable class should be abstract and implemented from Durable interface with @PersistentEntity annotation
 */
@PersistentEntity
public abstract class Person<E> implements Durable, Comparable<Person<E>> {
        E element; // Generic Type

        /**
         * callback for brand new durable object creation
         */
        @Override
        public void initializeAfterCreate() { 
                System.out.println("Initializing After Created");
        }
        
        /**
         * callback for durable object recovery
         */
        @Override
        public void initializeAfterRestore() { 
                System.out.println("Initializing After Restored");
        }

        /**
         * setup generic info manually to avoid performance penalty
         */
        @Override
        public void setupGenericInfo(EntityFactoryProxy[] efproxies, GenericField.GType[] gftypes) {

        }

        @Test
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

        /**
         * Getters and Setters for persistent fields with @persistentGetter and @PersistentSetter
         */
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

```

#### Use a durable class:

##### Setup an allocator for durable objects.
```java
        // create an allocator object with parameters ie. capacity and uri
        BigDataPMemAllocator act = new BigDataPMemAllocator(1024 * 1024 * 8, "./pobj_person.dat", true);
        // fetch underlying capacity of key-value pair store for persistent handler storage
        KEYCAPACITY = act.persistKeyCapacity();
        ....
        // close it after use
        act.close();
```

##### Generate structured durable objects.
```java
        // create a new durable person object from specific allocator
        person = PersonFactory.create(act);
        
        // set attributes
        person.setAge((short)rand.nextInt(50));
        person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);

        // keep this person on persistent key-value pair store
        act.setPersistKey(keyidx, person.getPersistentHandler());

        for (int deep = 0; deep < rand.nextInt(100); ++deep) {
                // create another person as mother
                mother = PersonFactory.create(act);
                mother.setAge((short)(50 + rand.nextInt(50)));
                mother.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);
                
                // set the person's mother
                person.setMother(mother, true);

                person = mother;
        }

```
##### Use the durable objects
```java
        for (long i = 0; i < KEYCAPACITY; ++i) {
                System.out.printf("----------Key %d--------------\n", i);
                // iterate persistent handlers from key-value store of specific allocator
                val = act.getPersistKey(i);
                if (0L == val) {
                        break;
                }
                // restore person objects from specific allocator
                Person<Integer> person = PersonFactory.restore(act, val, true);
                while (null != person) {
                        person.testOutput();
                        // iterate all mother's ancestors
                        person = person.getMother();
                }
        }

```

### How to build it ?

Please see the file LICENSE for information on how this library is licensed.


* **src** -- the source for the library
* **src/main/java** -- the Java source for the library
* **examples** -- Brief examples for this library
* **src/main/native** -- the native source for the library
* **src/test/java** -- the Java test & example source for the library
* **uml** -- modeling documents for the library
* **target** -- the generated packages for the library
* **target/apidocs** -- the generated API documents for the library


To build this library, you may need to install some required packages on the build system:


* **NVML** -- the Linux NVM library (Tag: 0.1+b16) (http://pmem.io) [Required]
* **JDK** -- the Java Develop Kit 1.6 or above (please properly configure JAVA_HOME) [Required]
* **PMFS** -- the PMFS should be properly installed and configured on Linux system if you want to simulate read latency [Optional]
* **PMalloc** -- the supported durable memory native library at https://github.com/bigdata-memory/pmalloc.git [Required]
* **Javapoet** -- the 1.3.1-SNAPSHOT revised for bdmem at https://github.com/wewela/javapoet.git [Required]


Once the build system is setup, the Big Memory Library is built using this command at the top level:
```bash
	$ mvn clean package -DskipTests -Dmaven.javadoc.skip=true -Dmaven.test.skip=true
```


To build and run the unit tests:
```bash
	$ mvn clean package
```


To install this package to local repository:
```bash
  $ mvn clean install
```


To build examples:  
(Note that the Big Data Memory Library should be installed to local repository at first):
```bash
  $ cd examples
  $ mvn clean package
```


To run an example:
```bash
  $ cd examples
  $ java -jar target/examples-X.X.X(-SSSSS).jar
```

