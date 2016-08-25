<img src="http://nonvolatilecomputing.github.io/Mnemonic/images/mnemonic_logo.png" width=200 />

================================ 

This is the "Dockerfile" that will automatically build the environment of this project. 

--------------
### Features:

*What does this Dockerfile do?* 

- 1. Build from centos
- 2. Set up http/https proxy. If no porxy is needed, modify proxy_host and proxy_port to empty and comment other ENV lines as shown below.
```bash
  ENV proxy_host ""
  ENV proxy_port ""
  #ENV http_proxy "http://${proxy_host}:${proxy_port}"
  #ENV https_proxy ${http_proxy}
  #ENV HTTP_PROXY ${http_proxy}
  #ENV HTTPS_PROXY ${http_proxy}
  #ENV proxy ${http_proxy}
```
- 3. Install yum default "Development Tools"
- 4. Install maven
- 5. Set up environment variables of paths
- 6. Create /ws folder and install pmalloc
- 7. Install nvml in /ws
- 8. Set up maven proxy mvn.sh
- 9. Clone mnemonic code then build/install  
- 10. Go to /ws fold and start bash.  

### How to build the docker image from Dockerfile in host OS?
 
In the folder of Dockerfile, run: 

```bash
  $ docker build [OPTIONS] PATH | URL | -
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
  $ docker run -t -i IMAGE
```
 * More detials please refer to [Docker run reference](https://docs.docker.com/engine/reference/run/)

