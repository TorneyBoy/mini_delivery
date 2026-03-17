// pages/driver/upload-product/upload-product.js
const app = getApp()

Page({
    data: {
        productList: [],        // 商品列表
        filteredProducts: [],   // 筛选后的商品列表
        searchKeyword: '',      // 搜索关键词
        loading: false,
        submitting: false,

        // 当前选中的商品
        currentProduct: null,
        currentImageUrl: '',

        // 弹窗相关
        showImageModal: false
    },

    onLoad() {
        this.loadProducts()
    },

    onShow() {
        // 每次显示页面时刷新列表
        this.loadProducts()
    },

    onPullDownRefresh() {
        this.loadProducts().then(() => {
            wx.stopPullDownRefresh()
        })
    },

    // 加载商品库列表
    loadProducts() {
        this.setData({ loading: true })

        console.log('开始加载商品库列表...')
        console.log('当前token:', app.globalData.token)
        console.log('当前用户信息:', app.globalData.userInfo)

        // 检查登录状态
        if (!app.globalData.token) {
            console.error('未登录，token为空')
            this.setData({ loading: false })
            wx.showToast({ title: '请先登录', icon: 'none' })
            return Promise.reject('未登录')
        }

        return app.request({
            url: '/driver/available-products'
        }).then(data => {
            console.log('商品库API返回数据:', data)
            const products = (data || []).map(item => ({
                ...item,
                hasImage: !!item.imageUrl,
                // 处理图片URL
                imageUrl: item.imageUrl ? app.getImageUrl(item.imageUrl) : null
            }))
            console.log('处理后的商品列表:', products)
            this.setData({
                productList: products,
                filteredProducts: products,
                loading: false
            })
            return products
        }).catch(err => {
            console.error('加载商品列表失败:', err)
            this.setData({
                loading: false,
                productList: [],
                filteredProducts: []
            })
            wx.showToast({
                title: typeof err === 'string' ? err : (err.message || '加载失败'),
                icon: 'none',
                duration: 2000
            })
            return []
        })
    },

    // 搜索输入
    onSearchInput(e) {
        const keyword = e.detail.value.trim().toLowerCase()
        this.setData({ searchKeyword: keyword })

        if (!keyword) {
            this.setData({ filteredProducts: this.data.productList })
            return
        }

        const filtered = this.data.productList.filter(item =>
            item.name && item.name.toLowerCase().includes(keyword)
        )
        this.setData({ filteredProducts: filtered })
    },

    // 清除搜索
    clearSearch() {
        this.setData({
            searchKeyword: '',
            filteredProducts: this.data.productList
        })
    },

    // 点击添加图片按钮
    onAddImage(e) {
        const productId = e.currentTarget.dataset.id
        const product = this.data.productList.find(item => item.id === productId)

        if (!product) return

        this.setData({
            currentProduct: product,
            currentImageUrl: '',
            showImageModal: true
        })
    },

    // 选择图片（拍照或相册）
    chooseImage() {
        wx.showActionSheet({
            itemList: ['拍照', '从相册选择'],
            success: (res) => {
                const sourceType = res.tapIndex === 0 ? ['camera'] : ['album']
                this.doChooseImage(sourceType)
            }
        })
    },

    // 执行选择图片
    doChooseImage(sourceType) {
        wx.chooseMedia({
            count: 1,
            mediaType: ['image'],
            sourceType: sourceType,
            success: (res) => {
                const tempFilePath = res.tempFiles[0].tempFilePath
                this.uploadImage(tempFilePath)
            }
        })
    },

    // 上传图片
    uploadImage(filePath) {
        wx.showLoading({ title: '上传中...' })

        wx.uploadFile({
            url: `${app.globalData.baseUrl}/upload/image`,
            filePath: filePath,
            name: 'file',
            header: {
                'Authorization': `Bearer ${app.globalData.token}`
            },
            success: (res) => {
                const data = JSON.parse(res.data)
                if (data.code === 200) {
                    this.setData({ currentImageUrl: data.data.url })
                    wx.showToast({ title: '上传成功', icon: 'success' })
                } else {
                    wx.showToast({ title: data.message || '上传失败', icon: 'none' })
                }
            },
            fail: () => {
                wx.showToast({ title: '上传失败', icon: 'none' })
            },
            complete: () => {
                wx.hideLoading()
            }
        })
    },

    // 预览当前图片
    previewCurrentImage() {
        if (this.data.currentImageUrl) {
            wx.previewImage({
                urls: [this.data.currentImageUrl]
            })
        }
    },

    // 删除当前图片
    deleteCurrentImage() {
        this.setData({ currentImageUrl: '' })
    },

    // 关闭弹窗
    closeModal() {
        this.setData({
            showImageModal: false,
            currentProduct: null,
            currentImageUrl: ''
        })
    },

    // 阻止冒泡
    stopPropagation() { },

    // 提交图片申请
    submitRequest() {
        if (!this.data.currentProduct) {
            wx.showToast({ title: '请选择商品', icon: 'none' })
            return
        }
        if (!this.data.currentImageUrl) {
            wx.showToast({ title: '请上传商品图片', icon: 'none' })
            return
        }

        this.setData({ submitting: true })

        app.request({
            url: '/driver/product-image-request',
            method: 'POST',
            data: {
                productId: this.data.currentProduct.id,
                productName: this.data.currentProduct.name,
                category: this.data.currentProduct.category,
                imageUrl: this.data.currentImageUrl,
                description: ''
            }
        }).then(() => {
            wx.showToast({ title: '提交成功', icon: 'success' })

            // 更新列表中的商品状态
            const productList = this.data.productList.map(item => {
                if (item.id === this.data.currentProduct.id) {
                    return { ...item, hasImage: true }
                }
                return item
            })

            this.setData({
                productList,
                filteredProducts: this.filterProducts(productList, this.data.searchKeyword),
                showImageModal: false,
                currentProduct: null,
                currentImageUrl: ''
            })
        }).catch(err => {
            wx.showToast({ title: err.message || '提交失败', icon: 'none' })
        }).finally(() => {
            this.setData({ submitting: false })
        })
    },

    // 筛选商品
    filterProducts(products, keyword) {
        if (!keyword) return products
        return products.filter(item =>
            item.name && item.name.toLowerCase().includes(keyword.toLowerCase())
        )
    },

    // 预览商品已有图片
    previewProductImage(e) {
        const url = e.currentTarget.dataset.url
        if (url) {
            wx.previewImage({
                urls: [url]
            })
        }
    }
})
