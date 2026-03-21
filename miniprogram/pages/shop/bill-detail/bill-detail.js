// pages/shop/bill-detail/bill-detail.js
const app = getApp();

Page({
  data: {
    bill: null,
    loading: true,
    paying: false,
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

    if (bill.status === 1) {
      wx.showToast({ title: '账单已支付', icon: 'none' });
      return;
    }

    wx.showModal({
      title: '支付确认',
      content: `确定支付 ¥${bill.totalAmount} 吗？`,
      success: (res) => {
        if (res.confirm) {
          this.doWechatPay(bill.id);
        }
      }
    });
  },

  // 调用微信支付
  doWechatPay(billId) {
    if (this.data.paying) return;
    this.setData({ paying: true });

    // 获取用户openid（需要先登录获取）
    const openid = wx.getStorageSync('openid');
    if (!openid) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      this.setData({ paying: false });
      return;
    }

    // 1. 先调用后端创建支付订单
    app.request({
      url: `/pay/create/${billId}`,
      method: 'POST',
      data: { openid }
    })
      .then(payData => {
        // 2. 调用微信支付
        return this.requestPayment(payData);
      })
      .then(() => {
        // 3. 支付成功
        wx.showToast({ title: '支付成功', icon: 'success' });
        // 刷新账单详情
        this.loadBillDetail(billId);
      })
      .catch(err => {
        console.error('支付失败', err);
        if (err.errMsg && err.errMsg.includes('cancel')) {
          wx.showToast({ title: '已取消支付', icon: 'none' });
        } else {
          wx.showToast({ title: err.message || err.errMsg || '支付失败', icon: 'none' });
        }
      })
      .finally(() => {
        this.setData({ paying: false });
      });
  },

  // 调用微信支付API
  requestPayment(payData) {
    return new Promise((resolve, reject) => {
      wx.requestPayment({
        timeStamp: payData.timeStamp,
        nonceStr: payData.nonceStr,
        package: payData.packageVal,
        signType: payData.signType,
        paySign: payData.paySign,
        success: (res) => {
          resolve(res);
        },
        fail: (err) => {
          reject(err);
        }
      });
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
