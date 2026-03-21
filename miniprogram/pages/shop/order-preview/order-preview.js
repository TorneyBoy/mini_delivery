// pages/shop/order-preview/order-preview.js
const app = getApp();

Page({
  data: {
    orderItems: [],
    totalAmount: '0.00',
    deliveryDate: '',
    minDate: '',
    maxDate: '',
    dateRange: [],
    loading: false,
    remark: ''
  },

  onLoad() {
    // 从全局获取订单商品
    const orderItems = app.globalData.orderItems || [];
    const totalAmount = app.globalData.orderAmount || '0.00';

    // 计算可选日期范围
    const now = new Date();
    const hour = now.getHours();

    // 凌晨2点前可选当天，否则从次日开始
    let startDate = new Date(now);
    if (hour >= 2) {
      startDate.setDate(startDate.getDate() + 1);
    }

    // 可选未来7天
    const endDate = new Date(startDate);
    endDate.setDate(endDate.getDate() + 7);

    // 生成日期范围
    const dateRange = [];
    const tempDate = new Date(startDate);
    while (tempDate <= endDate) {
      dateRange.push({
        value: tempDate.toISOString().split('T')[0],
        label: this.formatDateLabel(tempDate)
      });
      tempDate.setDate(tempDate.getDate() + 1);
    }

    this.setData({
      orderItems,
      totalAmount,
      deliveryDate: dateRange[0]?.value || '',
      minDate: startDate.toISOString().split('T')[0],
      maxDate: endDate.toISOString().split('T')[0],
      dateRange
    });
  },

  // 格式化日期标签
  formatDateLabel(date) {
    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    const dateStr = date.toISOString().split('T')[0];
    const todayStr = today.toISOString().split('T')[0];
    const tomorrowStr = tomorrow.toISOString().split('T')[0];

    if (dateStr === todayStr) {
      return '今天';
    } else if (dateStr === tomorrowStr) {
      return '明天';
    } else {
      const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
      return `${date.getMonth() + 1}月${date.getDate()}日 ${weekDays[date.getDay()]}`;
    }
  },

  // 选择日期
  onDateChange(e) {
    const index = e.detail.value;
    this.setData({
      deliveryDate: this.data.dateRange[index].value
    });
  },

  // 输入备注
  onRemarkInput(e) {
    this.setData({
      remark: e.detail.value
    });
  },

  // 修改商品数量
  onQuantityChange(e) {
    const { index, type } = e.currentTarget.dataset;
    const orderItems = [...this.data.orderItems];
    const item = orderItems[index];

    if (type === 'plus') {
      item.quantity = parseFloat((item.quantity + 1).toFixed(2));
    } else if (type === 'minus' && item.quantity > 0) {
      item.quantity = parseFloat((item.quantity - 1).toFixed(2));
    }

    // 重新计算总价
    let totalAmount = 0;
    orderItems.forEach(item => {
      totalAmount += item.quantity * parseFloat(item.unitPrice);
    });

    this.setData({
      orderItems,
      totalAmount: totalAmount.toFixed(2)
    });
  },

  // 删除商品
  onRemoveItem(e) {
    const index = e.currentTarget.dataset.index;
    const orderItems = [...this.data.orderItems];
    orderItems.splice(index, 1);

    // 重新计算总价
    let totalAmount = 0;
    orderItems.forEach(item => {
      totalAmount += item.quantity * parseFloat(item.unitPrice);
    });

    this.setData({
      orderItems,
      totalAmount: totalAmount.toFixed(2)
    });

    if (orderItems.length === 0) {
      wx.showToast({ title: '购物车已清空', icon: 'none' });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    }
  },

  // 提交订单
  onSubmit() {
    if (this.data.orderItems.length === 0) {
      wx.showToast({ title: '请选择商品', icon: 'none' });
      return;
    }

    if (!this.data.deliveryDate) {
      wx.showToast({ title: '请选择收货日期', icon: 'none' });
      return;
    }

    this.setData({ loading: true });

    // 构建订单数据
    const orderData = {
      deliveryDate: this.data.deliveryDate,
      items: this.data.orderItems.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        unitPrice: item.unitPrice
      })),
      remark: this.data.remark
    };

    app.request({
      url: '/shop/orders',
      method: 'POST',
      data: orderData
    })
      .then(data => {
        this.setData({ loading: false });

        // 清空全局购物车
        app.globalData.orderItems = [];
        app.globalData.orderAmount = '0.00';

        wx.showToast({ title: '下单成功', icon: 'success' });

        setTimeout(() => {
          wx.redirectTo({ url: '/pages/shop/order-list/order-list' });
        }, 1500);
      })
      .catch(err => {
        this.setData({ loading: false });
        wx.showToast({ title: err.message || '下单失败', icon: 'none' });
      });
  }
});
