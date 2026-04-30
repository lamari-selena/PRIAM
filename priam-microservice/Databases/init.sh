#!/bin/bash

# Read environment variables
MYSQL_USER=${MYSQL_USER:-'myuser'}
MYSQL_PASSWORD=${MYSQL_PASSWORD:-'mypassword'}

# Generate the SQL script
cat <<EOF > /tmp/1_init.sql
-- Create a user with permissions to create databases
-- CREATE USER '$MYSQL_USER'@'%' IDENTIFIED BY '$MYSQL_PASSWORD';
GRANT ALL PRIVILEGES ON *.* TO '$MYSQL_USER'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
EOF

# Copy the SQL script to the correct location
cp /tmp/1_init.sql /docker-entrypoint-initdb.d/1_init.sql

chmod +x /docker-entrypoint-initdb.d/1_init.sql
