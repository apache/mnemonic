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

FROM centos:7.4.1708
MAINTAINER Gang Wang (garyw@apache.org)

#set up your proxy below, please refer to readme in the Docker folder
ARG proxy_host=""
ARG proxy_port=""
ENV http_proxy ${proxy_host:+"http://${proxy_host}:${proxy_port}"}
ENV https_proxy ${http_proxy}
ENV HTTP_PROXY ${http_proxy}
ENV HTTPS_PROXY ${http_proxy}

RUN echo The proxy set : ${http_proxy}

RUN curl -sSL https://s3.amazonaws.com/download.fpcomplete.com/centos/7/fpco.repo | tee /etc/yum.repos.d/fpco.repo

RUN yum -y update && yum -y groupinstall 'Development Tools' && \
    yum -y install java-devel cmake check check-devel libuuid-devel man zlib-devel wget stack && yum clean all

# required packages by pmdk
RUN yum install -y which autoconf asciidoc xmlto automake libtool kmod-devel libudev-devel uuid-devel json-c-devel

RUN curl -O http://mirror.cogentco.com/pub/apache/maven/maven-3/3.5.4/binaries/apache-maven-3.5.4-bin.tar.gz && \
    tar xvf apache-maven-3.5.4-bin.tar.gz && \
    mv apache-maven-3.5.4 /usr/local/apache-maven

ENV M2_HOME /usr/local/apache-maven
ENV M2 $M2_HOME/bin
ENV PATH $M2:$PATH
ENV JAVA_HOME /usr/lib/jvm/java
ENV PATH $JAVA_HOME/bin:$PATH

RUN mkdir -p /ws && cd /ws && wget https://hackage.haskell.org/package/pandoc-1.17.0.3/pandoc-1.17.0.3.tar.gz && \
    tar xvzf pandoc-1.17.0.3.tar.gz && \
    mv pandoc-1.17.0.3 pandoc && \
    rm pandoc-1.17.0.3.tar.gz

RUN cd /ws/pandoc && stack setup && stack install
ENV PD_HOME /ws/pandoc/.stack-work/install/x86_64-linux/lts-5.8/7.10.3/bin
ENV PATH $PD_HOME:$PATH

RUN cd /ws && git clone https://github.com/NonVolatileComputing/pmalloc.git && \
    cd pmalloc && mkdir build && cd build && cmake .. && make && make install

RUN cd /ws && git clone https://github.com/pmem/nvml.git && \
    cd nvml && git checkout 630862e82f && make && make install

# deploy ndctl required by pmdk
RUN cd /ws && git clone https://github.com/pmem/ndctl.git && \
    cd ndctl && git checkout ndctl-60.y && \
    ./autogen.sh && ./configure CFLAGS='-g -O0' --prefix=/usr --sysconfdir=/etc --libdir=/usr/lib && \
    make && make check && make install

# deploy pmdk
RUN cd /ws && git clone https://github.com/pmem/pmdk.git && \
    cd pmdk && git checkout stable-1.4 && make && \
#    the test run time is too long
#    cp src/test/testconfig.sh.example src/test/testconfig.sh && make check && \
    make install

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

