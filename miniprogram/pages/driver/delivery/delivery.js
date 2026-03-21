// pages/driver/delivery/delivery.js
const app = getApp();

Page({
  data: {
    deliveryList: [],
    loading: false,
    completing: false,
    showDetail: false,
    currentDelivery: null,
    // 送达照片相关
    showPhotoModal: false,
    currentDeliveryId: null,
    currentShopName: '',
    tempPhotoPath: '',
    showPreview: false
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

  // 阻止事件冒泡（空方法）
  stopPropagation() {
    // 阻止事件冒泡，不做任何处理
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

  // 完成送货 - 显示选择照片方式弹窗
  onCompleteDelivery(e) {
    const { id, name } = e.currentTarget.dataset;

    this.setData({
      showPhotoModal: true,
      currentDeliveryId: id,
      currentShopName: name,
      tempPhotoPath: ''
    });
  },

  // 关闭照片选择弹窗
  onClosePhotoModal() {
    this.setData({
      showPhotoModal: false,
      currentDeliveryId: null,
      currentShopName: '',
      tempPhotoPath: ''
    });
  },

  // 拍照
  onTakePhoto() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['camera'],
      success: (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath;
        this.setData({
          tempPhotoPath: tempFilePath,
          showPhotoModal: false,
          showPreview: true
        });
      },
      fail: (err) => {
        if (err.errMsg.indexOf('auth deny') !== -1) {
          wx.showModal({
            title: '提示',
            content: '需要授权相机权限才能拍照留证，请在设置中开启',
            confirmText: '去设置',
            success: (modalRes) => {
              if (modalRes.confirm) {
                wx.openSetting();
              }
            }
          });
        }
      }
    });
  },

  // 从图库选择
  onChooseFromAlbum() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album'],
      success: (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath;
        this.setData({
          tempPhotoPath: tempFilePath,
          showPhotoModal: false,
          showPreview: true
        });
      },
      fail: (err) => {
        if (err.errMsg.indexOf('auth deny') !== -1) {
          wx.showModal({
            title: '提示',
            content: '需要授权相册权限才能选择照片，请在设置中开启',
            confirmText: '去设置',
            success: (modalRes) => {
              if (modalRes.confirm) {
                wx.openSetting();
              }
            }
          });
        }
      }
    });
  },

  // 预览界面 - 重新选择
  onReselectPhoto() {
    this.setData({
      showPreview: false,
      showPhotoModal: true
    });
  },

  // 预览界面 - 确认提交
  onConfirmSubmit() {
    const { currentDeliveryId, tempPhotoPath } = this.data;

    if (!tempPhotoPath) {
      wx.showToast({ title: '请先选择照片', icon: 'none' });
      return;
    }

    this.uploadPhotoAndComplete(currentDeliveryId, tempPhotoPath);
  },

  // 关闭预览界面
  onClosePreview() {
    this.setData({
      showPreview: false,
      tempPhotoPath: ''
    });
  },

  // 上传照片并完成送货
  uploadPhotoAndComplete(deliveryId, tempFilePath) {
    this.setData({ completing: true });

    // 先上传照片
    wx.uploadFile({
      url: `${app.globalData.baseUrl}/upload/image`,
      filePath: tempFilePath,
      name: 'file',
      header: {
        'Authorization': `Bearer ${app.globalData.token}`
      },
      success: (uploadRes) => {
        const data = JSON.parse(uploadRes.data);
        if (data.code === 200) {
          const photoUrl = data.data.url;
          // 提交完成送货
          this.submitCompleteDelivery(deliveryId, photoUrl);
        } else {
          wx.showToast({ title: data.message || '上传失败', icon: 'none' });
          this.setData({ completing: false });
        }
      },
      fail: () => {
        wx.showToast({ title: '上传照片失败', icon: 'none' });
        this.setData({ completing: false });
      }
    });
  },

  // 提交完成送货
  submitCompleteDelivery(deliveryId, deliveryPhoto) {
    app.request({
      url: `/driver/delivery-list/${deliveryId}/complete`,
      method: 'PUT',
      data: { deliveryPhoto }
    })
      .then(() => {
        wx.showToast({ title: '送达成功', icon: 'success' });
        this.setData({
          showPreview: false,
          tempPhotoPath: '',
          currentDeliveryId: null,
          currentShopName: ''
        });
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
