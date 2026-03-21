// pages/shop/profile/profile.js
const app = getApp();

Page({
    data: {
        shopInfo: null,
        statistics: {
            totalOrders: 0,
            totalAmount: '0.00',
            pendingBills: 0,
            completedOrders: 0
        },
        loading: true,
        menuList: [
            { icon: '📊', title: '历史订单', desc: '查看所有历史订单', page: '/pages/shop/order-list/order-list' },
            { icon: '💳', title: '账单记录', desc: '查看账单和支付记录', page: '/pages/shop/bill-list/bill-list' }
        ]
    },

    onLoad() {
        // 个人中心需要登录
        if (!app.checkLogin()) {
            wx.reLaunch({ url: '/pages/welcome/welcome' });
            return;
        }
        this.loadShopInfo();
        this.loadStatistics();
    },

    onShow() {
        // 刷新数据
        this.loadStatistics();
    },

    // 加载店铺信息
    loadShopInfo() {
        app.request({ url: '/shop/info' })
            .then(data => {
                this.setData({
                    shopInfo: data,
                    loading: false
                });
            })
            .catch(err => {
                this.setData({ loading: false });
                console.error('加载店铺信息失败', err);
            });
    },

    // 加载统计数据
    loadStatistics() {
        // 获取订单统计
        app.request({ url: '/shop/orders', data: { page: 1, size: 100 } })
            .then(data => {
                const orders = data.records || [];
                const totalOrders = orders.length;
                const completedOrders = orders.filter(o => o.status === 4).length;
                let totalAmount = 0;
                orders.forEach(o => {
                    totalAmount += parseFloat(o.totalAmount || 0);
                });

                this.setData({
                    'statistics.totalOrders': totalOrders,
                    'statistics.completedOrders': completedOrders,
                    'statistics.totalAmount': totalAmount.toFixed(2)
                });
            })
            .catch(err => console.error(err));

        // 获取账单统计
        app.request({ url: '/shop/bills', data: { page: 1, size: 100 } })
            .then(data => {
                const bills = data.records || [];
                const pendingBills = bills.filter(b => b.status === 0).length;

                this.setData({
                    'statistics.pendingBills': pendingBills
                });
            })
            .catch(err => console.error(err));
    },

    // 跳转页面
    goToPage(e) {
        const page = e.currentTarget.dataset.page;
        if (page) {
            wx.navigateTo({ url: page });
        } else {
            wx.showToast({ title: '功能开发中', icon: 'none' });
        }
    },

    // 退出登录
    onLogout() {
        wx.showModal({
            title: '提示',
            content: '确定要退出登录吗？',
            success: (res) => {
                if (res.confirm) {
                    app.logout();
                }
            }
        });
    }
});
