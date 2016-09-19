<img src="http://nonvolatilecomputing.github.io/Mnemonic/images/mnemonic_logo.png" width=200 />

================================

Apache Mnemonic is an advanced hybrid memory storages oriented library, it proposed a non-volatile/durable Java object model and durable computing service that bring several advantages to significantly improve the performance of massive real-time data processing/analytics. developers are able to use this library to design their cache-less and SerDe-less high performance applications.

### Features:

* In-place data storage on local non-volatile memory
* Durable Object Model
* Object graphs lazy loading & sharing
* Auto-reclaim memory resources and Mnemonic objects
* Hierarchical cache pool for massive data caching
* Extensible memory services for new device adoption and allocation optimization
* Durable data structure collection(WIP)
* Durable computing service
* Minimize memory footprint on Java heap
* Reduce GC Overheads as the following chart shown (collected from Apache Spark experiments)

![Mnemonic_GC_stats](http://nonvolatilecomputing.github.io/Mnemonic/images/mnemonic_GC_stats.png)

### Mnemonic Way

![Mnemonic_Way](http://nonvolatilecomputing.github.io/Mnemonic/images/mnemonic_way.png)


![Mnemonic_Modes](http://nonvolatilecomputing.github.io/Mnemonic/images/mnemonic_modes.png)


### How to use it ?

#### Define a Non-Volatile class:

```java
/**
 * a durable class should be abstract, implement Durable interface and marked with @DurableEntity annotation
 */
@DurableEntity
public abstract class Person<E> implements Durable, Comparable<Person<E>> {
        E element; // Generic Type

        /**
         * callback for this durable object creation
         */
        @Override
        public void initializeAfterCreate() { 
                System.out.println("Initializing After Created");
        }
        
        /**
         * callback for this durable object recovery
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
        public void testOutput() throws RetrieveDurableEntityError {
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
         * Getters and Setters for non-volatile fields marked with @DurableGetter and @DurableSetter
         */
        @DurableGetter(Id = 1L)
        abstract public Short getAge();
        @DurableSetter
        abstract public void setAge(Short age);

        @DurableGetter(Id = 2L)
        abstract public String getName() throws RetrieveDurableEntityError;
        @DurableSetter
        abstract public void setName(String name, boolean destroy) throws OutOfPersistentMemory, RetrieveDurableEntityError;

        @DurableGetter(Id = 3L)
        abstract public Person<E> getMother() throws RetrieveDurableEntityError;
        @DurableSetter
        abstract public void setMother(Person<E> mother, boolean destroy) throws RetrieveDurableEntityError;

        @DurableGetter(Id = 4L)
        abstract public Person<E> getFather() throws RetrieveDurableEntityError;
        @DurableSetter
        abstract public void setFather(Person<E> mother, boolean destroy) throws RetrieveDurableEntityError;
}

```

#### Use a non-volatile class:

##### Setup an allocator for non-volatile object graphs.
```java
        // create an allocator instance
        NonVolatileMemAllocator act = new NonVolatileMemAllocator(1024 * 1024 * 8, "./pobj_person.dat", true);
        
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
        act.setHandler(keyidx, person.getHandler());

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
##### Perform the durable native computing (e.g. printing) w/o packing/unpacking massive object graphs
```java
         // fetch print service
         GeneralComputingService gcsvr = Utils.getGeneralComputingService("print");
         // instantiate a value info for a value matrix
         ValueInfo vinfo = new ValueInfo();
         // instantiate a object stack
         List<long[][]> objstack = new ArrayList<long[][]>();
         // fill up with all durable object info in order
         objstack.add(firstnv.getNativeFieldInfo());
         objstack.add(person.getNativeFieldInfo());
         // configure the Id stack for each level of durable objects
         long[][] fidinfostack = {{2L, 1L}, {0L, 1L}};
         // configure the handler of a value matrix
         vinfo.handler = handler;
         // set translate table from handler's allocator
         vinfo.transtable = m_act.getTranslateTable();
         // specify the durable type of value
         vinfo.dtype = DurableType.SHORT;
         // generate frames for this value matri from both stacks
         vinfo.frames = Utils.genNativeParamForm(objstack, fidinfostack);
         // form an array of value infos
         ValueInfo[] vinfos = {vinfo};
         // perform the print operation
         gcsvr.perform(vinfos);
```

### How to build it ?

Please see the file LICENSE for information on how this library is licensed.


* **mnemonic-core** -- the submodule project for core
* **mnemonic-collections** -- the submodule project for generic collections
* **mnemonic-examples** -- the submodule project for examples, Please refer to the testcases of respective module as complete examples.
* **mnemonic-memory-services/mnemonic-pmalloc-service** -- the submodule project for pmalloc memory service
* **mnemonic-memory-services/mnemonic-nvml-vmem-service** -- the submodule project for vmem memory service
* **mnemonic-memory-services/service-dist** -- the location of extensive memory services (auto-generated)
* **mnemonic-computing-services/mnemonic-utilities-service** -- the submodule project for utilities computing service
* **mnemonic-computing-services/service-dist** -- the location of extensive computing services (auto-generated)

To build this library, you may need to install some required packages on the build system:

* **Maven** -- the building tool v3.2.1 or above [Required]
* **NVML** -- the NVM library (Please compile this library that is tagged with 1.1 release with pandoc dependency) (http://pmem.io) [Optional if mnemonic-nvml-vmem-service is excluded, e.g. MacOSX]
* **JDK** -- the Java Develop Kit 1.6 or above (please properly configure JAVA_HOME) [Required]
* **PMFS** -- the PMFS should be properly installed and configured on Linux system if you want to simulate read latency [Optional]
* **PMalloc** -- a supported durable memory native library(Latest) at https://github.com/NonVolatileComputing/pmalloc.git [Optional if mnemonic-pmalloc-service is excluded]


Once the build system is setup, this Library is built using this command at the top level:
```bash
  $ git clean -xdf # if pull from a git repo.
  $ mvn clean package install
```


To exclude a customized memory service for your platform e.g. OSX, note that if you excluded one or both memory services, some or all testcases/examples will fail since their dependent memory services are unavailable.
```bash
  $ git clean -xdf # if pull from a git repo.
  $ mvn -pl '!mnemonic-memory-services/mnemonic-nvml-vmem-service' clean package install
```


To install this package to local repository (required to run examples and testcases):
```bash
  $ mvn clean install
```


To run an example:
```bash
  $ # requires 'vmem' memory service to run, please refer to the code of test cases for more examples.
  $ mvn exec:exec -Pexample -pl mnemonic-examples
```


To run several test cases:
```bash
  $ # a testcase for module "mnemonic-core" that requires 'pmalloc' memory service to pass
  $ mvn -Dtest=DurablePersonNGTest test -pl mnemonic-core -DskipTests=false
  
  $ # a testcase for module "mnemonic-core" that requires 'pmalloc' memory service to pass
  $ mvn -Dtest=NonVolatileMemAllocatorNGTest test -pl mnemonic-core -DskipTests=false
  
  $ # a testcase for module "mnemonic-core" that requires 'vmem' memory service to pass
  $ mvn -Dtest=VolatileMemAllocatorNGTest test -pl mnemonic-core -DskipTests=false
  
  $ # a testcase for module "mnemonic-core" that requires 'vmem memory service to pass
  $ mvn -Dtest=MemClusteringNGTest test -pl mnemonic-core -DskipTests=false
  
  $ # a testcase for module "mnemonic-collection" that requires 'pmalloc' memory service to pass
  $ mvn -Dtest=DurableSinglyLinkedListNGTest  test -pl mnemonic-collections -DskipTests=false
  
  $ # a testcase for module "mnemonic-collection" that requires 'pmalloc' memory service to pass
  $ mvn -Dtest=DurablePersonNGTest  test -pl mnemonic-collections -DskipTests=false
  
  $ # a testcase for module "mnemonic-computing-services/mnemonic-utilities-service" that requires 'pmalloc' memory service to pass
  $ mvn -Dtest=DurableSinglyLinkedListNGPrintTest test -pl mnemonic-computing-services/mnemonic-utilities-service -DskipTests=false
```

### Where is the document ?
 * Source code comments
 * [API Documentation](http://nonvolatilecomputing.github.io/Mnemonic/apidocs/index.html)
 * [Mnemonic Presentation (.pdf)](https://wiki.apache.org/incubator/MnemonicProposal?action=AttachFile&do=get&target=Project_Mnemonic_Pub1.0.pdf)

### How to apply it for other projects ?
 * [Apache Spark Integration Demo](https://github.com/NonVolatileComputing/spark)
 * [Apache Arrow Integration Demo](https://github.com/NonVolatileComputing/arrow)
