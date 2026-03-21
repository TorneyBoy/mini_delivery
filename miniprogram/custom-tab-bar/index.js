// custom-tab-bar/index.js
Component({
    data: {
        selected: 0,
        color: "#999999",
        selectedColor: "#1890ff",
        list: [
            {
                pagePath: "/pages/shop/index/index",
                text: "商品",
                iconPath: "/assets/icons/shop.png",
                selectedIconPath: "/assets/icons/shop-active.png"
            },
            {
                pagePath: "/pages/shop/order-list/order-list",
                text: "订单",
                iconPath: "/assets/icons/order.png",
                selectedIconPath: "/assets/icons/order-active.png"
            },
            {
                pagePath: "/pages/shop/bill-list/bill-list",
                text: "账单",
                iconPath: "/assets/icons/bill.png",
                selectedIconPath: "/assets/icons/bill-active.png"
            }
        ]
    },

    methods: {
        switchTab(e) {
            const data = e.currentTarget.dataset;
            const url = data.path;
            wx.switchTab({ url });
        }
    }
});
