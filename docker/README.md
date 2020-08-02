<img src="https://mnemonic.apache.org/img/mnemonic_logo.png" width=200 />

================================

This is the "Dockerfile" that will automatically build the CentOS/Ubuntu environment of this project.

--------------
### Features:

*What does the Dockerfile do?*

- 1. Build from centos/ubuntu.
- 2. Install dependency packages.
- 3. Set up environment variables of paths.
- 4. Create /ws folder.
- 5. Install pmalloc in /ws
- 6. Clone mnemonic code then build/install.
- 7. Go to /ws fold and start bash.

### How to build the docker image from Dockerfile in host OS?
Build from git repository

```bash
  $ docker build -t NAME[:TAG] https://github.com/apache/mnemonic.git#:docker/docker-CentOS
  $ docker build -t NAME[:TAG] https://github.com/apache/mnemonic.git#:docker/docker-Ubuntu
```

-- OR --

In the folder of Dockerfile, run:

```bash
  $ docker build -t NAME[:TAG] .
```

* More detials please refer to [Docker build reference](https://docs.docker.com/engine/reference/commandline/build/)

#### Optional: After build, push image to dockerhub:

```bash
  $ docker login [OPTIONS] [SERVER]
  $ docker push [OPTIONS] NAME[:TAG]
```

* More detials please refer to [Docker login reference](https://docs.docker.com/engine/reference/commandline/login/)
 and [Docker push reference](https://docs.docker.com/engine/reference/commandline/push/)

### How to run image after build

Run image:

```bash
  $ docker run --name CONTAINER_NAME -it NAME[:TAG]
```

### Sharing host project folder to Dock container for IDEs e.g. Eclipse, Intellij IDEA

```bash
  $ docker run -v <hostdir>/mnemonic:/ws/mnemonic -it NAME[:TAG]
```
Note: this command will override the container's project folder, you can use another name to avoid it.

 * More details please refer to [Docker run reference](https://docs.docker.com/engine/reference/run/)
