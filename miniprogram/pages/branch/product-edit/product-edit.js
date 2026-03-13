// pages/branch/product-edit/product-edit.js
const app = getApp()

Page({
  data: {
    mode: 'add',
    id: null,
    name: '',
    category: '',
    oldPrice: '',
    newPrice: '',
    unit: '斤',
    description: '',
    imageUrl: '',
    status: 1,
    detail: {},
    categories: ['水果', '蔬菜', '肉类', '海鲜', '干货', '饮料', '其他'],
    categoryIndex: 0,
    units: ['斤', '公斤', '箱', '件', '个', '盒', '袋'],
    unitIndex: 0
  },

  onLoad(options) {
    if (options.mode === 'add') {
      this.setData({ mode: 'add' })
      wx.setNavigationBarTitle({ title: '添加商品' })
    } else if (options.id) {
      const mode = options.mode || 'view'
      this.setData({ mode: mode, id: options.id })
      wx.setNavigationBarTitle({ title: mode === 'edit' ? '编辑商品' : '商品详情' })
      this.loadDetail(options.id)
    }
  },

  loadDetail(id) {
    wx.showLoading({ title: '加载中...' })
    wx.request({
      url: `${app.globalData.baseUrl}/branch/products/${id}`,
      method: 'GET',
      header: { 'Authorization': `Bearer ${app.globalData.token}` },
      success: (res) => {
        if (res.data.code === 200) {
          const data = res.data.data
          const categoryIndex = this.data.categories.indexOf(data.category)
          const unitIndex = this.data.units.indexOf(data.unit)
          this.setData({
            detail: data,
            name: data.name,
            category: data.category,
            categoryIndex: categoryIndex >= 0 ? categoryIndex : 0,
            oldPrice: data.oldPrice ? String(data.oldPrice) : '',
            newPrice: data.newPrice ? String(data.newPrice) : '',
            unit: data.unit,
            unitIndex: unitIndex >= 0 ? unitIndex : 0,
            description: data.description || '',
            imageUrl: data.imageUrl || '',
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

  onCategoryChange(e) {
    const index = e.detail.value
    this.setData({
      categoryIndex: index,
      category: this.data.categories[index]
    })
  },

  onOldPriceInput(e) { this.setData({ oldPrice: e.detail.value }) },
  onNewPriceInput(e) { this.setData({ newPrice: e.detail.value }) },

  onUnitChange(e) {
    const index = e.detail.value
    this.setData({
      unitIndex: index,
      unit: this.data.units[index]
    })
  },

  onDescriptionInput(e) { this.setData({ description: e.detail.value }) },
  onStatusChange(e) { this.setData({ status: e.detail.value ? 1 : 0 }) },

  chooseImage() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath
        this.uploadImage(tempFilePath)
      }
    })
  },

  uploadImage(filePath) {
    wx.showLoading({ title: '上传中...' })
    wx.uploadFile({
      url: `${app.globalData.baseUrl}/upload/image`,
      filePath: filePath,
      name: 'file',
      header: { 'Authorization': `Bearer ${app.globalData.token}` },
      success: (res) => {
        const data = JSON.parse(res.data)
        if (data.code === 200) {
          this.setData({ imageUrl: data.data.url })
          wx.showToast({ title: '上传成功', icon: 'success' })
        } else {
          wx.showToast({ title: data.message || '上传失败', icon: 'none' })
        }
      },
      fail: () => { wx.showToast({ title: '上传失败', icon: 'none' }) },
      complete: () => { wx.hideLoading() }
    })
  },

  switchToEdit() {
    this.setData({ mode: 'edit' })
    wx.setNavigationBarTitle({ title: '编辑商品' })
  },

  handleSubmit() {
    const { mode, id, name, category, oldPrice, newPrice, unit, description, imageUrl, status } = this.data
    if (!name) { wx.showToast({ title: '请输入商品名称', icon: 'none' }); return }
    if (!category) { wx.showToast({ title: '请选择商品类别', icon: 'none' }); return }
    if (!oldPrice) { wx.showToast({ title: '请输入老用户价格', icon: 'none' }); return }
    if (!newPrice) { wx.showToast({ title: '请输入新用户价格', icon: 'none' }); return }
    if (!unit) { wx.showToast({ title: '请选择单位', icon: 'none' }); return }

    wx.showLoading({ title: '提交中...' })

    const data = {
      name,
      category,
      oldPrice: parseFloat(oldPrice),
      newPrice: parseFloat(newPrice),
      unit,
      description,
      imageUrl
    }

    if (mode === 'add') {
      wx.request({
        url: `${app.globalData.baseUrl}/branch/products`,
        method: 'POST',
        header: { 'Authorization': `Bearer ${app.globalData.token}`, 'Content-Type': 'application/json' },
        data: data,
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
        url: `${app.globalData.baseUrl}/branch/products/${id}`,
        method: 'PUT',
        header: { 'Authorization': `Bearer ${app.globalData.token}`, 'Content-Type': 'application/json' },
        data: data,
        success: (res) => {
          if (res.data.code === 200) {
            wx.showToast({ title: '保存成功', icon: 'success' })
            this.setRefreshFlag()
            // 更新详情数据
            this.setData({
              'detail.name': name,
              'detail.category': category,
              'detail.oldPrice': oldPrice,
              'detail.newPrice': newPrice,
              'detail.unit': unit,
              'detail.description': description,
              'detail.imageUrl': imageUrl
            })
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
      content: '确定要删除该商品吗？删除后无法恢复。',
      success: (res) => {
        if (res.confirm) {
          this.deleteProduct()
        }
      }
    })
  },

  deleteProduct() {
    wx.showLoading({ title: '删除中...' })
    wx.request({
      url: `${app.globalData.baseUrl}/branch/products/${this.data.id}`,
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
  },

  setRefreshFlag() {
    const pages = getCurrentPages()
    const prevPage = pages[pages.length - 2]
    if (prevPage && prevPage.route === 'pages/branch/product-list/product-list') {
      prevPage.setData({ needRefresh: true })
    }
  }
})
