// pages/admin/index/index.js
const app = getApp()

Page({

  /**
   * 页面的初始数据
   */
  data: {
    statistics: {
      branchManagerCount: 0,
      shopCount: 0,
      driverCount: 0,
      productCount: 0
    },
    branchManagers: [],
    loading: false,
    needRefresh: false
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 检查登录状态
    if (!app.checkLogin()) {
      return
    }
    this.loadData()
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // 检查登录状态
    if (!app.globalData.token) {
      return
    }
    // 检查是否需要刷新
    if (this.data.needRefresh) {
      this.loadData()
      this.setData({ needRefresh: false })
    }
  },

  /**
   * 加载数据
   */
  loadData() {
    this.setData({ loading: true })

    // 并行加载统计数据和分管理列表
    Promise.all([
      this.loadStatistics(),
      this.loadRecentBranchManagers()
    ]).finally(() => {
      this.setData({ loading: false })
    })
  },

  /**
   * 加载统计数据
   */
  loadStatistics() {
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${app.globalData.baseUrl}/admin/statistics`,
        method: 'GET',
        header: {
          'Authorization': `Bearer ${app.globalData.token}`
        },
        success: (res) => {
          if (res.data.code === 200) {
            this.setData({
              statistics: res.data.data
            })
            resolve(res.data.data)
          } else {
            console.error('加载统计数据失败:', res.data.message)
            resolve(null)
          }
        },
        fail: (err) => {
          console.error('网络请求失败:', err)
          resolve(null)
        }
      })
    })
  },

  /**
   * 加载最近的分管理列表
   */
  loadRecentBranchManagers() {
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${app.globalData.baseUrl}/admin/branch-managers`,
        method: 'GET',
        data: {
          page: 1,
          size: 5
        },
        header: {
          'Authorization': `Bearer ${app.globalData.token}`
        },
        success: (res) => {
          if (res.data.code === 200) {
            this.setData({
              branchManagers: res.data.data.list || []
            })
            resolve(res.data.data)
          } else {
            console.error('加载分管理列表失败:', res.data.message)
            resolve(null)
          }
        },
        fail: (err) => {
          console.error('网络请求失败:', err)
          resolve(null)
        }
      })
    })
  },

  /**
   * 跳转到分管理列表
   */
  goToBranchList() {
    wx.navigateTo({
      url: '/pages/admin/branch-list/branch-list'
    })
  },

  /**
   * 跳转到分管理设置
   */
  goToBranchDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/admin/branch-detail/branch-detail?id=${id}&mode=setting`
    })
  },

  /**
   * 跳转到新增分管理
   */
  goToAddBranch() {
    wx.navigateTo({
      url: '/pages/admin/branch-detail/branch-detail?mode=add'
    })
  },

  /**
   * 跳转到数据中心
   */
  goToStatistics() {
    wx.navigateTo({
      url: '/pages/admin/statistics/statistics'
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
          // 清除本地存储
          wx.removeStorageSync('token')
          wx.removeStorageSync('userInfo')

          // 清除全局数据
          app.globalData.token = null
          app.globalData.userInfo = null
          app.globalData.role = null

          // 跳转到登录页
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
    this.loadData().finally(() => {
      wx.stopPullDownRefresh()
    })
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
