// pages/branch/data-center/data-center.js
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
            shopCount: 0,
            driverCount: 0,
            productCount: 0,
            totalOrders: 0,
            completedOrders: 0,
            totalAmount: '0.00',
            completionRate: '0.00'
        },
        salesTrend: [],
        productOrderRank: [],
        productSalesRank: [],
        revenueByShop: [],
        orderStatusDist: {},
        billStats: {},
        // 门店商品统计
        shopProductStats: [],
        // 按送达日期整理的下单数据（预约订单）
        orderStatsByDeliveryDate: [],
        // 按日期整理的送达数据
        deliveryStatsByDate: [],
        loading: false,
        activeTab: 'overview', // overview, product, shop, shopProduct, orderStats, deliveryStats
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

        // 并行加载两个接口
        Promise.all([
            this.loadMainData(),
            this.loadShopProductStats()
        ]).then(() => {
            wx.hideLoading()
            this.setData({ loading: false })
        }).catch(() => {
            wx.hideLoading()
            this.setData({ loading: false })
        })
    },

    /**
     * 加载主要数据
     */
    loadMainData() {
        return new Promise((resolve, reject) => {
            wx.request({
                url: `${app.globalData.baseUrl}/branch/data-center`,
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
                        console.log('数据中心返回数据:', data)
                        console.log('预约订单数据:', data.orderStatsByDeliveryDate)
                        console.log('送达数据:', data.deliveryStatsByDate)

                        // 格式化金额
                        if (data.overview) {
                            data.overview.totalAmount = this.formatMoney(data.overview.totalAmount || 0)
                            data.overview.completionRate = this.formatRate(data.overview.completionRate || 0)
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
                            salesTrend: data.salesTrend || [],
                            productOrderRank: data.productOrderRank || [],
                            productSalesRank: data.productSalesRank || [],
                            revenueByShop: data.revenueByShop || [],
                            orderStatusDist: data.orderStatusDist || {},
                            billStats: data.billStats || {},
                            orderStatsByDeliveryDate: data.orderStatsByDeliveryDate || [],
                            deliveryStatsByDate: data.deliveryStatsByDate || []
                        })
                        resolve()
                    } else {
                        reject()
                    }
                },
                fail: (err) => {
                    console.error('加载数据失败:', err)
                    wx.showToast({
                        title: '加载失败',
                        icon: 'error'
                    })
                    reject()
                }
            })
        })
    },

    /**
     * 加载门店商品统计
     */
    loadShopProductStats() {
        return new Promise((resolve, reject) => {
            wx.request({
                url: `${app.globalData.baseUrl}/branch/shop-product-statistics`,
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
                        // 处理数据，为每个商品添加格式化的完成率
                        const shopProductStats = (res.data.data || []).map(shop => {
                            if (shop.products && shop.products.length > 0) {
                                shop.products = shop.products.map(product => {
                                    // 计算并格式化完成率
                                    if (product.orderQuantity > 0) {
                                        product.completionRate = (product.completedQuantity / product.orderQuantity * 100).toFixed(1)
                                    } else {
                                        product.completionRate = '0.0'
                                    }
                                    return product
                                })
                            }
                            return shop
                        })
                        this.setData({
                            shopProductStats: shopProductStats
                        })
                    }
                    resolve()
                },
                fail: (err) => {
                    console.error('加载门店商品统计失败:', err)
                    resolve() // 不阻塞其他数据加载
                }
            })
        })
    },

    /**
     * 格式化金额
     */
    formatMoney(amount) {
        return parseFloat(amount).toFixed(2)
    },

    /**
     * 格式化比率
     */
    formatRate(rate) {
        return parseFloat(rate).toFixed(2)
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
