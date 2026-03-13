// pages/admin/branch-detail/branch-detail.js
const app = getApp()

Page({

  /**
   * 页面的初始数据
   */
  data: {
    mode: 'add', // add, setting
    id: null,
    brandName: '',
    phone: '',
    password: '',
    status: 1,
    detail: {}
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    if (options.mode === 'add') {
      this.setData({ mode: 'add' })
      wx.setNavigationBarTitle({ title: '新增分管理' })
    } else if (options.id) {
      this.setData({
        mode: 'setting',
        id: options.id
      })
      wx.setNavigationBarTitle({ title: '分管理设置' })
      this.loadDetail(options.id)
    }
  },

  /**
   * 加载详情
   */
  loadDetail(id) {
    wx.showLoading({ title: '加载中...' })

    wx.request({
      url: `${app.globalData.baseUrl}/admin/branch-managers/${id}`,
      method: 'GET',
      header: {
        'Authorization': `Bearer ${app.globalData.token}`
      },
      success: (res) => {
        if (res.data.code === 200) {
          const data = res.data.data
          this.setData({
            detail: data,
            brandName: data.brandName,
            phone: data.phone,
            status: data.status
          })
        } else {
          wx.showToast({
            title: res.data.message || '加载失败',
            icon: 'none'
          })
        }
      },
      fail: (err) => {
        wx.showToast({
          title: '网络请求失败',
          icon: 'none'
        })
      },
      complete: () => {
        wx.hideLoading()
      }
    })
  },

  /**
   * 品牌名称输入
   */
  onBrandNameInput(e) {
    this.setData({ brandName: e.detail.value })
  },

  /**
   * 手机号输入
   */
  onPhoneInput(e) {
    this.setData({ phone: e.detail.value })
  },

  /**
   * 密码输入
   */
  onPasswordInput(e) {
    this.setData({ password: e.detail.value })
  },

  /**
   * 状态切换
   */
  onStatusChange(e) {
    const newStatus = e.detail.value ? 1 : 0
    this.setData({ status: newStatus })
  },

  /**
   * 提交
   */
  handleSubmit() {
    const { mode, id, brandName, phone, password, status } = this.data

    // 验证
    if (!brandName) {
      wx.showToast({ title: '请输入品牌名称', icon: 'none' })
      return
    }

    if (!phone) {
      wx.showToast({ title: '请输入手机号', icon: 'none' })
      return
    }

    if (mode === 'add' && !password) {
      wx.showToast({ title: '请输入密码', icon: 'none' })
      return
    }

    wx.showLoading({ title: '提交中...' })

    if (mode === 'add') {
      // 创建分管理
      wx.request({
        url: `${app.globalData.baseUrl}/admin/branch-managers`,
        method: 'POST',
        header: {
          'Authorization': `Bearer ${app.globalData.token}`,
          'Content-Type': 'application/json'
        },
        data: {
          brandName,
          phone,
          password
        },
        success: (res) => {
          if (res.data.code === 200) {
            wx.showToast({ title: '创建成功', icon: 'success' })
            this.setRefreshFlag()
            setTimeout(() => {
              wx.navigateBack()
            }, 1500)
          } else {
            wx.showToast({
              title: res.data.message || '创建失败',
              icon: 'none'
            })
          }
        },
        fail: (err) => {
          wx.showToast({ title: '网络请求失败', icon: 'none' })
        },
        complete: () => {
          wx.hideLoading()
        }
      })
    } else {
      // 更新分管理信息
      wx.request({
        url: `${app.globalData.baseUrl}/admin/branch-managers/${id}`,
        method: 'PUT',
        header: {
          'Authorization': `Bearer ${app.globalData.token}`,
          'Content-Type': 'application/json'
        },
        data: {
          brandName,
          phone,
          status
        },
        success: (res) => {
          if (res.data.code === 200) {
            wx.showToast({ title: '保存成功', icon: 'success' })
            this.setRefreshFlag()
            // 更新本地数据
            this.setData({
              'detail.brandName': brandName,
              'detail.phone': phone,
              'detail.status': status
            })
          } else {
            wx.showToast({
              title: res.data.message || '保存失败',
              icon: 'none'
            })
          }
        },
        fail: (err) => {
          wx.showToast({ title: '网络请求失败', icon: 'none' })
        },
        complete: () => {
          wx.hideLoading()
        }
      })
    }
  },

  /**
   * 删除分管理
   */
  handleDelete() {
    wx.showModal({
      title: '确认删除',
      content: '删除后将同时删除该分管理下的所有店铺和司机，此操作不可恢复，确定要删除吗？',
      confirmText: '确认删除',
      confirmColor: '#ff4d4f',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '删除中...' })

          wx.request({
            url: `${app.globalData.baseUrl}/admin/branch-managers/${this.data.id}`,
            method: 'DELETE',
            header: {
              'Authorization': `Bearer ${app.globalData.token}`
            },
            success: (res) => {
              if (res.data.code === 200) {
                wx.showToast({ title: '删除成功', icon: 'success' })
                this.setRefreshFlag()
                setTimeout(() => {
                  wx.navigateBack()
                }, 1500)
              } else {
                wx.showToast({
                  title: res.data.message || '删除失败',
                  icon: 'none'
                })
              }
            },
            fail: (err) => {
              wx.showToast({ title: '网络请求失败', icon: 'none' })
            },
            complete: () => {
              wx.hideLoading()
            }
          })
        }
      }
    })
  },

  /**
   * 设置刷新标记
   */
  setRefreshFlag() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      const prevPage = pages[pages.length - 2]
      if (prevPage.route === 'pages/admin/index/index' ||
        prevPage.route === 'pages/admin/branch-list/branch-list') {
        prevPage.setData({ needRefresh: true })
      }
    }
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {

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
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {

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
