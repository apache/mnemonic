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

FROM centos:8
MAINTAINER Gang Wang (garyw@apache.org)

#set up your proxy below, please refer to readme in the Docker folder
ARG proxy_host=""
ARG proxy_port=""
ENV http_proxy ${proxy_host:+"http://${proxy_host}:${proxy_port}"}
ENV https_proxy ${http_proxy}
ENV HTTP_PROXY ${http_proxy}
ENV HTTPS_PROXY ${http_proxy}

RUN echo The proxy set : ${http_proxy}

RUN yum -y install epel-release
RUN yum -y update && yum -y groupinstall 'Development Tools' && \
    yum -y install java-devel cmake check check-devel libuuid-devel man zlib-devel && yum clean all

RUN mkdir -p /ws

RUN curl -O http://mirror.cogentco.com/pub/apache/maven/maven-3/3.5.4/binaries/apache-maven-3.5.4-bin.tar.gz && \
    tar xvf apache-maven-3.5.4-bin.tar.gz && \
    mv apache-maven-3.5.4 /usr/local/apache-maven

ENV M2_HOME /usr/local/apache-maven
ENV M2 $M2_HOME/bin
ENV PATH $M2:$PATH
ENV JAVA_HOME /usr/lib/jvm/java
ENV PATH $JAVA_HOME/bin:$PATH

RUN cd /ws && git clone https://github.com/NonVolatileComputing/pmalloc.git && \
    cd pmalloc && mkdir build && cd build && cmake .. && make && make install

RUN yum -y install ndctl-devel libpmem-devel libvmem-devel libpmemobj-devel

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
