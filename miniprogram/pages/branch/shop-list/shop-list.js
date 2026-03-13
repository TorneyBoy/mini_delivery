// pages/branch/shop-list/shop-list.js
const app = getApp()

Page({

  /**
   * 页面的初始数据
   */
  data: {
    list: [],
    page: 1,
    size: 10,
    hasMore: true,
    loading: false,
    searchKey: '',
    needRefresh: false
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    this.loadList()
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    if (this.data.needRefresh) {
      this.refreshList()
      this.setData({ needRefresh: false })
    }
  },

  /**
   * 刷新列表
   */
  refreshList() {
    this.setData({
      page: 1,
      hasMore: true,
      list: []
    })
    this.loadList()
  },

  /**
   * 加载列表
   */
  loadList() {
    if (this.data.loading) return

    this.setData({ loading: true })

    wx.request({
      url: `${app.globalData.baseUrl}/branch/shops`,
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
        this.setData({ loading: false })
      }
    })
  },

  /**
   * 加载更多
   */
  loadMore() {
    if (this.data.hasMore && !this.data.loading) {
      this.setData({
        page: this.data.page + 1
      })
      this.loadList()
    }
  },

  /**
   * 搜索输入
   */
  onSearchInput(e) {
    this.setData({
      searchKey: e.detail.value
    })
  },

  /**
   * 搜索
   */
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
    this.setData({
      list: filteredList,
      hasMore: false
    })
  },

  /**
   * 跳转到详情
   */
  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/branch/shop-edit/shop-edit?id=${id}&mode=view`
    })
  },

  /**
   * 跳转到编辑
   */
  goToEdit(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/branch/shop-edit/shop-edit?id=${id}&mode=edit`
    })
  },

  /**
   * 跳转到添加
   */
  goToAdd() {
    wx.navigateTo({
      url: '/pages/branch/shop-edit/shop-edit?mode=add'
    })
  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {
    this.refreshList()
    wx.stopPullDownRefresh()
  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {
    this.loadMore()
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
   * 用户点击右上角分享
   */
  onShareAppMessage() {

  }
})
