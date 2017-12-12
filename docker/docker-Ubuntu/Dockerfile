#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM ubuntu:16.04
MAINTAINER Yanhui Zhao (yanhui.zhao@outlook.com)

#set up your proxy below, please refer to readme in the Docker folder
ARG proxy_host=""
ARG proxy_port=""
ENV http_proxy ${proxy_host:+"http://${proxy_host}:${proxy_port}"}
ENV https_proxy ${http_proxy}
ENV HTTP_PROXY ${http_proxy}
ENV HTTPS_PROXY ${http_proxy}

RUN echo The proxy set : ${http_proxy}

RUN apt-get -y update && \
    apt-get install -y default-jdk cmake check git maven pkg-config autoconf man build-essential gcc g++ uuid-dev pandoc devscripts flex doxygen

RUN apt-get clean
    
ENV M2_HOME /usr/share/maven
ENV M2 $M2_HOME/bin
ENV PATH $M2:$PATH
ENV JAVA_HOME /usr/lib/jvm/default-java
ENV PATH $JAVA_HOME/bin:$PATH

RUN mkdir -p /ws
RUN cd /ws && git clone https://github.com/NonVolatileComputing/pmalloc.git && \
    cd pmalloc && mkdir build && cd build && cmake .. && make && make install 
    
RUN cd /ws && git clone https://github.com/pmem/nvml.git && \
    cd nvml && git checkout 630862e82f && make && make install

RUN touch /etc/profile.d/mvn.sh && chmod +x /etc/profile.d/mvn.sh && \
    if [ "x" != "x${proxy_host}" ]; then echo export MAVEN_OPTS="\" -DproxySet=\\\"true\\\" -DproxyHost=${proxy_host} -DproxyPort=${proxy_port} \"" > /etc/profile.d/mvn.sh; fi

RUN cat /etc/profile.d/mvn.sh > ~/.bash_profile
RUN cat /etc/profile.d/mvn.sh > ~/.bashrc

RUN . /etc/profile.d/mvn.sh && cd /ws && git clone https://github.com/apache/mnemonic.git && \
    cd mnemonic && mvn clean package install

ENV MNEMONIC_HOME /ws/mnemonic

#RUN cd /ws/mnemonic && bin/runall.sh -y

WORKDIR /ws

CMD ["bash"]

