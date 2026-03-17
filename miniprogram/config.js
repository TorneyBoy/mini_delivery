// config.js
// 环境配置文件

/**
 * 环境选择
 * dev: 开发环境（本地调试）
 * prod: 生产环境（正式上线）
 * 
 * 使用说明：
 * - 本地开发/真机调试：使用 dev 环境
 * - 正式上线：使用 prod 环境
 */
const env = 'prod';

const config = {
    dev: {
        // 开发环境配置
        // 真机调试时，请将下面的IP地址修改为你电脑的局域网IP
        // 
        // 查看方法:
        // Windows: 打开命令行，输入 ipconfig，找到 IPv4 地址
        // Mac/Linux: 打开终端，输入 ifconfig 或 ip addr
        // 
        // 例如: 如果你的电脑IP是 192.168.1.50，则修改为:
        // baseUrl: 'http://192.168.1.50:8081/api'
        baseUrl: 'http://localhost:8081/api',
        tencentMapKey: '' // 腾讯地图key（可选）
    },
    prod: {
        // 生产环境配置
        // 上线时修改为正式服务器地址，必须是https
        // 格式: https://你的域名/api
        baseUrl: 'https://your-domain.com/api',
        tencentMapKey: '' // 腾讯地图key，上线前需要配置
    }
};

module.exports = {
    baseUrl: config[env].baseUrl,
    tencentMapKey: config[env].tencentMapKey,
    env: env
};
