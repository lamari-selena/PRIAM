#!/bin/bash

# Define the list of services and their corresponding folders
declare -A services=(
    ["priam/actor-ms"]="PRIAM-Services/PRIAM-Actor-service"
    ["priam/consent-ms"]="PRIAM-Services/PRIAM-Consent-Service"
    ["priam/data-ms"]="PRIAM-Services/PRIAM-Data-service"
    ["priam/eureka"]="PRIAM-Services/PRIAM-Eureka"
    ["priam/right-ms"]="PRIAM-Services/PRIAM-Right-service"
    ["priam/provider-ms"]="PRIAM-Services/Provider-microservice"
    ["priam/api-gateway"]="PRIAM-Services/PRIAM-Gateway"
    ["priam/databases"]="Databases"
)

# Loop through each service and build the Docker image
for image in "${!services[@]}"; do
    folder="${services[$image]}"
    echo "Building Docker image: $image from folder: $folder"
    cd "$folder" || { echo "Failed to enter directory $folder"; exit 1; }
    docker build -t "$image" .
    cd - > /dev/null
done

echo "All Docker images have been built successfully."

