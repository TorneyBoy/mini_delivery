// pages/shop/order-list/order-list.js
const app = getApp();

Page({
  data: {
    tabs: [
      { key: 'today', name: '今日订单' },
      { key: 'future', name: '预定订单' },
      { key: 'history', name: '历史订单' }
    ],
    currentTab: 'today',
    orders: [],
    page: 1,
    size: 10,
    hasMore: true,
    loading: false,
    today: '',
    hour: 0,
    statusMap: {
      0: { text: '待支付', class: 'status-pending' },
      1: { text: '待分配', class: 'status-waiting' },
      2: { text: '待拣货', class: 'status-picking' },
      3: { text: '待送货', class: 'status-delivery' },
      4: { text: '已完成', class: 'status-completed' }
    }
  },

  onLoad() {
    // 订单列表需要登录
    if (!app.checkLogin()) {
      wx.reLaunch({ url: '/pages/welcome/welcome' });
      return;
    }
    // 初始化日期和时间
    const now = new Date();
    this.setData({
      today: now.toISOString().split('T')[0],
      hour: now.getHours()
    });
    this.loadOrders(true);
  },

  onShow() {
    // 刷新数据
    this.loadOrders(true);
  },

  // 切换Tab
  onTabChange(e) {
    const key = e.currentTarget.dataset.key;
    if (key === this.data.currentTab) return;

    this.setData({
      currentTab: key,
      orders: [],
      page: 1,
      hasMore: true
    });
    this.loadOrders(true);
  },

  // 加载订单
  loadOrders(refresh = false) {
    if (this.data.loading) return;
    if (!refresh && !this.data.hasMore) return;

    const page = refresh ? 1 : this.data.page;
    const today = new Date().toISOString().split('T')[0];

    this.setData({ loading: true });

    app.request({
      url: '/shop/orders',
      data: {
        page,
        size: this.data.size
      }
    })
      .then(data => {
        let orders = data.records || [];

        // 根据当前Tab过滤订单
        orders = orders.filter(order => {
          const deliveryDate = order.deliveryDate;
          const status = order.status;

          switch (this.data.currentTab) {
            case 'today':
              // 今日订单：收货日期为今天且未完成
              return deliveryDate === today && status < 4;
            case 'future':
              // 预定订单：收货日期在今天之后
              return deliveryDate > today;
            case 'history':
              // 历史订单：已完成或收货日期在今天之前
              return status === 4 || deliveryDate < today;
            default:
              return true;
          }
        });

        const newOrders = refresh ? orders : [...this.data.orders, ...orders];

        this.setData({
          orders: newOrders,
          page: page + 1,
          hasMore: data.records && data.records.length === this.data.size,
          loading: false
        });
      })
      .catch(err => {
        this.setData({ loading: false });
        wx.showToast({ title: '加载失败', icon: 'none' });
      });
  },

  // 查看订单详情
  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/shop/order-detail/order-detail?id=${id}` });
  },

  // 修改订单
  goToEdit(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/shop/order-edit/order-edit?id=${id}` });
  },

  // 检查订单是否可修改
  canModifyOrder(order) {
    const now = new Date();
    const today = now.toISOString().split('T')[0];
    const hour = now.getHours();
    const deliveryDate = order.deliveryDate;

    // 预定订单（收货日期在今天之后）：可修改
    if (deliveryDate > today) {
      return true;
    }

    // 今日订单：凌晨2点前可修改
    if (deliveryDate === today && hour < 2) {
      return true;
    }

    return false;
  },

  // 确认收货
  onConfirmReceive(e) {
    const id = e.currentTarget.dataset.id;

    wx.showModal({
      title: '确认收货',
      content: '确定已收到货物吗？',
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: `/shop/orders/${id}/receive`,
            method: 'PUT'
          })
            .then(() => {
              wx.showToast({ title: '已确认收货', icon: 'success' });
              this.loadOrders(true);
            })
            .catch(err => {
              wx.showToast({ title: err.message || '操作失败', icon: 'none' });
            });
        }
      }
    });
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.setData({
      orders: [],
      page: 1,
      hasMore: true
    });
    this.loadOrders(true);
    wx.stopPullDownRefresh();
  },

  // 加载更多
  onReachBottom() {
    this.loadOrders();
  }
});
