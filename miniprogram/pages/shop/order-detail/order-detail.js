// pages/shop/order-detail/order-detail.js
const app = getApp();

Page({
  data: {
    order: null,
    loading: true,
    canModify: false,
    modifyDeadline: '',
    today: '',
    hour: 0,
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
    // 初始化日期和时间
    const now = new Date();
    this.setData({
      today: now.toISOString().split('T')[0],
      hour: now.getHours()
    });
    if (id) {
      this.loadOrderDetail(id);
    }
  },

  // 加载订单详情
  loadOrderDetail(id) {
    this.setData({ loading: true });

    app.request({ url: `/shop/orders/${id}` })
      .then(data => {
        // 检查是否可以修改
        const canModify = this.checkCanModify(data);
        let modifyDeadline = '';

        if (canModify) {
          const today = this.data.today;
          const deliveryDate = data.deliveryDate;
          if (deliveryDate === today) {
            modifyDeadline = '今日凌晨2:00前可修改';
          } else if (deliveryDate > today) {
            modifyDeadline = '预定订单可随时修改';
          }
        }

        this.setData({
          order: data,
          loading: false,
          canModify,
          modifyDeadline
        });
      })
      .catch(err => {
        this.setData({ loading: false });
        wx.showToast({ title: err.message || '加载失败', icon: 'none' });
      });
  },

  // 检查是否可以修改订单
  checkCanModify(order) {
    const today = this.data.today;
    const hour = this.data.hour;
    const deliveryDate = order.deliveryDate;

    // 已完成的订单不能修改
    if (order.status >= 4) {
      return false;
    }

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

  // 跳转到修改页面
  goToEdit() {
    const order = this.data.order;
    if (!order) return;
    wx.navigateTo({ url: `/pages/shop/order-edit/order-edit?id=${order.id}` });
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
