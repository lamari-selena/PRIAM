#!/bin/bash

# Define the list of services and their corresponding folders
declare -A services=(
    ["priam/actor-ms"]="PRIAM-Actor-service"
    ["priam/consent-ms"]="PRIAM-Consent-Service"
    ["priam/data-ms"]="PRIAM-Data-service"
    ["priam/eureka"]="PRIAM-Eureka"
    ["priam/right-ms"]="PRIAM-Right-service"
    ["priam/provider-ms"]="Provider-microservice"
    ["priam/api-gateway"]="PRIAM-Gateway"
    ["priam/databases"]="Databases"
)

# Loop through each service and build the Docker image
for image in "${!services[@]}"; do
    folder="${services[$image]}"
    echo "Building Docker image: $image from folder: $folder"
    cd "$folder" || { echo "Failed to enter directory $folder"; exit 1; }
    docker build -t "$image" .
    cd ..
done

echo "All Docker images have been built successfully."

