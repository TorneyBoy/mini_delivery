// pages/driver/profile/profile.js
const app = getApp();

Page({
    data: {
        driverInfo: {},
        statistics: {
            todayOrders: 0,
            todayShops: 0,
            totalOrders: 0,
            totalAmount: '0.00'
        },
        historyList: [],
        loading: false
    },

    onLoad() {
        this.loadDriverInfo();
        this.loadStatistics();
        this.loadHistory();
    },

    onShow() {
        this.loadStatistics();
    },

    onPullDownRefresh() {
        Promise.all([this.loadStatistics(), this.loadHistory()]).then(() => {
            wx.stopPullDownRefresh();
        });
    },

    // 加载司机信息
    loadDriverInfo() {
        const userInfo = wx.getStorageSync('userInfo');
        if (userInfo) {
            this.setData({
                driverInfo: userInfo
            });
        }

        // 从服务器获取最新信息
        app.request({ url: '/driver/info' })
            .then(data => {
                this.setData({
                    driverInfo: data || userInfo
                });
                wx.setStorageSync('userInfo', data);
            })
            .catch(err => {
                console.error('获取司机信息失败', err);
            });
    },

    // 加载统计数据
    loadStatistics() {
        return app.request({ url: '/driver/statistics' })
            .then(data => {
                this.setData({
                    statistics: data || {
                        todayOrders: 0,
                        todayShops: 0,
                        totalOrders: 0,
                        totalAmount: '0.00'
                    }
                });
            })
            .catch(err => {
                console.error('获取统计数据失败', err);
            });
    },

    // 加载历史记录
    loadHistory() {
        this.setData({ loading: true });

        return app.request({ url: '/driver/delivery-history' })
            .then(data => {
                this.setData({
                    historyList: data || [],
                    loading: false
                });
            })
            .catch(err => {
                this.setData({ loading: false });
                console.error('获取历史记录失败', err);
            });
    },

    // 退出登录
    onLogout() {
        wx.showModal({
            title: '退出登录',
            content: '确定要退出登录吗？',
            success: (res) => {
                if (res.confirm) {
                    // 调用全局登出方法，清除所有登录状态
                    app.logout();
                }
            }
        });
    },

    // 跳转到商品图片请求页面
    goToProductRequests() {
        wx.navigateTo({
            url: '/pages/driver/product-requests/product-requests'
        });
    }
});
