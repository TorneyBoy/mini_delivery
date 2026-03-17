// pages/branch/order-detail/order-detail.js
const app = getApp();

Page({
    data: {
        order: null,
        loading: true,
        statusMap: {
            0: { text: '待支付', class: 'status-pending' },
            1: { text: '待分配', class: 'status-waiting' },
            2: { text: '待拣货', class: 'status-picking' },
            3: { text: '待送货', class: 'status-delivery' },
            4: { text: '已完成', class: 'status-completed' },
            5: { text: '已取消', class: 'status-cancelled' }
        }
    },

    onLoad(options) {
        const id = options.id;
        if (id) {
            this.loadOrderDetail(id);
        }
    },

    // 加载订单详情
    loadOrderDetail(id) {
        this.setData({ loading: true });

        app.request({ url: `/branch/orders/${id}` })
            .then(data => {
                // 处理图片URL
                if (data.deliveryPhoto) {
                    data.deliveryPhoto = app.getImageUrl(data.deliveryPhoto);
                }

                this.setData({
                    order: data,
                    loading: false
                });
            })
            .catch(err => {
                this.setData({ loading: false });
                wx.showToast({ title: err.message || '加载失败', icon: 'none' });
            });
    },

    // 复制订单号
    onCopyOrderNo() {
        const order = this.data.order;
        if (!order) return;

        wx.setClipboardData({
            data: order.orderNo,
            success: () => {
                wx.showToast({ title: '已复制', icon: 'success' });
            }
        });
    },

    // 预览送达照片
    onPreviewPhoto() {
        const order = this.data.order;
        if (!order || !order.deliveryPhoto) return;

        wx.previewImage({
            current: order.deliveryPhoto,
            urls: [order.deliveryPhoto]
        });
    },

    // 返回
    goBack() {
        wx.navigateBack();
    }
});
