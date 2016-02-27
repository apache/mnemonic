<img src="http://nonvolatilecomputing.github.io/Mnemonic/images/mnemonic_logo.png" width=200 />
A Structured Persistent Memory Library
================================
(This project has been manually tranferred from https://github.com/bigdata-memory/bdmem)

A structured data in-memory persistence & hybrid memory resources management library. It is featured with in-place non-volatile Java object programming model.

### Features:

* In-place data storage on local non-volatile memory
* In-place generic Java object persistence
* Data objects lazy loading
* Any map-able device could be used as a non-volatile memory resource
* Reclaim allocated memory when it is no longer used
* Hierarchical cache pool for massive data caching
* A set of persistent data structures
* Minimize memory footprint on Java heap
* Reduce GC Overhead as following data shown (collected from Apache Spark experiments)

![Mnemonic_GC_stats](http://nonvolatilecomputing.github.io/Mnemonic/images/mnemonic_GC_stats.png)

### Mnemonic Way

![Mnemonic_Way](http://nonvolatilecomputing.github.io/Mnemonic/images/mnemonic_way.png)

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


* **core** -- the submodule project for core
* **collections** -- the submodule project for generic collections
* **examples** -- the submodule project for examples
* **allocator-services/pmalloc-service** -- the submodule project for pmalloc allocator service
* **allocator-services/nvml-vmem-service** -- the submodule project for vmem allocator service
* **allocator-services/service-dist** -- the location of pluggable allocator services

To build this library, you may need to install some required packages on the build system:

* **Maven** -- the building tool v3.2.1 or above [Required]
* **NVML** -- the NVM library (Please compile this library that is tagged with 0.1+b16) (http://pmem.io) [Optional if nvml-vmem-service is excluded]
* **JDK** -- the Java Develop Kit 1.6 or above (please properly configure JAVA_HOME) [Required]
* **PMFS** -- the PMFS should be properly installed and configured on Linux system if you want to simulate read latency [Optional]
* **PMalloc** -- a supported durable memory native library at https://github.com/bigdata-memory/pmalloc.git [Optional if pmalloc-service is excluded]
* **Javapoet** -- a dependant project v1.3.1-SNAPSHOT revised for this project at https://github.com/wewela/javapoet.git [Required]


Once the build system is setup, this Library is built using this command at the top level:
```bash
  $ mvn clean package
```


To exclude a customized allocator service for your platform e.g. OSX, note that if you excluded one or both allocator services, some or all testcases/examples will fail since their allocator is unavailable.
```bash
  $ mvn -pl '!allocator-services/nvml-vmem-service' clean package
```


To build and run the unit tests:
```bash
  $ mvn clean package -DskipTests=false
```


To install this package to local repository (required to run examples and testcases):
```bash
  $ mvn clean install
```


To run an example:
```bash
  $ mvn exec:exec -Pexample -pl examples
```


To run several test cases:
```bash
  $ mvn -Dtest=NonVolatilePersonNGTest test -pl core -DskipTests=false # a testcase for module "core"
  $ mvn -Dtest=BigDataMemAllocatorNGTest test -pl core -DskipTests=false # the second testcase for module "core"
  $ mvn -Dtest=MemClusteringNGTest test -pl core -DskipTests=false # the third testcase for module "core"
  $ mvn -Dtest=NonVolatileNodeValueNGTest  test -pl collections -DskipTests=false # a testcase for module "collection"
  $ mvn -Dtest=NonVolatilePersonNGTest  test -pl collections -DskipTests=false # another testcase for module "collection"
```

### Where is the document ?
 * [API Documentation](http://nonvolatilecomputing.github.io/Mnemonic/apidocs/index.html)
 * [Mnemonic Presentation (.pdf)](https://wiki.apache.org/incubator/MnemonicProposal?action=AttachFile&do=get&target=Project_Mnemonic_Pub1.0.pdf)

### How to apply it for other projects ?
 * [Apache Spark Integration Demo](https://github.com/NonVolatileComputing/spark)
