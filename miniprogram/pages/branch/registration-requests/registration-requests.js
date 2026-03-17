// pages/branch/registration-requests/registration-requests.js
const app = getApp()

Page({
    data: {
        shops: [],
        drivers: [],
        totalPending: 0,
        loading: false,
        activeTab: 'all' // all, shop, driver
    },

    onLoad(options) {
        this.loadData()
    },

    onShow() {
        this.loadData()
    },

    /**
     * 加载注册申请数据
     */
    loadData() {
        this.setData({ loading: true })

        wx.request({
            url: `${app.globalData.baseUrl}/branch/registration-requests`,
            method: 'GET',
            header: {
                'Authorization': `Bearer ${app.globalData.token}`
            },
            success: (res) => {
                if (res.data.code === 200) {
                    this.setData({
                        shops: res.data.data.shops || [],
                        drivers: res.data.data.drivers || [],
                        totalPending: res.data.data.totalPending || 0
                    })
                } else {
                    wx.showToast({
                        title: res.data.message || '加载失败',
                        icon: 'none'
                    })
                }
            },
            fail: (err) => {
                console.error('加载注册申请失败:', err)
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
     * 切换标签
     */
    switchTab(e) {
        const tab = e.currentTarget.dataset.tab
        this.setData({ activeTab: tab })
    },

    /**
     * 审核店铺注册
     */
    reviewShop(e) {
        const { id, action } = e.currentTarget.dataset
        const actionText = action === 'approve' ? '通过' : '拒绝'

        wx.showModal({
            title: '确认操作',
            content: `确定要${actionText}该店铺的注册申请吗？`,
            success: (res) => {
                if (res.confirm) {
                    this.submitShopReview(id, action === 'approve')
                }
            }
        })
    },

    /**
     * 提交店铺审核
     */
    submitShopReview(id, approved) {
        wx.request({
            url: `${app.globalData.baseUrl}/branch/shop/${id}/review`,
            method: 'POST',
            header: {
                'Authorization': `Bearer ${app.globalData.token}`,
                'Content-Type': 'application/json'
            },
            data: { approved },
            success: (res) => {
                if (res.data.code === 200) {
                    wx.showToast({
                        title: approved ? '已通过' : '已拒绝',
                        icon: 'success'
                    })
                    this.loadData()
                } else {
                    wx.showToast({
                        title: res.data.message || '操作失败',
                        icon: 'none'
                    })
                }
            },
            fail: (err) => {
                wx.showToast({
                    title: '网络请求失败',
                    icon: 'none'
                })
            }
        })
    },

    /**
     * 审核司机注册
     */
    reviewDriver(e) {
        const { id, action } = e.currentTarget.dataset
        const actionText = action === 'approve' ? '通过' : '拒绝'

        wx.showModal({
            title: '确认操作',
            content: `确定要${actionText}该司机的注册申请吗？`,
            success: (res) => {
                if (res.confirm) {
                    this.submitDriverReview(id, action === 'approve')
                }
            }
        })
    },

    /**
     * 提交司机审核
     */
    submitDriverReview(id, approved) {
        wx.request({
            url: `${app.globalData.baseUrl}/branch/driver/${id}/review`,
            method: 'POST',
            header: {
                'Authorization': `Bearer ${app.globalData.token}`,
                'Content-Type': 'application/json'
            },
            data: { approved },
            success: (res) => {
                if (res.data.code === 200) {
                    wx.showToast({
                        title: approved ? '已通过' : '已拒绝',
                        icon: 'success'
                    })
                    this.loadData()
                } else {
                    wx.showToast({
                        title: res.data.message || '操作失败',
                        icon: 'none'
                    })
                }
            },
            fail: (err) => {
                wx.showToast({
                    title: '网络请求失败',
                    icon: 'none'
                })
            }
        })
    },

    /**
     * 格式化时间
     */
    formatTime(dateStr) {
        if (!dateStr) return ''
        const date = new Date(dateStr)
        const month = (date.getMonth() + 1).toString().padStart(2, '0')
        const day = date.getDate().toString().padStart(2, '0')
        const hour = date.getHours().toString().padStart(2, '0')
        const minute = date.getMinutes().toString().padStart(2, '0')
        return `${month}-${day} ${hour}:${minute}`
    },

    /**
     * 下拉刷新
     */
    onPullDownRefresh() {
        this.loadData()
        wx.stopPullDownRefresh()
    }
})
