// pages/branch/driver-list/driver-list.js
const app = getApp()

Page({
  data: {
    list: [],
    page: 1,
    size: 10,
    hasMore: true,
    loading: false,
    searchKey: '',
    needRefresh: false,
    shareItem: null
  },

  onLoad(options) {
    this.loadList()
  },

  onShow() {
    if (this.data.needRefresh) {
      this.refreshList()
      this.setData({ needRefresh: false })
    }
  },

  refreshList() {
    this.setData({
      page: 1,
      hasMore: true,
      list: []
    })
    this.loadList()
  },

  loadList() {
    if (this.data.loading) return
    this.setData({ loading: true })

    wx.request({
      url: `${app.globalData.baseUrl}/branch/drivers`,
      method: 'GET',
      data: {
        page: this.data.page,
        size: this.data.size
      },
      header: {
        'Authorization': `Bearer ${app.globalData.token}`
      },
      success: (res) => {
        if (res.data.code === 200) {
          const newList = res.data.data.records || res.data.data.list || []
          this.setData({
            list: this.data.page === 1 ? newList : [...this.data.list, ...newList],
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

  loadMore() {
    if (this.data.hasMore && !this.data.loading) {
      this.setData({ page: this.data.page + 1 })
      this.loadList()
    }
  },

  onSearchInput(e) {
    this.setData({ searchKey: e.detail.value })
  },

  handleSearch() {
    const key = this.data.searchKey.trim()
    if (!key) {
      this.refreshList()
      return
    }
    const filteredList = this.data.list.filter(item =>
      (item.name && item.name.includes(key)) ||
      item.phone.includes(key)
    )
    this.setData({ list: filteredList, hasMore: false })
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/branch/driver-edit/driver-edit?id=${id}&mode=view`
    })
  },

  goToEdit(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/branch/driver-edit/driver-edit?id=${id}&mode=edit`
    })
  },

  goToAdd() {
    wx.navigateTo({
      url: '/pages/branch/driver-edit/driver-edit?mode=add'
    })
  },

  /**
   * 分享司机注册链接
   */
  handleShare(e) {
    const item = e.currentTarget.dataset.item
    this.setData({ shareItem: item })
    // 触发分享
  },

  /**
   * 分享给司机
   */
  onShareAppMessage() {
    const item = this.data.shareItem
    const userInfo = app.globalData.userInfo
    const branchManagerId = userInfo ? userInfo.id : null

    if (item && branchManagerId) {
      return {
        title: `邀请您注册成为司机【${item.name}】`,
        path: `/pages/register/register?role=DRIVER&branchManagerId=${branchManagerId}&name=${encodeURIComponent(item.name)}&phone=${item.phone}`,
        success: () => {
          wx.showToast({ title: '分享成功', icon: 'success' })
        }
      }
    }
    return {
      title: '邀请您注册成为司机',
      path: `/pages/register/register?role=DRIVER&branchManagerId=${branchManagerId || ''}`
    }
  },

  onPullDownRefresh() {
    this.refreshList()
    wx.stopPullDownRefresh()
  },

  onReachBottom() {
    this.loadMore()
  }
})
