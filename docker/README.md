<img src="http://nonvolatilecomputing.github.io/Mnemonic/images/mnemonic_logo.png" width=200 />

================================ 

This is the "Dockerfile" that will automatically build the environment of this project. 

--------------
### Features:

*What does this Dockerfile do?* 

- 1. Build from centos.
- 2. Default is not using porxy. Please see instruction below to set up http/https proxy.
- 3. Install yum default "Development Tools".
- 4. Install maven.
- 5. Set up environment variables of paths.
- 6. Create /ws folder and install pmalloc.
- 7. Install nvml in /ws.
- 8. Set up maven proxy mvn.sh.
- 9. Clone mnemonic code then build/install.  
- 10. Go to /ws fold and start bash.  

#### How to set up proxy? 

Input IP address and port as the example below, then uncomment all other ENV lines. 
```bash
ENV proxy_host "172.17.42.1"
ENV proxy_port "8668"
ENV http_proxy "http://${proxy_host}:${proxy_port}"
ENV https_proxy ${http_proxy}
ENV HTTP_PROXY ${http_proxy}
ENV HTTPS_PROXY ${http_proxy}
ENV proxy ${http_proxy}
```

### How to build the docker image from Dockerfile in host OS?
 
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
 * More detials please refer to [Docker run reference](https://docs.docker.com/engine/reference/run/)

