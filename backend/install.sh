#!/bin/bash

# DiffGrader Backend Installation Script
# This script sets up the DiffGrader backend environment

set -e

echo "🚀 DiffGrader Backend Installation Script"
echo "=========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check Java version
check_java() {
    print_status "Checking Java installation..."
    
    if command_exists java; then
        JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1-2)
        JAVA_MAJOR=$(echo $JAVA_VERSION | cut -d'.' -f1)
        
        if [ "$JAVA_MAJOR" -ge 11 ]; then
            print_success "Java $JAVA_VERSION found"
        else
            print_error "Java 11 or higher is required. Found: $JAVA_VERSION"
            echo "Please install Java 11+ from: https://adoptium.net/"
            exit 1
        fi
    else
        print_error "Java not found!"
        echo "Please install Java 11+ from: https://adoptium.net/"
        exit 1
    fi
}

# Check Maven
check_maven() {
    print_status "Checking Maven installation..."
    
    if command_exists mvn; then
        MVN_VERSION=$(mvn -version | head -n1 | cut -d' ' -f3)
        print_success "Maven $MVN_VERSION found"
    else
        print_warning "Maven not found. Using Maven wrapper..."
        if [ ! -f "./mvnw" ]; then
            print_error "Maven wrapper not found!"
            echo "Please install Maven from: https://maven.apache.org/"
            exit 1
        fi
    fi
}

# Check PostgreSQL
check_postgresql() {
    print_status "Checking PostgreSQL installation..."
    
    if command_exists psql; then
        PG_VERSION=$(psql --version | cut -d' ' -f3)
        print_success "PostgreSQL $PG_VERSION found"
        
        # Check if PostgreSQL is running
        if pg_isready >/dev/null 2>&1; then
            print_success "PostgreSQL is running"
        else
            print_warning "PostgreSQL is not running"
            echo "Please start PostgreSQL service:"
            echo "  - macOS: brew services start postgresql"
            echo "  - Ubuntu: sudo systemctl start postgresql"
            echo "  - Windows: Start PostgreSQL service from Services"
        fi
    else
        print_error "PostgreSQL not found!"
        echo "Please install PostgreSQL 13+ from: https://www.postgresql.org/download/"
        exit 1
    fi
}

# Setup database
setup_database() {
    print_status "Setting up database..."
    
    read -p "Enter PostgreSQL username (default: postgres): " DB_USER
    DB_USER=${DB_USER:-postgres}
    
    read -s -p "Enter PostgreSQL password: " DB_PASSWORD
    echo
    
    read -p "Enter database name (default: diffgrader): " DB_NAME
    DB_NAME=${DB_NAME:-diffgrader}
    
    # Create database
    print_status "Creating database '$DB_NAME'..."
    PGPASSWORD=$DB_PASSWORD createdb -h localhost -U $DB_USER $DB_NAME 2>/dev/null || {
        print_warning "Database '$DB_NAME' might already exist"
    }
    
    # Create user for application
    read -p "Enter application database username (default: diffgrader): " APP_USER
    APP_USER=${APP_USER:-diffgrader}
    
    read -s -p "Enter application database password (default: password): " APP_PASSWORD
    APP_PASSWORD=${APP_PASSWORD:-password}
    echo
    
    print_status "Creating application user '$APP_USER'..."
    PGPASSWORD=$DB_PASSWORD psql -h localhost -U $DB_USER -d $DB_NAME -c "
        CREATE USER $APP_USER WITH PASSWORD '$APP_PASSWORD';
        GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $APP_USER;
        GRANT ALL ON SCHEMA public TO $APP_USER;
    " 2>/dev/null || {
        print_warning "User '$APP_USER' might already exist"
    }
    
    print_success "Database setup completed"
    
    # Store database configuration
    cat > .env << EOF
DB_USERNAME=$APP_USER
DB_PASSWORD=$APP_PASSWORD
DB_NAME=$DB_NAME
EOF
    
    print_success "Database configuration saved to .env file"
}

# Create application.yml
create_config() {
    print_status "Creating application configuration..."
    
    if [ -f ".env" ]; then
        source .env
    fi
    
    cat > src/main/resources/application.yml << EOF
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  application:
    name: diffgrader-backend
  
  datasource:
    url: jdbc:postgresql://localhost:5432/${DB_NAME:-diffgrader}
    username: \${DB_USERNAME:${DB_USERNAME:-diffgrader}}
    password: \${DB_PASSWORD:${DB_PASSWORD:-password}}
    driver-class-name: org.postgresql.Driver
    
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
      
  jackson:
    serialization:
      write-dates-as-timestamps: false
    default-property-inclusion: non_null

logging:
  level:
    com.diffgrader: INFO
    org.springframework.security: WARN
    org.hibernate.SQL: WARN
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

# Application specific configuration
diffgrader:
  file:
    upload-path: \${FILE_UPLOAD_PATH:./uploads}
    max-size: 52428800 # 50MB in bytes
    allowed-types: 
      - application/zip
      - application/x-zip-compressed
      
  aws:
    s3:
      bucket-name: \${AWS_S3_BUCKET:diffgrader-files}
      region: \${AWS_REGION:us-east-1}
      enabled: \${AWS_S3_ENABLED:false}
      
  jwt:
    secret: \${JWT_SECRET:mySecretKey123456789012345678901234567890}
    expiration: 86400000 # 24 hours in milliseconds
    
  analysis:
    timeout: 300000 # 5 minutes in milliseconds
    similarity-threshold: 0.7
    max-concurrent-analyses: 5

# Actuator endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

# Swagger/OpenAPI Documentation
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method
EOF
    
    print_success "Configuration file created"
}

# Create upload directory
create_directories() {
    print_status "Creating necessary directories..."
    
    mkdir -p uploads
    mkdir -p logs
    
    print_success "Directories created"
}

# Build application
build_application() {
    print_status "Building application..."
    
    if command_exists mvn; then
        mvn clean compile
    else
        ./mvnw clean compile
    fi
    
    print_success "Application built successfully"
}

# Run tests
run_tests() {
    print_status "Running tests..."
    
    if command_exists mvn; then
        mvn test
    else
        ./mvnw test
    fi
    
    print_success "Tests completed"
}

# Main installation function
install() {
    echo
    print_status "Starting DiffGrader Backend installation..."
    echo
    
    # Check prerequisites
    check_java
    check_maven
    check_postgresql
    echo
    
    # Setup database
    print_status "Database setup required..."
    read -p "Do you want to setup the database now? (y/n): " SETUP_DB
    if [[ $SETUP_DB =~ ^[Yy]$ ]]; then
        setup_database
    else
        print_warning "Skipping database setup. Please configure manually."
    fi
    echo
    
    # Create configuration
    create_config
    create_directories
    echo
    
    # Build and test
    build_application
    echo
    
    read -p "Do you want to run tests? (y/n): " RUN_TESTS
    if [[ $RUN_TESTS =~ ^[Yy]$ ]]; then
        run_tests
    fi
    echo
    
    print_success "🎉 Installation completed successfully!"
    echo
    echo "Next steps:"
    echo "1. Start the application:"
    echo "   ./mvnw spring-boot:run"
    echo
    echo "2. Access the application:"
    echo "   - API: http://localhost:8080/api"
    echo "   - Swagger UI: http://localhost:8080/api/swagger-ui.html"
    echo "   - Health Check: http://localhost:8080/api/actuator/health"
    echo
    echo "3. Configure environment variables in .env file if needed"
    echo
    print_success "Happy coding! 🚀"
}

# Script execution
case "${1:-install}" in
    install)
        install
        ;;
    check)
        check_java
        check_maven
        check_postgresql
        ;;
    database)
        setup_database
        ;;
    config)
        create_config
        ;;
    build)
        build_application
        ;;
    test)
        run_tests
        ;;
    *)
        echo "Usage: $0 {install|check|database|config|build|test}"
        echo
        echo "Commands:"
        echo "  install   - Full installation (default)"
        echo "  check     - Check prerequisites only"
        echo "  database  - Setup database only"
        echo "  config    - Create configuration only"
        echo "  build     - Build application only"
        echo "  test      - Run tests only"
        exit 1
        ;;
esac 