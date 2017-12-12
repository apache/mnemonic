# Mnemonic Examples

The examples demonstrate how to use the [Mnemonic](http://mnemonic.apache.org/).

To run the default example [Main](src/main/java/org/apache/mnemonic/examples/Main.java):
```bash
  $ # requires 'vmem' memory service to run, please refer to the code of test cases for details.
  $ mvn exec:exec -Pexample -pl mnemonic-examples
```


To run a specific example by providing the example name under the [examples](src/main/java/org/apache/mnemonic/examples):
```bash
  $ mvn exec:exec -Pexample -pl mnemonic-examples -Dexample.name=<the example name> [-Dexample.args="<arguments separated by space>"]
```


For how to run the examples in the [Docker](https://www.docker.com), please refer to the [Docker usage](http://mnemonic.apache.org/docs/docker.html) at the documentation of Mnemonic.
