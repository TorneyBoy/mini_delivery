// app.js
App({
    globalData: {
        userInfo: null,
        token: null,
        role: null,
        baseUrl: 'http://localhost:8081/api',
        cartItems: [],  // 购物车商品列表
        orderItems: [], // 订单商品列表
        orderAmount: '0.00' // 订单金额
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
                            userId: data.userId,
                            phone: data.phone,
                            role: data.role,
                            roleDesc: data.roleDesc
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
    }
});
