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

FROM fedora:34
LABEL maintainer="Xiaojin Jiao (xjiao@apache.org)"

# update container
RUN dnf -y update && dnf clean all
# install required package
RUN dnf -y install cmake check git \
                   maven man \
                   gcc g++ \
                   java-latest-openjdk-devel.x86_64

RUN dnf -y install ndctl-devel libpmem-devel libvmem-devel libpmemobj-devel libmemkind-devel &&\
    dnf clean all

ENV JAVA_HOME /usr/lib/jvm/java-16-openjdk
ENV PATH $JAVA_HOME/bin:$PATH

WORKDIR /ws

RUN git clone https://github.com/redis/hiredis.git && \
	cd hiredis && make && make install

RUN cd /ws

RUN git clone https://github.com/apache/mnemonic.git && \
    cd mnemonic && mvn clean package install

ENV MNEMONIC_HOME /ws/mnemonic

CMD ["bash"]
