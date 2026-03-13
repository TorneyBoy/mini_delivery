// pages/branch/index/index.js
const app = getApp()

Page({

  /**
   * 页面的初始数据
   */
  data: {
    brandName: '',
    statistics: {
      shopCount: 0,
      driverCount: 0,
      productCount: 0,
      orderCount: 0
    }
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    this.loadUserInfo()
    this.loadStatistics()
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    this.loadStatistics()
  },

  /**
   * 加载用户信息
   */
  loadUserInfo() {
    const userInfo = app.globalData.userInfo
    if (userInfo) {
      this.setData({
        brandName: userInfo.brandName || '分管理'
      })
    }
  },

  /**
   * 加载统计数据
   */
  loadStatistics() {
    wx.request({
      url: `${app.globalData.baseUrl}/branch/statistics`,
      method: 'GET',
      header: {
        'Authorization': `Bearer ${app.globalData.token}`
      },
      success: (res) => {
        if (res.data.code === 200) {
          this.setData({
            statistics: res.data.data
          })
        }
      },
      fail: (err) => {
        console.error('加载统计数据失败:', err)
      }
    })
  },

  /**
   * 跳转到店铺列表
   */
  goToShopList() {
    wx.navigateTo({
      url: '/pages/branch/shop-list/shop-list'
    })
  },

  /**
   * 跳转到添加店铺
   */
  goToAddShop() {
    wx.navigateTo({
      url: '/pages/branch/shop-edit/shop-edit?mode=add'
    })
  },

  /**
   * 跳转到司机列表
   */
  goToDriverList() {
    wx.navigateTo({
      url: '/pages/branch/driver-list/driver-list'
    })
  },

  /**
   * 跳转到添加司机
   */
  goToAddDriver() {
    wx.navigateTo({
      url: '/pages/branch/driver-edit/driver-edit?mode=add'
    })
  },

  /**
   * 跳转到商品列表
   */
  goToProductList() {
    wx.navigateTo({
      url: '/pages/branch/product-list/product-list'
    })
  },

  /**
   * 跳转到添加商品
   */
  goToAddProduct() {
    wx.navigateTo({
      url: '/pages/branch/product-edit/product-edit?mode=add'
    })
  },

  /**
   * 跳转到订单监控
   */
  goToOrderMonitor() {
    wx.navigateTo({
      url: '/pages/branch/order-monitor/order-monitor'
    })
  },

  /**
   * 退出登录
   */
  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('token')
          wx.removeStorageSync('userInfo')
          app.globalData.token = null
          app.globalData.userInfo = null
          app.globalData.role = null
          wx.reLaunch({
            url: '/pages/login/login'
          })
        }
      }
    })
  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {
    this.loadStatistics()
    wx.stopPullDownRefresh()
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {

  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide() {

  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {

  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {

  }
})
