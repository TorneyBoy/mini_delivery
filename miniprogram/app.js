// app.js
const config = require('./config.js');

App({
    globalData: {
        userInfo: null,
        token: null,
        role: null,
        baseUrl: config.baseUrl,
        cartItems: [],  // 购物车商品列表
        orderItems: [], // 订单商品列表
        orderAmount: '0.00', // 订单金额
        tencentMapKey: config.tencentMapKey
    },

    onLaunch() {
        // 检查登录状态
        const token = wx.getStorageSync('token');
        const userInfo = wx.getStorageSync('userInfo');

        if (token && userInfo) {
            this.globalData.token = token;
            this.globalData.userInfo = userInfo;
            this.globalData.role = userInfo.role;
        }
    },

    // 登录方法
    login(phone, password) {
        return new Promise((resolve, reject) => {
            wx.request({
                url: `${this.globalData.baseUrl}/auth/login`,
                method: 'POST',
                data: { phone, password },
                success: (res) => {
                    if (res.data.code === 200) {
                        const data = res.data.data;
                        this.globalData.token = data.token;
                        this.globalData.userInfo = {
                            id: data.userId,
                            userId: data.userId,
                            phone: data.phone,
                            role: data.role,
                            roleDesc: data.roleDesc,
                            brandName: data.brandName || null,
                            name: data.name || null
                        };
                        this.globalData.role = data.role;

                        wx.setStorageSync('token', data.token);
                        wx.setStorageSync('userInfo', this.globalData.userInfo);

                        resolve(data);
                    } else {
                        reject(res.data.message);
                    }
                },
                fail: (err) => {
                    reject('网络请求失败');
                }
            });
        });
    },

    // 登出方法
    logout() {
        this.globalData.token = null;
        this.globalData.userInfo = null;
        this.globalData.role = null;
        wx.removeStorageSync('token');
        wx.removeStorageSync('userInfo');
        wx.reLaunch({ url: '/pages/login/login' });
    },

    // 检查登录状态
    checkLogin() {
        if (!this.globalData.token) {
            wx.reLaunch({ url: '/pages/login/login' });
            return false;
        }
        return true;
    },

    // 获取请求头
    getHeaders() {
        return {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${this.globalData.token}`
        };
    },

    // 封装请求方法
    request(options) {
        return new Promise((resolve, reject) => {
            wx.request({
                url: `${this.globalData.baseUrl}${options.url}`,
                method: options.method || 'GET',
                data: options.data || {},
                header: this.getHeaders(),
                success: (res) => {
                    if (res.data.code === 200) {
                        resolve(res.data.data);
                    } else if (res.data.code === 401) {
                        this.logout();
                        reject('登录已过期');
                    } else {
                        reject(res.data.message);
                    }
                },
                fail: (err) => {
                    reject('网络请求失败');
                }
            });
        });
    },

    /**
     * 获取完整的图片URL
     * @param {string} path - 图片路径（可能是相对路径或完整URL）
     * @returns {string} 完整的图片URL
     */
    getImageUrl(path) {
        if (!path) {
            return '';
        }
        // 如果已经是完整URL（http/https开头），直接返回
        if (path.startsWith('http://') || path.startsWith('https://')) {
            return path;
        }
        // 如果是相对路径，拼接服务器地址
        // baseUrl 格式为 http://localhost:8081/api，需要去掉 /api 部分
        let serverUrl = this.globalData.baseUrl;
        if (serverUrl.endsWith('/api')) {
            serverUrl = serverUrl.slice(0, -4); // 去掉 '/api'
        }
        // 确保路径以 / 开头
        if (!path.startsWith('/')) {
            path = '/' + path;
        }
        return serverUrl + path;
    }
});
