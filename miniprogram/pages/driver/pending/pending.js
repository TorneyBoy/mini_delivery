// pages/driver/pending/pending.js
const app = getApp();

Page({
  data: {
    orders: [],
    selectedOrders: [],
    loading: false,
    submitting: false
  },

  onLoad() {
    // 检查登录状态
    if (!app.checkLogin()) {
      return
    }
    this.loadPendingOrders();
  },

  onShow() {
    // 检查登录状态
    if (!app.globalData.token) {
      return
    }
    this.loadPendingOrders();
  },

  onPullDownRefresh() {
    this.loadPendingOrders().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  // 加载待分配订单
  loadPendingOrders() {
    this.setData({ loading: true });

    return app.request({ url: '/driver/pending-orders' })
      .then(data => {
        const orders = (data || []).map(order => ({
          ...order,
          selected: false,
          itemsText: (order.items || []).map(item =>
            `${item.productName} ${item.quantity}${item.unit || '斤'}`
          ).join('、')
        }));

        this.setData({
          orders,
          loading: false,
          selectedOrders: []
        });
      })
      .catch(err => {
        this.setData({ loading: false });
        wx.showToast({ title: '加载失败', icon: 'none' });
      });
  },

  // 选择/取消选择订单
  onToggleSelect(e) {
    const { id } = e.currentTarget.dataset;
    const orders = this.data.orders.map(order => {
      if (order.id === id) {
        return { ...order, selected: !order.selected };
      }
      return order;
    });

    const selectedOrders = orders.filter(o => o.selected).map(o => o.id);

    this.setData({ orders, selectedOrders });
  },

  // 全选
  onSelectAll() {
    const allSelected = this.data.selectedOrders.length === this.data.orders.length;

    if (allSelected) {
      // 取消全选
      const orders = this.data.orders.map(order => ({ ...order, selected: false }));
      this.setData({ orders, selectedOrders: [] });
    } else {
      // 全选
      const orders = this.data.orders.map(order => ({ ...order, selected: true }));
      const selectedOrders = orders.map(o => o.id);
      this.setData({ orders, selectedOrders });
    }
  },

  // 确认选择订单
  onConfirmSelect() {
    if (this.data.selectedOrders.length === 0) {
      wx.showToast({ title: '请选择订单', icon: 'none' });
      return;
    }

    wx.showModal({
      title: '确认选择',
      content: `确定要选择 ${this.data.selectedOrders.length} 个订单吗？`,
      success: (res) => {
        if (res.confirm) {
          this.submitSelectOrders();
        }
      }
    });
  },

  // 提交选择的订单
  submitSelectOrders() {
    this.setData({ submitting: true });

    app.request({
      url: '/driver/pending-orders/select',
      method: 'POST',
      data: this.data.selectedOrders
    })
      .then(() => {
        wx.showToast({ title: '选择成功', icon: 'success' });
        this.loadPendingOrders();
      })
      .catch(err => {
        wx.showToast({ title: err || '选择失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ submitting: false });
      });
  },

  // 查看订单详情
  onViewDetail(e) {
    const { order } = e.currentTarget.dataset;
    wx.showModal({
      title: '订单详情',
      content: `订单号：${order.orderNo}\n店铺：${order.shopName || '未知'}\n商品：${order.itemsText}\n总金额：¥${order.totalAmount}\n收货日期：${order.deliveryDate}`,
      showCancel: false
    });
  }
});
