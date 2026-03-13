// pages/shop/bill-list/bill-list.js
const app = getApp();

Page({
  data: {
    bills: [],
    page: 1,
    size: 10,
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
        const newBills = refresh ? bills : [...this.data.bills, ...bills];

        this.setData({
          bills: newBills,
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

  // 查看账单详情
  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/shop/bill-detail/bill-detail?id=${id}` });
  },

  // 支付账单
  onPay(e) {
    const id = e.currentTarget.dataset.id;
    const bill = this.data.bills.find(b => b.id === id);

    if (!bill) return;

    // TODO: 集成微信支付
    wx.showModal({
      title: '支付确认',
      content: `确定支付 ¥${bill.totalAmount} 吗？`,
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: `/shop/bills/${id}/pay`,
            method: 'POST'
          })
            .then(() => {
              wx.showToast({ title: '支付成功', icon: 'success' });
              this.loadBills(true);
            })
            .catch(err => {
              wx.showToast({ title: err.message || '支付失败', icon: 'none' });
            });
        }
      }
    });
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.setData({
      bills: [],
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
