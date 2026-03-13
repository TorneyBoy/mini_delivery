// pages/shop/bill-detail/bill-detail.js
const app = getApp();

Page({
  data: {
    bill: null,
    loading: true,
    statusMap: {
      0: { text: '待支付', class: 'status-pending' },
      1: { text: '已支付', class: 'status-paid' }
    }
  },

  onLoad(options) {
    const id = options.id;
    if (id) {
      this.loadBillDetail(id);
    }
  },

  // 加载账单详情
  loadBillDetail(id) {
    this.setData({ loading: true });

    app.request({ url: `/shop/bills/${id}` })
      .then(data => {
        this.setData({
          bill: data,
          loading: false
        });
      })
      .catch(err => {
        this.setData({ loading: false });
        wx.showToast({ title: err.message || '加载失败', icon: 'none' });
      });
  },

  // 支付账单
  onPay() {
    const bill = this.data.bill;
    if (!bill) return;

    wx.showModal({
      title: '支付确认',
      content: `确定支付 ¥${bill.totalAmount} 吗？`,
      success: (res) => {
        if (res.confirm) {
          this.doPay(bill.id);
        }
      }
    });
  },

  // 执行支付
  doPay(billId) {
    app.request({
      url: `/shop/bills/${billId}/pay`,
      method: 'POST'
    })
      .then(() => {
        wx.showToast({ title: '支付成功', icon: 'success' });
        // 刷新账单详情
        this.loadBillDetail(billId);
      })
      .catch(err => {
        wx.showToast({ title: err.message || '支付失败', icon: 'none' });
      });
  },

  // 复制账单号
  onCopyBillNo() {
    const bill = this.data.bill;
    if (!bill) return;

    wx.setClipboardData({
      data: bill.billNo,
      success: () => {
        wx.showToast({ title: '已复制', icon: 'success' });
      }
    });
  },

  // 查看订单详情
  goToOrderDetail(e) {
    const orderId = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/shop/order-detail/order-detail?id=${orderId}` });
  }
});
