#!/bin/bash

# DiffGrader 快速启动脚本
# 解决所有环境问题并启动系统

set -e

echo "🚀 DiffGrader 快速启动脚本"
echo "=============================="

# 设置环境变量
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
export PATH="/opt/homebrew/opt/postgresql@15/bin:$PATH"

echo "✅ 环境变量设置完成"

# 检查并启动PostgreSQL
echo "🔧 检查PostgreSQL..."
if ! brew services list | grep postgresql | grep started > /dev/null; then
    echo "启动PostgreSQL..."
    brew services start postgresql@15
    sleep 3
fi

# 创建数据库和用户
echo "🗄️ 设置数据库..."
createdb diffgrader 2>/dev/null || echo "数据库已存在"
psql -d diffgrader -c "CREATE USER diffgrader WITH PASSWORD 'password';" 2>/dev/null || echo "用户已存在"
psql -d diffgrader -c "GRANT ALL PRIVILEGES ON DATABASE diffgrader TO diffgrader;" 2>/dev/null || echo "权限已设置"

echo "✅ 数据库设置完成"

# 恢复PostgreSQL配置
echo "🔧 恢复PostgreSQL配置..."
cat > backend/src/main/resources/application.yml << 'EOF'
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  application:
    name: diffgrader-backend
  
  datasource:
    url: jdbc:postgresql://localhost:5432/diffgrader
    username: diffgrader
    password: password
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
      
  security:
    user:
      name: admin
      password: admin123
      
  jackson:
    serialization:
      write-dates-as-timestamps: false
    default-property-inclusion: non_null

logging:
  level:
    com.diffgrader: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

# Application specific configuration
diffgrader:
  file:
    upload-path: ${FILE_UPLOAD_PATH:./uploads}
    max-size: 52428800 # 50MB in bytes
    allowed-types: 
      - application/zip
      - application/x-zip-compressed
      
  aws:
    s3:
      bucket-name: ${AWS_S3_BUCKET:diffgrader-files}
      region: ${AWS_REGION:us-east-1}
      enabled: ${AWS_S3_ENABLED:false}
      
  jwt:
    secret: ${JWT_SECRET:mySecretKey123456789012345678901234567890}
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

echo "✅ 配置文件更新完成"

# 编译后端
echo "🔧 编译后端项目..."
cd backend
mvn clean compile

# 启动后端
echo "🚀 启动后端服务..."
mvn spring-boot:run &
BACKEND_PID=$!

echo "⏳ 等待后端启动..."
sleep 20

# 检查后端状态
if curl -s http://localhost:8080/api/actuator/health > /dev/null; then
    echo "✅ 后端服务启动成功！"
    echo "🌐 后端API地址: http://localhost:8080/api"
    echo "📚 API文档: http://localhost:8080/api/swagger-ui.html"
else
    echo "❌ 后端服务启动失败"
    exit 1
fi

# 启动前端
echo "🌐 启动前端服务..."
cd ../frontend
npm start &
FRONTEND_PID=$!

echo "⏳ 等待前端启动..."
sleep 10

echo "🎉 系统启动完成！"
echo ""
echo "📱 访问地址："
echo "- 前端应用: http://localhost:3000"
echo "- 后端API: http://localhost:8080/api"
echo "- API文档: http://localhost:8080/api/swagger-ui.html"
echo ""
echo "🛑 停止服务: 按 Ctrl+C"

# 等待用户中断
trap "echo '正在停止服务...'; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit" INT
wait 