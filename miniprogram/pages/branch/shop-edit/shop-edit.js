// pages/branch/shop-edit/shop-edit.js
const app = getApp()

Page({

  /**
   * 页面的初始数据
   */
  data: {
    mode: 'add', // add, edit, view
    id: null,
    name: '',
    address: '',
    phone: '',
    password: '',
    showPrice: 1,
    priceType: 1,
    priceTypeIndex: 0,
    status: 1,
    detail: {},
    latitude: null,
    longitude: null,
    priceTypes: [
      { value: 1, label: '老用户价' },
      { value: 2, label: '新用户价' }
    ]
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    if (options.mode === 'add') {
      this.setData({ mode: 'add' })
      wx.setNavigationBarTitle({ title: '添加店铺' })
    } else if (options.id) {
      const mode = options.mode || 'view'
      this.setData({
        mode: mode,
        id: options.id
      })
      wx.setNavigationBarTitle({ title: mode === 'edit' ? '编辑店铺' : '店铺详情' })
      this.loadDetail(options.id)
    }
  },

  /**
   * 加载详情
   */
  loadDetail(id) {
    wx.showLoading({ title: '加载中...' })

    wx.request({
      url: `${app.globalData.baseUrl}/branch/shops/${id}`,
      method: 'GET',
      header: {
        'Authorization': `Bearer ${app.globalData.token}`
      },
      success: (res) => {
        if (res.data.code === 200) {
          const data = res.data.data
          this.setData({
            detail: data,
            name: data.name,
            address: data.address,
            latitude: data.latitude,
            longitude: data.longitude,
            phone: data.phone,
            showPrice: data.showPrice,
            priceType: data.priceType,
            priceTypeIndex: data.priceType === 1 ? 0 : 1,
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
   * 输入事件
   */
  onNameInput(e) {
    this.setData({ name: e.detail.value })
  },

  onAddressInput(e) {
    this.setData({ address: e.detail.value })
  },

  /**
   * 选择地图位置
   */
  chooseLocation() {
    wx.chooseLocation({
      success: (res) => {
        // res.name: 位置名称
        // res.address: 详细地址
        // res.latitude: 纬度
        // res.longitude: 经度
        this.setData({
          address: res.address || res.name,
          latitude: res.latitude,
          longitude: res.longitude
        })
      },
      fail: (err) => {
        // 用户取消选择不提示错误
        if (err.errMsg.indexOf('auth deny') !== -1) {
          wx.showModal({
            title: '提示',
            content: '需要授权位置信息才能使用地图选点功能，请在设置中开启',
            confirmText: '去设置',
            success: (modalRes) => {
              if (modalRes.confirm) {
                wx.openSetting()
              }
            }
          })
        }
      }
    })
  },

  onPhoneInput(e) {
    this.setData({ phone: e.detail.value })
  },

  onPasswordInput(e) {
    this.setData({ password: e.detail.value })
  },

  /**
   * 开关事件
   */
  onShowPriceChange(e) {
    this.setData({ showPrice: e.detail.value ? 1 : 0 })
  },

  onPriceTypeChange(e) {
    const index = parseInt(e.detail.value)
    this.setData({
      priceTypeIndex: index,
      priceType: this.data.priceTypes[index].value
    })
  },

  onStatusChange(e) {
    this.setData({ status: e.detail.value ? 1 : 0 })
  },

  /**
   * 切换到编辑模式
   */
  switchToEdit() {
    this.setData({ mode: 'edit' })
    wx.setNavigationBarTitle({ title: '编辑店铺' })
  },

  /**
   * 提交
   */
  handleSubmit() {
    const { mode, id, name, address, phone, password, showPrice, priceType, status, latitude, longitude } = this.data

    // 验证
    if (!name) {
      wx.showToast({ title: '请输入店铺名称', icon: 'none' })
      return
    }

    if (!address) {
      wx.showToast({ title: '请输入地址', icon: 'none' })
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
      // 创建店铺
      wx.request({
        url: `${app.globalData.baseUrl}/branch/shops`,
        method: 'POST',
        header: {
          'Authorization': `Bearer ${app.globalData.token}`,
          'Content-Type': 'application/json'
        },
        data: {
          name,
          address,
          latitude,
          longitude,
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
      // 更新店铺
      wx.request({
        url: `${app.globalData.baseUrl}/branch/shops/${id}`,
        method: 'PUT',
        header: {
          'Authorization': `Bearer ${app.globalData.token}`,
          'Content-Type': 'application/json'
        },
        data: {
          name,
          address,
          latitude,
          longitude,
          phone,
          showPrice,
          priceType,
          status
        },
        success: (res) => {
          if (res.data.code === 200) {
            wx.showToast({ title: '保存成功', icon: 'success' })
            this.setRefreshFlag()
            this.setData({
              'detail.name': name,
              'detail.address': address,
              'detail.phone': phone,
              'detail.showPrice': showPrice,
              'detail.priceType': priceType,
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
   * 删除店铺
   */
  handleDelete() {
    wx.showModal({
      title: '确认删除',
      content: '删除后该店铺的所有数据将被清除，此操作不可恢复，确定要删除吗？',
      confirmText: '确认删除',
      confirmColor: '#ff4d4f',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '删除中...' })

          wx.request({
            url: `${app.globalData.baseUrl}/branch/shops/${this.data.id}`,
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
      if (prevPage.route === 'pages/branch/shop-list/shop-list' ||
        prevPage.route === 'pages/branch/index/index') {
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
