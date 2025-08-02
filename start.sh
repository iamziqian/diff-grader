#!/bin/bash

# DiffGrader 一键启动脚本
# 自动检查依赖并启动前后端服务

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

command_exists() {
    command -v "$1" >/dev/null 2>&1
}

echo "🚀 DiffGrader 一键启动脚本"
echo "=============================="

# 检查并安装依赖
print_status "检查系统依赖..."

# 检查 Homebrew
if ! command_exists brew; then
    print_error "Homebrew 未安装，请先安装 Homebrew"
    echo "安装命令: /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
    exit 1
fi

# 检查并安装 PostgreSQL
if ! command_exists psql; then
    print_warning "PostgreSQL 未安装，正在安装..."
    brew install postgresql@15
    brew services start postgresql@15
    sleep 3
else
    print_success "PostgreSQL 已安装"
fi

# 检查并安装 Maven
if ! command_exists mvn; then
    print_warning "Maven 未安装，正在安装..."
    brew install maven
else
    print_success "Maven 已安装"
fi

# 创建数据库（如果不存在）
print_status "设置数据库..."
createdb diffgrader 2>/dev/null || print_warning "数据库可能已存在"

# 检查前端依赖
print_status "检查前端依赖..."
if [ ! -d "frontend/node_modules" ]; then
    print_warning "前端依赖未安装，正在安装..."
    cd frontend
    npm install
    cd ..
else
    print_success "前端依赖已安装"
fi

print_success "所有依赖检查完成！"
echo

# 询问用户启动方式
echo "请选择启动方式："
echo "1. 自动启动 (推荐) - 自动在新终端窗口启动前后端"
echo "2. 手动启动 - 显示启动命令，手动执行"
echo "3. 仅启动后端"
echo "4. 仅启动前端"

read -p "请输入选择 (1-4): " choice

case $choice in
    1)
        print_status "正在自动启动前后端服务..."
        
        # 启动后端 (在新终端)
        osascript << EOF
tell application "Terminal"
    do script "cd $(pwd)/backend && echo '🔧 启动后端服务...' && mvn spring-boot:run"
end tell
EOF
        
        sleep 2
        
        # 启动前端 (在新终端)
        osascript << EOF
tell application "Terminal"
    do script "cd $(pwd)/frontend && echo '🌐 启动前端服务...' && npm start"
end tell
EOF
        
        print_success "前后端服务启动中..."
        echo
        echo "🌐 访问地址："
        echo "- 前端应用: http://localhost:3000"
        echo "- 后端API: http://localhost:8080/api"
        echo "- API文档: http://localhost:8080/api/swagger-ui.html"
        ;;
        
    2)
        echo
        print_status "手动启动命令："
        echo
        echo "🔧 后端启动 (在新终端中):"
        echo "cd backend && mvn spring-boot:run"
        echo
        echo "🌐 前端启动 (在另一个新终端中):"
        echo "cd frontend && npm start"
        echo
        echo "📱 访问地址："
        echo "- 前端: http://localhost:3000"
        echo "- 后端: http://localhost:8080/api"
        ;;
        
    3)
        print_status "启动后端服务..."
        cd backend
        mvn spring-boot:run
        ;;
        
    4)
        print_status "启动前端服务..."
        cd frontend
        npm start
        ;;
        
    *)
        print_error "无效选择"
        exit 1
        ;;
esac

print_success "启动脚本执行完成！" 