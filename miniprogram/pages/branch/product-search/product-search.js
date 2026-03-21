// pages/branch/product-search/product-search.js
const app = getApp()

Page({
    data: {
        searchType: 'name', // 'name' 或 'code'
        searchKey: '',
        products: [],
        loading: false,
        searched: false
    },

    onLoad(options) {
        if (options.type === 'code') {
            this.setData({ searchType: 'code' })
            // 如果是扫码模式，自动打开扫码
            this.scanCode()
        }
    },

    // 切换搜索类型
    switchSearchType(e) {
        const type = e.currentTarget.dataset.type
        this.setData({
            searchType: type,
            searchKey: '',
            products: [],
            searched: false
        })
    },

    // 输入搜索关键词
    onSearchInput(e) {
        this.setData({ searchKey: e.detail.value })
    },

    // 扫码
    scanCode() {
        wx.scanCode({
            success: (res) => {
                this.setData({ searchKey: res.result })
                this.search()
            },
            fail: () => {
                wx.showToast({ title: '扫码取消', icon: 'none' })
            }
        })
    },

    // 搜索
    search() {
        const { searchType, searchKey } = this.data
        if (!searchKey.trim()) {
            wx.showToast({ title: '请输入搜索内容', icon: 'none' })
            return
        }

        this.setData({ loading: true, searched: true })

        const url = searchType === 'code'
            ? `/branch/product/search-by-code?upcCode=${encodeURIComponent(searchKey)}`
            : `/branch/product/search-by-name?productName=${encodeURIComponent(searchKey)}`

        app.request({ url })
            .then(data => {
                // 处理图片URL
                const products = (data || []).map(item => ({
                    ...item,
                    imageUrl: item.imageUrl ? app.getImageUrl(item.imageUrl) : null
                }))
                this.setData({
                    products: products,
                    loading: false
                })
            })
            .catch(err => {
                wx.showToast({ title: err.message || '搜索失败', icon: 'none' })
                this.setData({ loading: false })
            })
    },

    // 选择商品
    selectProduct(e) {
        const product = e.currentTarget.dataset.product
        // 跳转到商品编辑页面，带上商品信息
        wx.navigateTo({
            url: `/pages/branch/product-edit/product-edit?mode=add&fromMeituan=1&data=${encodeURIComponent(JSON.stringify(product))}`
        })
    },

    // 预览图片
    previewImage(e) {
        const url = e.currentTarget.dataset.url
        if (url) {
            wx.previewImage({
                urls: [url]
            })
        }
    },

    // 图片加载失败处理
    onImageError(e) {
        console.log('图片加载失败', e)
    }
})
