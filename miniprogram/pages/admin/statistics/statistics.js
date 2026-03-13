// pages/admin/statistics/statistics.js
const app = getApp()

Page({

    /**
     * 页面的初始数据
     */
    data: {
        statistics: {
            branchManagerCount: 0,
            shopCount: 0,
            driverCount: 0,
            productCount: 0
        },
        orderStats: {
            total: 0,
            pending: 0,
            completed: 0
        },
        billStats: {
            total: 0,
            totalAmount: '0.00',
            paid: 0
        },
        deliveryStats: {
            picking: 0,
            delivering: 0,
            delivered: 0
        },
        loading: false
    },

    /**
     * 生命周期函数--监听页面加载
     */
    onLoad(options) {
        this.loadData()
    },

    /**
     * 加载数据
     */
    loadData() {
        this.setData({ loading: true })

        wx.showLoading({ title: '加载中...' })

        // 加载基础统计
        this.loadStatistics()

        // 模拟加载其他统计数据（后端可扩展）
        setTimeout(() => {
            wx.hideLoading()
            this.setData({ loading: false })
        }, 500)
    },

    /**
     * 加载统计数据
     */
    loadStatistics() {
        wx.request({
            url: `${app.globalData.baseUrl}/admin/statistics`,
            method: 'GET',
            header: {
                'Authorization': `Bearer ${app.globalData.token}`
            },
            success: (res) => {
                if (res.data.code === 200) {
                    this.setData({
                        statistics: res.data.data
                    })
                }
            },
            fail: (err) => {
                console.error('加载统计数据失败:', err)
            }
        })
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
