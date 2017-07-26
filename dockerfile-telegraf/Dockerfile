## MIT License
##
## Copyright (c) 2016-2017 Logimethods
##
## Permission is hereby granted, free of charge, to any person obtaining a copy
## of this software and associated documentation files (the "Software"), to deal
## in the Software without restriction, including without limitation the rights
## to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
## copies of the Software, and to permit persons to whom the Software is
## furnished to do so, subject to the following conditions:
##
## The above copyright notice and this permission notice shall be included in all
## copies or substantial portions of the Software.
##
## THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
## IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
## FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
## AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
## LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
## OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
## SOFTWARE.

FROM logimethods/ek_telegraf

RUN apt-get install -y default-jre

# http://dba.stackexchange.com/questions/68332/how-can-i-get-nodetool-without-cassandra
COPY tar/ /nodetool/
RUN cd /nodetool && tar -zxf dsc-cassandra-3.0.9-bin.tar.gz && rm dsc-cassandra-3.0.9-bin.tar.gz

# RUN apk --no-cache add docker
COPY entrypoint_prepare.sh /

COPY script/ /etc/telegraf/
RUN chmod +x /etc/telegraf/*.sh
COPY conf/ /etc/telegraf/
