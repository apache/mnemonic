Mnemonic: Structured Persistent Memory Library
================================
(This project has been manually tranferred from https://github.com/bigdata-memory/bdmem)

A structured data in-memory persistence & hybrid memory resources management library. It is featured with in-place non-volatile Java object programming model.

### Features:

* In-place data storage on local non-volatile memory
* In-place generic Java object persistence
* Lazily data object loading
* Any map-able device could be used as a non-volatile memory resource
* Reclaim allocated memory when it is no longer used
* Hierarchical cache pool for massive data caching
* A set of persistent data structures
* Minimize memory footprint on Java heap
* Reduce GC Overhead as following data shown (collected from Apache Spark experiments)

![BDML_GC_Footprint](http://bigdata-memory.github.io/images/BDML_GC_impact.png)

### How to use it ?

#### Define a Non-Volatile class:

```java
/**
 * a durable class should be abstract and implemented from Durable interface with @NonVolatileEntity annotation
 */
@NonVolatileEntity
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
        public void testOutput() throws RetrieveNonVolatileEntityError {
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
         * Getters and Setters for persistent fields with @NonVolatileGetter and @NonVolatileSetter
         */
        @NonVolatileGetter
        abstract public Short getAge();
        @NonVolatileSetter
        abstract public void setAge(Short age);

        @NonVolatileGetter
        abstract public String getName() throws RetrieveNonVolatileEntityError;
        @NonVolatileSetter
        abstract public void setName(String name, boolean destroy) throws OutOfPersistentMemory, RetrieveNonVolatileEntityError;

        @NonVolatileGetter
        abstract public Person<E> getMother() throws RetrieveNonVolatileEntityError;
        @NonVolatileSetter
        abstract public void setMother(Person<E> mother, boolean destroy) throws RetrieveNonVolatileEntityError;

        @NonVolatileGetter
        abstract public Person<E> getFather() throws RetrieveNonVolatileEntityError;
        @NonVolatileSetter
        abstract public void setFather(Person<E> mother, boolean destroy) throws RetrieveNonVolatileEntityError;
}

```

#### Use a non-volatile class:

##### Setup an allocator for non-volatile objects.
```java
        // create an allocator object with parameters ie. capacity and uri
        BigDataPMemAllocator act = new BigDataPMemAllocator(1024 * 1024 * 8, "./pobj_person.dat", true);
        // fetch underlying capacity of key-value pair store for Non Volatile handler storage
        KEYCAPACITY = act.persistKeyCapacity();
        ....
        // close it after use
        act.close();
```

##### Generate structured non-volatile objects.
```java
        // create a new durable person object from specific allocator
        person = PersonFactory.create(act);
        
        // set attributes
        person.setAge((short)rand.nextInt(50));
        person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);

        // keep this person on persistent key-value pair store
        act.setPersistKey(keyidx, person.getNonVolatileHandler());

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
##### Use the non-volatile objects
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


* **NVML** -- the NVM library (Please compile this library that is tagged with 0.1+b16) (http://pmem.io) [Required]
* **JDK** -- the Java Develop Kit 1.6 or above (please properly configure JAVA_HOME) [Required]
* **PMFS** -- the PMFS should be properly installed and configured on Linux system if you want to simulate read latency [Optional]
* **PMalloc** -- the supported durable memory native library at https://github.com/bigdata-memory/pmalloc.git [Required]
* **Javapoet** -- the 1.3.1-SNAPSHOT revised for bdmem at https://github.com/wewela/javapoet.git [Required]


Once the build system is setup, this Library is built using this command at the top level:
```bash
  $ mvn clean package
```


To build and run the unit tests:
```bash
  $ mvn clean package -DskipTests=false
```


To install this package to local repository:
```bash
  $ mvn clean install
```

To run an example:
```bash
  $ cd examples
  $ java -jar target/examples-X.X.X(-SSSSS).jar
```

