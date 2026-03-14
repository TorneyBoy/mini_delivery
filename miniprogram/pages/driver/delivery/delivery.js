// pages/driver/delivery/delivery.js
const app = getApp();

Page({
  data: {
    deliveryList: [],
    loading: false,
    completing: false,
    showDetail: false,
    currentDelivery: null
  },

  onLoad() {
    this.loadDeliveryList();
  },

  onShow() {
    this.loadDeliveryList();
  },

  onPullDownRefresh() {
    this.loadDeliveryList().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  // 加载送货清单
  loadDeliveryList() {
    this.setData({ loading: true });

    return app.request({ url: '/driver/delivery-list' })
      .then(data => {
        this.setData({
          deliveryList: data || [],
          loading: false
        });
      })
      .catch(err => {
        this.setData({ loading: false });
        wx.showToast({ title: '加载失败', icon: 'none' });
      });
  },

  // 查看详情
  onViewDetail(e) {
    const { delivery } = e.currentTarget.dataset;
    this.setData({
      showDetail: true,
      currentDelivery: delivery
    });
  },

  // 关闭详情弹窗
  onCloseDetail() {
    this.setData({
      showDetail: false,
      currentDelivery: null
    });
  },

  // 打开地图导航
  onOpenMap(e) {
    const { address, name } = e.currentTarget.dataset;

    // 先尝试获取地址的经纬度
    wx.showLoading({ title: '获取位置中...' });

    // 从全局配置获取腾讯地图key
    const mapKey = app.globalData.tencentMapKey || '';

    if (!mapKey) {
      // 如果没有配置地图key，直接复制地址
      wx.hideLoading();
      this.copyAddress(address);
      wx.showToast({ title: '请配置腾讯地图Key', icon: 'none' });
      return;
    }

    wx.request({
      url: 'https://apis.map.qq.com/ws/geocoder/v1/',
      data: {
        address: address,
        key: mapKey
      },
      success: (res) => {
        wx.hideLoading();
        if (res.data.status === 0 && res.data.result && res.data.result.location) {
          const location = res.data.result.location;
          wx.openLocation({
            latitude: location.lat,
            longitude: location.lng,
            name: name || '目的地',
            address: address,
            scale: 18
          });
        } else {
          // 如果获取失败，直接复制地址
          this.copyAddress(address);
        }
      },
      fail: () => {
        wx.hideLoading();
        this.copyAddress(address);
      }
    });
  },

  // 复制地址
  copyAddress(address) {
    wx.setClipboardData({
      data: address,
      success: () => {
        wx.showToast({
          title: '地址已复制',
          icon: 'success'
        });
      }
    });
  },

  // 完成送货
  onCompleteDelivery(e) {
    const { id, name } = e.currentTarget.dataset;

    wx.showModal({
      title: '确认送达',
      content: `确定已将货物送达「${name}」吗？`,
      success: (res) => {
        if (res.confirm) {
          this.submitCompleteDelivery(id);
        }
      }
    });
  },

  // 提交完成送货
  submitCompleteDelivery(deliveryId) {
    this.setData({ completing: true });

    app.request({
      url: `/driver/delivery-list/${deliveryId}/complete`,
      method: 'PUT'
    })
      .then(() => {
        wx.showToast({ title: '送达成功', icon: 'success' });
        this.loadDeliveryList();
      })
      .catch(err => {
        wx.showToast({ title: err || '操作失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ completing: false });
      });
  },

  // 拨打电话
  onCallPhone(e) {
    const { phone } = e.currentTarget.dataset;
    wx.makePhoneCall({
      phoneNumber: phone,
      fail: () => {
        wx.showToast({ title: '拨号取消', icon: 'none' });
      }
    });
  },

  // 获取商品汇总文本
  getItemsText(items) {
    if (!items || items.length === 0) return '';
    return items.map(item => `${item.productName} ${item.quantity}${item.unit || '斤'}`).join('、');
  }
});
