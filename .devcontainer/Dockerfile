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

FROM ubuntu:22.04
LABEL maintainer="Yanhui Zhao (yanhui.zhao@outlook.com)"

RUN apt-get update && apt-get install -y software-properties-common && add-apt-repository ppa:openjdk-r/ppa && apt-get -y update && \
    apt-get install -y openjdk-17-jdk cmake check git pkg-config autoconf man build-essential gcc g++ uuid-dev pandoc devscripts flex doxygen maven tree

RUN apt-get install -y libndctl-dev libpmem-dev libpmemobj-dev libmemkind-dev

RUN apt-get clean

WORKDIR /ws

RUN git clone https://github.com/pmem/vmem && \
    cd vmem && make && make install

RUN cd /ws

RUN git clone https://github.com/redis/hiredis.git && \
	cd hiredis && make && make install

RUN curl https://sh.rustup.rs -sSf | sh -s -- -y

RUN cd /ws

ENV MNEMONIC_HOME /ws/mnemonic

#RUN cd /ws/mnemonic && tools/runall.sh -y

SHELL ["/bin/bash", "-c"]
RUN fp=$(readlink -e $(which javac)); echo "export JAVA_HOME=${fp/%\/bin\/javac/}" >> ~/.bashrc
RUN echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> ~/.bashrc

CMD ["bash"]
