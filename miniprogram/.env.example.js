// 复制本文件为 .env.js，并按个人环境修改
module.exports = {
    // 开发者工具（模拟器）访问地址
    DEV_BASE_URL: 'http://localhost:8081/api',

    // 真机联调访问地址（每个开发者可改为自己的局域网IP）
    LAN_BASE_URL: 'http://你的局域网ip:8081/api',

    // 实际服务器地址（发布版自动使用）
    PROD_BASE_URL: 'https://服务器ip/api'
};
