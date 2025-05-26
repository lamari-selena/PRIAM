# Use the official MySQL image as a base
FROM mysql:9-oracle

# Set the environment variables (used at runtime by the MySQL image)
ENV MYSQL_ROOT_PASSWORD=MaiRP_pWd-ToOr
ENV MYSQL_DATABASE=priam-actor
ENV MYSQL_USER=priamu
ENV MYSQL_PASSWORD=MaiRP_pWd-UsEr

USER root

# Copy init script (which will generate the SQL script at runtime)
COPY ./init.sh /init.sh
RUN chmod +x /init.sh

# Copy the db creation SQL
COPY db_creation_script.sql /docker-entrypoint-initdb.d/
#COPY teadb.sql /docker-entrypoint-initdb.d/
COPY db_insertion_script.sql /docker-entrypoint-initdb.d/

# Replace the entrypoint to generate the SQL before MySQL starts
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]

# Expose port
EXPOSE 3306

# Default command
CMD ["mysqld"]
