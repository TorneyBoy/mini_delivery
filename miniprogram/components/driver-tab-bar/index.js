// components/driver-tab-bar/index.js
Component({
    properties: {
        current: {
            type: Number,
            value: 0
        },
        pendingCount: {
            type: Number,
            value: 0
        },
        pickingCount: {
            type: Number,
            value: 0
        },
        deliveryCount: {
            type: Number,
            value: 0
        }
    },

    data: {
        list: [
            { text: '待分配', icon: '📋', page: '/pages/driver/pending/pending' },
            { text: '待拣货', icon: '📦', page: '/pages/driver/picking/picking' },
            { text: '待送货', icon: '🚚', page: '/pages/driver/delivery/delivery' },
            { text: '我的', icon: '👤', page: '/pages/driver/profile/profile' }
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
