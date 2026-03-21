// pages/admin/statistics/statistics.js
const app = getApp()

Page({

    /**
     * 页面的初始数据
     */
    data: {
        dateRange: '近三个月',
        startDate: '',
        endDate: '',
        overview: {
            branchManagerCount: 0,
            shopCount: 0,
            driverCount: 0,
            productCount: 0,
            totalOrders: 0,
            totalAmount: '0.00'
        },
        productOrderRank: [],
        productSalesRank: [],
        salesTrend: [],
        revenueByBranch: [],
        revenueByShop: [],
        shopTrend: [],
        orderStatusDist: {},
        billStats: {},
        // 按日期整理的送达数据
        deliveryStatsByDate: [],
        loading: false,
        activeTab: 'overview', // overview, product, branch, shop, deliveryStats
        showDatePicker: false
    },

    /**
     * 生命周期函数--监听页面加载
     */
    onLoad(options) {
        // 默认近三个月
        const end = new Date()
        const start = new Date()
        start.setMonth(start.getMonth() - 3)

        this.setData({
            endDate: this.formatDate(end),
            startDate: this.formatDate(start)
        })

        this.loadData()
    },

    /**
     * 格式化日期
     */
    formatDate(date) {
        const year = date.getFullYear()
        const month = String(date.getMonth() + 1).padStart(2, '0')
        const day = String(date.getDate()).padStart(2, '0')
        return `${year}-${month}-${day}`
    },

    /**
     * 加载数据
     */
    loadData() {
        this.setData({ loading: true })
        wx.showLoading({ title: '加载中...' })

        wx.request({
            url: `${app.globalData.baseUrl}/admin/data-center`,
            method: 'GET',
            data: {
                startDate: this.data.startDate,
                endDate: this.data.endDate
            },
            header: {
                'Authorization': `Bearer ${app.globalData.token}`
            },
            success: (res) => {
                if (res.data.code === 200) {
                    const data = res.data.data

                    // 格式化金额
                    if (data.overview) {
                        data.overview.totalAmount = this.formatMoney(data.overview.totalAmount || 0)
                    }

                    // 格式化分管理收入金额
                    if (data.revenueByBranch) {
                        data.revenueByBranch = data.revenueByBranch.map(item => ({
                            ...item,
                            revenue: this.formatMoney(item.revenue || 0)
                        }))
                    }

                    // 格式化店铺收入金额
                    if (data.revenueByShop) {
                        data.revenueByShop = data.revenueByShop.map(item => ({
                            ...item,
                            revenue: this.formatMoney(item.revenue || 0)
                        }))
                    }

                    // 格式化账单金额
                    if (data.billStats) {
                        data.billStats.totalBillAmount = this.formatMoney(data.billStats.totalBillAmount || 0)
                        data.billStats.paidBillAmount = this.formatMoney(data.billStats.paidBillAmount || 0)
                    }

                    this.setData({
                        overview: data.overview || {},
                        productOrderRank: data.productOrderRank || [],
                        productSalesRank: data.productSalesRank || [],
                        salesTrend: data.salesTrend || [],
                        revenueByBranch: data.revenueByBranch || [],
                        revenueByShop: data.revenueByShop || [],
                        shopTrend: data.shopTrend || [],
                        orderStatusDist: data.orderStatusDist || {},
                        billStats: data.billStats || {},
                        deliveryStatsByDate: data.deliveryStatsByDate || []
                    })
                }
            },
            fail: (err) => {
                console.error('加载数据失败:', err)
                wx.showToast({
                    title: '加载失败',
                    icon: 'error'
                })
            },
            complete: () => {
                wx.hideLoading()
                this.setData({ loading: false })
            }
        })
    },

    /**
     * 格式化金额
     */
    formatMoney(amount) {
        return parseFloat(amount).toFixed(2)
    },

    /**
     * 切换标签
     */
    switchTab(e) {
        const tab = e.currentTarget.dataset.tab
        this.setData({ activeTab: tab })
    },

    /**
     * 显示日期选择器
     */
    showDatePicker() {
        this.setData({ showDatePicker: true })
    },

    /**
     * 隐藏日期选择器
     */
    hideDatePicker() {
        this.setData({ showDatePicker: false })
    },

    /**
     * 选择日期范围
     */
    selectDateRange(e) {
        const range = e.currentTarget.dataset.range
        const end = new Date()
        let start = new Date()

        switch (range) {
            case 'week':
                start.setDate(start.getDate() - 7)
                this.setData({ dateRange: '近一周' })
                break
            case 'month':
                start.setMonth(start.getMonth() - 1)
                this.setData({ dateRange: '近一个月' })
                break
            case 'quarter':
                start.setMonth(start.getMonth() - 3)
                this.setData({ dateRange: '近三个月' })
                break
            case 'year':
                start.setFullYear(start.getFullYear() - 1)
                this.setData({ dateRange: '近一年' })
                break
        }

        this.setData({
            startDate: this.formatDate(start),
            endDate: this.formatDate(end),
            showDatePicker: false
        })

        this.loadData()
    },

    /**
     * 页面相关事件处理函数--监听用户下拉动作
     */
    onPullDownRefresh() {
        this.loadData()
        wx.stopPullDownRefresh()
    },

    /**
     * 生命周期函数--监听页面初次渲染完成
     */
    onReady() {

    },

    /**
     * 生命周期函数--监听页面显示
     */
    onShow() {

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
     * 页面上拉触底事件的处理函数
     */
    onReachBottom() {

    },

    /**
     * 用户点击右上角分享
     */
    onShareAppMessage() {

    }
})
