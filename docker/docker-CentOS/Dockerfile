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
    yum -y install java-devel cmake check check-devel libuuid-devel man zlib-devel maven

RUN yum -y install ndctl-devel libpmem-devel libvmem-devel libpmemobj-devel

RUN yum clean all

ENV JAVA_HOME /usr/lib/jvm/java
ENV PATH $JAVA_HOME/bin:$PATH

WORKDIR /ws

RUN git clone https://github.com/apache/mnemonic.git && \
    cd mnemonic && mvn clean package install

ENV MNEMONIC_HOME /ws/mnemonic

#RUN cd /ws/mnemonic && tools/runall.sh -y

CMD ["bash"]
