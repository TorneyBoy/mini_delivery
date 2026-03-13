// components/tab-bar/index.js
Component({
    properties: {
        current: {
            type: Number,
            value: 0
        }
    },

    data: {
        list: [
            { text: '商品', icon: '🛒', page: '/pages/shop/index/index' },
            { text: '订单', icon: '📋', page: '/pages/shop/order-list/order-list' },
            { text: '账单', icon: '💰', page: '/pages/shop/bill-list/bill-list' },
            { text: '我的', icon: '👤', page: '/pages/shop/profile/profile' }
        ]
    },

    methods: {
        onSwitch(e) {
            const { page, index } = e.currentTarget.dataset;
            if (index === this.data.current) return;
            wx.redirectTo({ url: page });
        }
    }
});
