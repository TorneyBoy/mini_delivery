// pages/branch/product-review/product-review.js
const app = getApp()

Page({
    data: {
        list: [],
        loading: false,
        currentRequest: null,
        showReviewModal: false,
        reviewApproved: true,
        rejectReason: ''
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
            url: '/branch/product-image-requests'
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

    // 打开审核弹窗
    openReviewModal(e) {
        const request = e.currentTarget.dataset.item
        this.setData({
            currentRequest: request,
            showReviewModal: true,
            reviewApproved: true,
            rejectReason: ''
        })
    },

    // 关闭审核弹窗
    closeReviewModal() {
        this.setData({
            showReviewModal: false,
            currentRequest: null
        })
    },

    // 选择通过
    selectApprove() {
        this.setData({ reviewApproved: true })
    },

    // 选择拒绝
    selectReject() {
        this.setData({ reviewApproved: false })
    },

    // 输入拒绝原因
    onRejectReasonInput(e) {
        this.setData({ rejectReason: e.detail.value })
    },

    // 提交审核
    submitReview() {
        const { currentRequest, reviewApproved, rejectReason } = this.data

        if (!reviewApproved && !rejectReason.trim()) {
            wx.showToast({ title: '请输入拒绝原因', icon: 'none' })
            return
        }

        wx.showLoading({ title: '提交中...' })

        app.request({
            url: '/branch/product-image-request/review',
            method: 'POST',
            data: {
                requestId: currentRequest.id,
                approved: reviewApproved,
                rejectReason: reviewApproved ? null : rejectReason
            }
        }).then(() => {
            wx.showToast({ title: '审核成功', icon: 'success' })
            this.closeReviewModal()
            this.loadList()
        }).catch(err => {
            wx.showToast({ title: err.message || '审核失败', icon: 'none' })
        }).finally(() => {
            wx.hideLoading()
        })
    }
})
