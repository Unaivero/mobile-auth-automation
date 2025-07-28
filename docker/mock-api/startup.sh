#!/bin/bash

# Wait for database to be ready
echo "Waiting for database connection..."
until pg_isready -h $DB_HOST -p 5432 -U $DB_USER; do
    echo "Database is unavailable - sleeping"
    sleep 2
done
echo "Database is ready!"

# Wait for Redis to be ready
echo "Waiting for Redis connection..."
until redis-cli -h $REDIS_HOST -p $REDIS_PORT ping; do
    echo "Redis is unavailable - sleeping"
    sleep 2
done
echo "Redis is ready!"

# Start the Flask application with Gunicorn
echo "Starting Mock API Server..."
exec gunicorn --bind 0.0.0.0:8081 --workers 4 --timeout 120 --access-logfile - --error-logfile - app:app