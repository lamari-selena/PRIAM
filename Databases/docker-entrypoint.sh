#!/bin/bash
# Generate SQL from environment variables
/init.sh

# Now run the original MySQL entrypoint script
exec docker-entrypoint.sh "$@"
