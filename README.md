<img src="http://nonvolatilecomputing.github.io/Mnemonic/images/mnemonic_logo.png" width=200 />
================================

This library comes up with a new programming model we call it non-volatile object programming model, this model directly offloads object graphs into a variety of memory-like devices e.g. SSD, NVMe, Off-heap, in this way, it brings some promising features for massive data processing and high performance computing.

### Features:

* In-place data storage on local non-volatile memory
* In-place generic Java object persistence
* Object graphs lazy loading & multi-process sharing
* Auto-reclaim memory resources and Mnemonic objects
* Hierarchical cache pool for massive data caching
* Pluggable allocator services for extension & optimization
* A set of non-volatile data structures (WIP)
* Minimize memory footprint on Java heap
* Reduce GC Overheads as the following chart shown (collected from Apache Spark experiments)
* [Coming major feature]: Distributed Object Graphs (DOG)
* [Coming major feature]: Columnar-aware object graphs & collections (Apache Arrow based optimization)

![Mnemonic_GC_stats](http://nonvolatilecomputing.github.io/Mnemonic/images/mnemonic_GC_stats.png)

### Mnemonic Way

![Mnemonic_Way](http://nonvolatilecomputing.github.io/Mnemonic/images/mnemonic_way.png)


![Mnemonic_Modes](http://nonvolatilecomputing.github.io/Mnemonic/images/mnemonic_modes.png)


### How to use it ?

#### Define a Non-Volatile class:

```java
/**
 * a non-volatile class should be abstract, implement Durable interface and marked with @NonVolatileEntity annotation
 */
@NonVolatileEntity
public abstract class Person<E> implements Durable, Comparable<Person<E>> {
        E element; // Generic Type

        /**
         * callback for this non-volatile object creation
         */
        @Override
        public void initializeAfterCreate() { 
                System.out.println("Initializing After Created");
        }
        
        /**
         * callback for this non-valatile object recovery
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
         * Getters and Setters for non-volatile fields marked with @NonVolatileGetter and @NonVolatileSetter
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

##### Setup an allocator for non-volatile object graphs.
```java
        // create an allocator instance
        BigDataPMemAllocator act = new BigDataPMemAllocator(1024 * 1024 * 8, "./pobj_person.dat", true);
        
        // fetch handler store capacity from this non-volatile storage managed by this allocator
        KEYCAPACITY = act.handlerCapacity();
        ....
        // close it after use
        act.close();
```

##### Generate structured non-volatile objects.
```java
        // create a new non-volatile person object from this specific allocator
        person = PersonFactory.create(act);
        
        // set attributes
        person.setAge((short)rand.nextInt(50));
        person.setName(String.format("Name: [%s]", UUID.randomUUID().toString()), true);

        // keep this person on non-volatile handler store
        act.setHandler(keyidx, person.getNonVolatileHandler());

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
                // iterate non-volatile handlers from handler store of this specific allocator
                val = act.getHandler(i);
                if (0L == val) {
                        break;
                }
                
                // restore person objects from this specific allocator
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
* **allocator-services/service-dist** -- the location of pluggable allocator services (auto-generated)

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
  $ mvn exec:exec -Pexample -pl examples # requires 'vmem' allocator service to run, please refer to the code of test cases for more examples.
```


To run several test cases:
```bash
  
  $ mvn -Dtest=NonVolatilePersonNGTest test -pl core -DskipTests=false # a testcase for module "core" that requires 'pmalloc' allocator service to pass
  
  $ mvn -Dtest=BigDataMemAllocatorNGTest test -pl core -DskipTests=false # the second testcase for module "core" that requires 'vmem' allocator service to pass
  
  $ mvn -Dtest=MemClusteringNGTest test -pl core -DskipTests=false # the third testcase for module "core" that requires 'vmem allocator service to pass
  
  $ mvn -Dtest=NonVolatileNodeValueNGTest  test -pl collections -DskipTests=false # a testcase for module "collection" that requires 'pmalloc' allocator service to pass
  
  $ mvn -Dtest=NonVolatilePersonNGTest  test -pl collections -DskipTests=false # another testcase for module "collection" that requires 'pmalloc' allocator service to pass
```

### Where is the document ?
 * [API Documentation](http://nonvolatilecomputing.github.io/Mnemonic/apidocs/index.html)
 * [Mnemonic Presentation (.pdf)](https://wiki.apache.org/incubator/MnemonicProposal?action=AttachFile&do=get&target=Project_Mnemonic_Pub1.0.pdf)

### How to apply it for other projects ?
 * [Apache Spark Integration Demo](https://github.com/NonVolatileComputing/spark)
