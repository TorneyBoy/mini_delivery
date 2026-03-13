// pages/shop/order-detail/order-detail.js
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

    app.request({ url: `/shop/orders/${id}` })
      .then(data => {
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

  // 确认收货
  onConfirmReceive() {
    const order = this.data.order;
    if (!order) return;

    wx.showModal({
      title: '确认收货',
      content: '确定已收到货物吗？',
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: `/shop/orders/${order.id}/receive`,
            method: 'PUT'
          })
            .then(() => {
              wx.showToast({ title: '已确认收货', icon: 'success' });
              // 刷新订单详情
              this.loadOrderDetail(order.id);
            })
            .catch(err => {
              wx.showToast({ title: err.message || '操作失败', icon: 'none' });
            });
        }
      }
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

  // 返回订单列表
  goBack() {
    wx.navigateBack();
  }
});
