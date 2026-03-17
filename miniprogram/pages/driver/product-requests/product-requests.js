// pages/driver/product-requests/product-requests.js
const app = getApp()

Page({
    data: {
        list: [],
        loading: false
    },

    onLoad() {
        this.loadList()
    },

    onShow() {
        this.loadList()
    },

    onPullDownRefresh() {
        this.loadList().then(() => {
            wx.stopPullDownRefresh()
        })
    },

    loadList() {
        this.setData({ loading: true })

        return app.request({
            url: '/driver/product-image-requests'
        }).then(data => {
            // 处理图片URL
            const list = (data || []).map(item => ({
                ...item,
                imageUrl: item.imageUrl ? app.getImageUrl(item.imageUrl) : null
            }))
            this.setData({
                list: list,
                loading: false
            })
        }).catch(err => {
            console.error('加载列表失败', err)
            this.setData({ loading: false })
        })
    },

    // 预览图片
    previewImage(e) {
        const url = e.currentTarget.dataset.url
        wx.previewImage({
            urls: [url]
        })
    },

    // 跳转到上传页面
    goUpload() {
        wx.navigateTo({
            url: '/pages/driver/upload-product/upload-product'
        })
    },

    // 获取状态样式类
    getStatusClass(status) {
        switch (status) {
            case 0: return 'status-pending'
            case 1: return 'status-approved'
            case 2: return 'status-rejected'
            default: return ''
        }
    }
})
