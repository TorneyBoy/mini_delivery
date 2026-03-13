// pages/branch/order-monitor/order-monitor.js
const app = getApp()

Page({
  data: {
    tabs: ['全部', '待支付', '待分配', '待拣货', '待送货', '已完成'],
    currentTab: 0,
    orders: [],
    page: 1,
    size: 10,
    hasMore: true,
    loading: false,
    statusMap: {
      'PENDING': '待支付',
      'PENDING_ASSIGN': '待分配',
      'PENDING_PICK': '待拣货',
      'PENDING_DELIVER': '待送货',
      'COMPLETED': '已完成'
    },
    drivers: [],
    showAssignModal: false,
    selectedOrderId: null,
    selectedDriverId: null,
    selectedDriverName: ''
  },

  onLoad(options) {
    this.loadOrders()
    this.loadDrivers()
  },

  onShow() {
    // 每次显示页面时刷新数据
    this.refreshOrders()
  },

  onPullDownRefresh() {
    this.refreshOrders()
    wx.stopPullDownRefresh()
  },

  refreshOrders() {
    this.setData({
      page: 1,
      hasMore: true,
      orders: []
    })
    this.loadOrders()
  },

  loadOrders() {
    if (this.data.loading) return
    this.setData({ loading: true })

    const statusFilter = this.getStatusFilter()

    wx.request({
      url: `${app.globalData.baseUrl}/branch/orders`,
      method: 'GET',
      data: {
        page: this.data.page,
        size: this.data.size,
        status: statusFilter
      },
      header: {
        'Authorization': `Bearer ${app.globalData.token}`
      },
      success: (res) => {
        if (res.data.code === 200) {
          const newList = res.data.data.records || res.data.data.list || []
          this.setData({
            orders: this.data.page === 1 ? newList : [...this.data.orders, ...newList],
            hasMore: newList.length >= this.data.size
          })
        } else {
          wx.showToast({ title: res.data.message || '加载失败', icon: 'none' })
        }
      },
      fail: () => {
        wx.showToast({ title: '网络请求失败', icon: 'none' })
      },
      complete: () => {
        this.setData({ loading: false })
      }
    })
  },

  loadDrivers() {
    wx.request({
      url: `${app.globalData.baseUrl}/branch/drivers`,
      method: 'GET',
      data: { page: 1, size: 100 },
      header: {
        'Authorization': `Bearer ${app.globalData.token}`
      },
      success: (res) => {
        if (res.data.code === 200) {
          const drivers = res.data.data.records || res.data.data.list || []
          this.setData({ drivers: drivers.filter(d => d.status === 1) })
        }
      }
    })
  },

  getStatusFilter() {
    const statusMap = {
      0: '', // 全部
      1: 'PENDING',
      2: 'PENDING_ASSIGN',
      3: 'PENDING_PICK',
      4: 'PENDING_DELIVER',
      5: 'COMPLETED'
    }
    return statusMap[this.data.currentTab] || ''
  },

  onTabChange(e) {
    const index = e.currentTarget.dataset.index
    if (index !== this.data.currentTab) {
      this.setData({ currentTab: index })
      this.refreshOrders()
    }
  },

  loadMore() {
    if (this.data.hasMore && !this.data.loading) {
      this.setData({ page: this.data.page + 1 })
      this.loadOrders()
    }
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/branch/order-detail/order-detail?id=${id}`
    })
  },

  // 显示分配司机弹窗
  showAssignDriver(e) {
    const orderId = e.currentTarget.dataset.id
    this.setData({
      showAssignModal: true,
      selectedOrderId: orderId,
      selectedDriverId: null,
      selectedDriverName: ''
    })
  },

  // 关闭弹窗
  hideAssignModal() {
    this.setData({
      showAssignModal: false,
      selectedOrderId: null,
      selectedDriverId: null,
      selectedDriverName: ''
    })
  },

  // 选择司机
  onDriverChange(e) {
    const index = e.detail.value
    const driver = this.data.drivers[index]
    this.setData({
      selectedDriverId: driver.id,
      selectedDriverName: driver.name
    })
  },

  // 确认分配
  confirmAssign() {
    const { selectedOrderId, selectedDriverId } = this.data
    if (!selectedDriverId) {
      wx.showToast({ title: '请选择司机', icon: 'none' })
      return
    }

    wx.showLoading({ title: '分配中...' })
    wx.request({
      url: `${app.globalData.baseUrl}/branch/orders/${selectedOrderId}/assign`,
      method: 'POST',
      header: {
        'Authorization': `Bearer ${app.globalData.token}`,
        'Content-Type': 'application/json'
      },
      data: { driverId: selectedDriverId },
      success: (res) => {
        if (res.data.code === 200) {
          wx.showToast({ title: '分配成功', icon: 'success' })
          this.hideAssignModal()
          this.refreshOrders()
        } else {
          wx.showToast({ title: res.data.message || '分配失败', icon: 'none' })
        }
      },
      fail: () => {
        wx.showToast({ title: '网络请求失败', icon: 'none' })
      },
      complete: () => {
        wx.hideLoading()
      }
    })
  },

  // 格式化时间
  formatTime(time) {
    if (!time) return ''
    const date = new Date(time)
    return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
  }
})
