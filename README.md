bdmem: Big Data Memory Library
================================


Please see the file LICENSE for information on how this library is licensed.


This tree contains a library for using Java Big Data Memory.
Here you'll find:

[JavaDoc: https://bigdata-memory.github.io/bigdata-memory](https://bigdata-memory.github.io/bigdata-memory/)

* **src** -- the source for the library
* **src/main/java** -- the Java source for the library
* **examples** -- Brief examples for this library
* **src/main/native** -- the native source for the library
* **src/test/java** -- the Java test & example source for the library
* **uml** -- modeling documents for the library
* **target** -- the generated packages for the library
* **target/apidocs** -- the generated API documents for the library


To build this library, you may need to install some required packages on the build system:


* **NVML** -- the Linux NVM library (Tag: 0.1+b16) (http://pmem.io)
* **JDK** -- the Java Develop Kit 1.6 or above (please properly configure JAVA_HOME)
* **PMFS** -- the PMFS should be properly installed and configured on Linux system if you want to simulate read latency
* **PMalloc** -- the supported durable memory native library at https://github.com/bigdata-memory/pmalloc.git
* **Javapoet** -- the 1.3.1-SNAPSHOT revised for bdmem at https//github.com/wewela/javapoet.git


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

