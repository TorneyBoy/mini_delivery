// pages/branch/driver-edit/driver-edit.js
const app = getApp()

Page({
  data: {
    mode: 'add',
    id: null,
    name: '',
    phone: '',
    password: '',
    status: 1,
    detail: {}
  },

  onLoad(options) {
    if (options.mode === 'add') {
      this.setData({ mode: 'add' })
      wx.setNavigationBarTitle({ title: '添加司机' })
    } else if (options.id) {
      const mode = options.mode || 'view'
      this.setData({ mode: mode, id: options.id })
      wx.setNavigationBarTitle({ title: mode === 'edit' ? '编辑司机' : '司机详情' })
      this.loadDetail(options.id)
    }
  },

  loadDetail(id) {
    wx.showLoading({ title: '加载中...' })
    wx.request({
      url: `${app.globalData.baseUrl}/branch/drivers/${id}`,
      method: 'GET',
      header: { 'Authorization': `Bearer ${app.globalData.token}` },
      success: (res) => {
        if (res.data.code === 200) {
          const data = res.data.data
          this.setData({
            detail: data,
            name: data.name,
            phone: data.phone,
            status: data.status
          })
        } else {
          wx.showToast({ title: res.data.message || '加载失败', icon: 'none' })
        }
      },
      fail: () => { wx.showToast({ title: '网络请求失败', icon: 'none' }) },
      complete: () => { wx.hideLoading() }
    })
  },

  onNameInput(e) { this.setData({ name: e.detail.value }) },
  onPhoneInput(e) { this.setData({ phone: e.detail.value }) },
  onPasswordInput(e) { this.setData({ password: e.detail.value }) },
  onStatusChange(e) { this.setData({ status: e.detail.value ? 1 : 0 }) },

  switchToEdit() {
    this.setData({ mode: 'edit' })
    wx.setNavigationBarTitle({ title: '编辑司机' })
  },

  handleSubmit() {
    const { mode, id, name, phone, password, status } = this.data
    if (!name) { wx.showToast({ title: '请输入姓名', icon: 'none' }); return }
    if (!phone) { wx.showToast({ title: '请输入手机号', icon: 'none' }); return }
    if (mode === 'add' && !password) { wx.showToast({ title: '请输入密码', icon: 'none' }); return }

    wx.showLoading({ title: '提交中...' })

    if (mode === 'add') {
      wx.request({
        url: `${app.globalData.baseUrl}/branch/drivers`,
        method: 'POST',
        header: { 'Authorization': `Bearer ${app.globalData.token}`, 'Content-Type': 'application/json' },
        data: { name, phone, password },
        success: (res) => {
          if (res.data.code === 200) {
            wx.showToast({ title: '创建成功', icon: 'success' })
            this.setRefreshFlag()
            setTimeout(() => { wx.navigateBack() }, 1500)
          } else { wx.showToast({ title: res.data.message || '创建失败', icon: 'none' }) }
        },
        fail: () => { wx.showToast({ title: '网络请求失败', icon: 'none' }) },
        complete: () => { wx.hideLoading() }
      })
    } else {
      wx.request({
        url: `${app.globalData.baseUrl}/branch/drivers/${id}`,
        method: 'PUT',
        header: { 'Authorization': `Bearer ${app.globalData.token}`, 'Content-Type': 'application/json' },
        data: { name, phone, status },
        success: (res) => {
          if (res.data.code === 200) {
            wx.showToast({ title: '保存成功', icon: 'success' })
            this.setRefreshFlag()
            this.setData({ 'detail.name': name, 'detail.phone': phone, 'detail.status': status })
          } else { wx.showToast({ title: res.data.message || '保存失败', icon: 'none' }) }
        },
        fail: () => { wx.showToast({ title: '网络请求失败', icon: 'none' }) },
        complete: () => { wx.hideLoading() }
      })
    }
  },

  handleDelete() {
    wx.showModal({
      title: '确认删除',
      content: '删除后该司机的所有数据将被清除，此操作不可恢复，确定要删除吗？',
      confirmText: '确认删除',
      confirmColor: '#ff4d4f',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '删除中...' })
          wx.request({
            url: `${app.globalData.baseUrl}/branch/drivers/${this.data.id}`,
            method: 'DELETE',
            header: { 'Authorization': `Bearer ${app.globalData.token}` },
            success: (res) => {
              if (res.data.code === 200) {
                wx.showToast({ title: '删除成功', icon: 'success' })
                this.setRefreshFlag()
                setTimeout(() => { wx.navigateBack() }, 1500)
              } else { wx.showToast({ title: res.data.message || '删除失败', icon: 'none' }) }
            },
            fail: () => { wx.showToast({ title: '网络请求失败', icon: 'none' }) },
            complete: () => { wx.hideLoading() }
          })
        }
      }
    })
  },

  setRefreshFlag() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      const prevPage = pages[pages.length - 2]
      if (prevPage.route === 'pages/branch/driver-list/driver-list' || prevPage.route === 'pages/branch/index/index') {
        prevPage.setData({ needRefresh: true })
      }
    }
  }
})
