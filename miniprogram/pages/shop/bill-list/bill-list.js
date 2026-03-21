// pages/shop/bill-list/bill-list.js
const app = getApp();

Page({
  data: {
    groupedBills: [],  // 按日期分组的账单
    allBills: [],      // 所有账单原始数据
    page: 1,
    size: 20,
    hasMore: true,
    loading: false,
    statusMap: {
      0: { text: '待支付', class: 'status-pending' },
      1: { text: '已支付', class: 'status-paid' }
    }
  },

  onLoad() {
    this.loadBills(true);
  },

  onShow() {
    this.loadBills(true);
  },

  // 加载账单
  loadBills(refresh = false) {
    if (this.data.loading) return;
    if (!refresh && !this.data.hasMore) return;

    const page = refresh ? 1 : this.data.page;

    this.setData({ loading: true });

    app.request({
      url: '/shop/bills',
      data: {
        page,
        size: this.data.size
      }
    })
      .then(data => {
        const bills = data.records || [];
        const allBills = refresh ? bills : [...this.data.allBills, ...bills];

        // 按日期分组
        const groupedBills = this.groupBillsByDate(allBills);

        this.setData({
          allBills,
          groupedBills,
          page: page + 1,
          hasMore: bills.length === this.data.size,
          loading: false
        });
      })
      .catch(err => {
        this.setData({ loading: false });
        wx.showToast({ title: '加载失败', icon: 'none' });
      });
  },

  // 按日期分组账单
  groupBillsByDate(bills) {
    const groups = {};

    bills.forEach(bill => {
      const date = bill.billDate || '未知日期';
      if (!groups[date]) {
        groups[date] = {
          date: date,
          bills: [],
          totalAmount: 0,
          pendingCount: 0,
          paidCount: 0
        };
      }

      groups[date].bills.push(bill);
      groups[date].totalAmount += parseFloat(bill.totalAmount || 0);

      if (bill.status === 0) {
        groups[date].pendingCount++;
      } else {
        groups[date].paidCount++;
      }
    });

    // 转换为数组并按日期降序排序
    const result = Object.values(groups).sort((a, b) => {
      return new Date(b.date) - new Date(a.date);
    });

    // 格式化金额
    result.forEach(group => {
      group.totalAmount = group.totalAmount.toFixed(2);
    });

    return result;
  },

  // 格式化日期显示
  formatDateDisplay(dateStr) {
    const today = this.formatDate(new Date());
    const yesterday = this.formatDate(new Date(Date.now() - 86400000));

    if (dateStr === today) {
      return '今天';
    } else if (dateStr === yesterday) {
      return '昨天';
    }
    return dateStr;
  },

  formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  },

  // 查看账单详情
  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/shop/bill-detail/bill-detail?id=${id}` });
  },

  // 支付账单
  onPay(e) {
    const id = e.currentTarget.dataset.id;
    const bill = this.data.allBills.find(b => b.id === id);

    if (!bill) return;

    wx.showModal({
      title: '支付确认',
      content: `确定支付 ¥${bill.totalAmount} 吗？`,
      success: (res) => {
        if (res.confirm) {
          this.doWechatPay(id);
        }
      }
    });
  },

  // 调用微信支付
  doWechatPay(billId) {
    // 获取用户openid
    const openid = wx.getStorageSync('openid');
    if (!openid) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '支付中...' });

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
        wx.hideLoading();
        wx.showToast({ title: '支付成功', icon: 'success' });
        // 刷新账单列表
        this.setData({
          allBills: [],
          page: 1,
          hasMore: true
        });
        this.loadBills(true);
      })
      .catch(err => {
        wx.hideLoading();
        console.error('支付失败', err);
        if (err.errMsg && err.errMsg.includes('cancel')) {
          wx.showToast({ title: '已取消支付', icon: 'none' });
        } else {
          wx.showToast({ title: err.message || err.errMsg || '支付失败', icon: 'none' });
        }
      });
  },

  // 调用微信支付API
  requestPayment(payData) {
    return new Promise((resolve, reject) => {
      wx.requestPayment({
        timeStamp: payData.timeStamp,
        nonceStr: payData.nonceStr,
        package: payData.packageValue,
        signType: payData.signType || 'RSA',
        paySign: payData.paySign,
        success: resolve,
        fail: reject
      });
    });
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.setData({
      allBills: [],
      groupedBills: [],
      page: 1,
      hasMore: true
    });
    this.loadBills(true);
    wx.stopPullDownRefresh();
  },

  // 加载更多
  onReachBottom() {
    this.loadBills();
  }
});
