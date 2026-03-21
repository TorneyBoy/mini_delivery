// pages/admin/branch-list/branch-list.js
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
    // 检查是否需要刷新
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

    console.log('=== 开始加载分管理列表 ===')
    console.log('请求URL:', `${app.globalData.baseUrl}/admin/branch-managers`)
    console.log('Token:', app.globalData.token)

    wx.request({
      url: `${app.globalData.baseUrl}/admin/branch-managers`,
      method: 'GET',
      data: {
        page: this.data.page,
        size: this.data.size
      },
      header: {
        'Authorization': `Bearer ${app.globalData.token}`
      },
      success: (res) => {
        console.log('=== 列表请求成功 ===')
        console.log('响应状态码:', res.statusCode)
        console.log('响应数据:', JSON.stringify(res.data, null, 2))

        if (res.data.code === 200) {
          // 后端返回的是 records 字段，不是 list
          const newList = res.data.data.records || res.data.data.list || []
          console.log('获取到的列表数据:', newList)
          console.log('列表长度:', newList.length)

          this.setData({
            list: this.data.page === 1 ? newList : [...this.data.list, ...newList],
            hasMore: newList.length >= this.data.size
          })

          console.log('设置后的页面数据:', this.data.list)
        } else {
          console.error('请求返回错误:', res.data.message)
          wx.showToast({
            title: res.data.message || '加载失败',
            icon: 'none'
          })
        }
      },
      fail: (err) => {
        console.error('=== 请求失败 ===')
        console.error('错误信息:', err)
        wx.showToast({
          title: '网络请求失败',
          icon: 'none'
        })
      },
      complete: () => {
        this.setData({ loading: false })
        console.log('=== 请求完成 ===')
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

    // 搜索时过滤列表（支持品牌名称和手机号）
    const filteredList = this.data.list.filter(item =>
      (item.brandName && item.brandName.includes(key)) ||
      item.phone.includes(key)
    )
    this.setData({
      list: filteredList,
      hasMore: false
    })
  },

  /**
   * 跳转到设置页面
   */
  goToSetting(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/admin/branch-detail/branch-detail?id=${id}&mode=setting`
    })
  },

  /**
   * 跳转到新增
   */
  goToAddBranch() {
    wx.navigateTo({
      url: '/pages/admin/branch-detail/branch-detail?mode=add'
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
