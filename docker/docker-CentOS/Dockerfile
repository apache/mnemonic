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

RUN yum -y install epel-release
RUN yum -y update && yum -y groupinstall 'Development Tools' && \
    yum -y install cmake check check-devel libuuid-devel man zlib-devel maven

RUN yum -y install ndctl-devel libpmem-devel libvmem-devel libpmemobj-devel

RUN yum clean all

# install java
RUN curl -O https://download.java.net/java/GA/jdk14.0.2/205943a0976c4ed48cb16f1043c5c647/12/GPL/openjdk-14.0.2_linux-x64_bin.tar.gz && \
    tar xvf openjdk-14.0.2_linux-x64_bin.tar.gz && \
    mv jdk-14.0.2/ /opt/ && \
    rm openjdk-14.0.2_linux-x64_bin.tar.gz

ENV JAVA_HOME /opt/jdk-14.0.2
ENV PATH $JAVA_HOME/bin:$PATH

WORKDIR /ws

RUN git clone https://github.com/apache/mnemonic.git && \
    cd mnemonic && mvn clean package install

ENV MNEMONIC_HOME /ws/mnemonic

#RUN cd /ws/mnemonic && tools/runall.sh -y

CMD ["bash"]
