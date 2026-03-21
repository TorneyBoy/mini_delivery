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
    // 检查登录状态
    if (!app.checkLogin()) {
      return
    }
    this.loadUserInfo()
    this.loadStatistics()
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // 检查登录状态
    if (!app.globalData.token) {
      return
    }
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
   * 跳转到商品图片审核
   */
  goToProductReview() {
    wx.navigateTo({
      url: '/pages/branch/product-review/product-review'
    })
  },

  /**
   * 跳转到数据中心
   */
  goToDataCenter() {
    wx.navigateTo({
      url: '/pages/branch/data-center/data-center'
    })
  },

  /**
   * 跳转到注册申请管理
   */
  goToRegistrationRequests() {
    wx.navigateTo({
      url: '/pages/branch/registration-requests/registration-requests'
    })
  },

  /**
   * 跳转到账单统计
   */
  goToBillStatistics() {
    wx.navigateTo({
      url: '/pages/branch/bill-statistics/bill-statistics'
    })
  },

  /**
   * 分享店铺注册链接
   */
  shareShopRegister() {
    const userInfo = app.globalData.userInfo
    if (!userInfo || !userInfo.id) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      return
    }

    // 设置分享状态，用于onShareAppMessage
    this._shareRole = 'SHOP'
    this._shareBranchManagerId = userInfo.id

    wx.showActionSheet({
      itemList: ['转发给微信好友', '生成小程序码'],
      success: (res) => {
        if (res.tapIndex === 0) {
          // 转发给微信好友
          wx.showModal({
            title: '转发分享',
            content: '请点击右上角"..."按钮，选择"转发"分享给好友',
            showCancel: false
          })
        } else if (res.tapIndex === 1) {
          // 生成小程序码
          this.generateQrCode('SHOP', userInfo.id)
        }
      }
    })
  },

  /**
   * 分享司机注册链接
   */
  shareDriverRegister() {
    const userInfo = app.globalData.userInfo
    if (!userInfo || !userInfo.id) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      return
    }

    // 设置分享状态，用于onShareAppMessage
    this._shareRole = 'DRIVER'
    this._shareBranchManagerId = userInfo.id

    wx.showActionSheet({
      itemList: ['转发给微信好友', '生成小程序码'],
      success: (res) => {
        if (res.tapIndex === 0) {
          // 转发给微信好友
          wx.showModal({
            title: '转发分享',
            content: '请点击右上角"..."按钮，选择"转发"分享给好友',
            showCancel: false
          })
        } else if (res.tapIndex === 1) {
          // 生成小程序码
          this.generateQrCode('DRIVER', userInfo.id)
        }
      }
    })
  },

  /**
   * 生成小程序码
   */
  generateQrCode(role, branchManagerId) {
    wx.showLoading({ title: '生成中...' })

    wx.request({
      url: `${app.globalData.baseUrl}/branch/generate-qrcode`,
      method: 'POST',
      header: {
        'Authorization': `Bearer ${app.globalData.token}`,
        'Content-Type': 'application/json'
      },
      data: {
        role: role,
        branchManagerId: branchManagerId
      },
      success: (res) => {
        wx.hideLoading()
        if (res.data.code === 200) {
          wx.previewImage({
            urls: [res.data.data.qrCodeUrl],
            current: res.data.data.qrCodeUrl
          })
        } else {
          wx.showToast({
            title: res.data.message || '生成失败',
            icon: 'none'
          })
        }
      },
      fail: (err) => {
        wx.hideLoading()
        wx.showToast({
          title: '网络请求失败',
          icon: 'none'
        })
      }
    })
  },

  /**
   * 跳转到修改密码页面
   */
  goToChangePassword() {
    wx.navigateTo({
      url: '/pages/branch/change-password/change-password'
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
    // 如果是通过分享按钮触发的
    if (this._shareRole && this._shareBranchManagerId) {
      const roleText = this._shareRole === 'SHOP' ? '店铺' : '司机'
      return {
        title: `邀请您注册成为${roleText}`,
        path: `pages/register/register?role=${this._shareRole}&branchManagerId=${this._shareBranchManagerId}`,
        success: () => {
          // 清除分享状态
          this._shareRole = null
          this._shareBranchManagerId = null
        }
      }
    }

    // 默认分享
    return {
      title: '配送管理系统',
      path: 'pages/login/login'
    }
  }
})
