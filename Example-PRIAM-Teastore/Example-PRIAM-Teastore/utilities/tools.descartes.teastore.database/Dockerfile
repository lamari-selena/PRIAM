FROM mariadb:10.4.12
MAINTAINER Chair of Software Engineering <se2-it@informatik.uni-wuerzburg.de>

# On copie les scripts d'initialisation
COPY setup.sql /docker-entrypoint-initdb.d/setup_0.sql
COPY setupData.sql /docker-entrypoint-initdb.d/setup_1.sql

# Augmenter max_connections
RUN sed -i "s/max_connections.*/max_connections = 2048/g" /etc/mysql/my.cnf

ENV MYSQL_ROOT_PASSWORD=rootpassword

EXPOSE 3306

