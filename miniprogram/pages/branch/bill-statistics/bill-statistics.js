// pages/branch/bill-statistics/bill-statistics.js
const app = getApp();

Page({
    data: {
        activeTab: 'ungenerated', // ungenerated | history
        ungeneratedShops: [],
        historyShops: [],
        loading: false,
        currentShopBills: [],
        currentShop: null,
        showShopBills: false,
        showBillDetail: false,
        currentBill: null,
        startDate: '',
        endDate: ''
    },

    onLoad() {
        // 设置默认日期范围
        const today = this.formatDate(new Date());
        const threeMonthsAgo = this.formatDate(new Date(Date.now() - 90 * 24 * 60 * 60 * 1000));
        this.setData({
            endDate: today,
            startDate: threeMonthsAgo
        });
        this.loadData();
    },

    onShow() {
        this.loadData();
    },

    // 格式化日期
    formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    },

    // 加载数据
    loadData() {
        if (this.data.activeTab === 'ungenerated') {
            this.loadUnGeneratedShops();
        } else {
            this.loadHistoryShops();
        }
    },

    // 切换Tab
    switchTab(e) {
        const tab = e.currentTarget.dataset.tab;
        this.setData({ activeTab: tab });
        this.loadData();
    },

    // 加载今日未生成账单的店铺
    loadUnGeneratedShops() {
        this.setData({ loading: true });

        app.request({
            url: '/branch/bills/ungenerated-shops',
            method: 'GET'
        })
            .then(data => {
                this.setData({
                    ungeneratedShops: data || [],
                    loading: false
                });
            })
            .catch(err => {
                this.setData({ loading: false });
                wx.showToast({ title: err.message || '加载失败', icon: 'none' });
            });
    },

    // 加载历史账单
    loadHistoryShops() {
        this.setData({ loading: true });

        app.request({
            url: '/branch/bills/history',
            method: 'GET',
            data: {
                startDate: this.data.startDate,
                endDate: this.data.endDate
            }
        })
            .then(data => {
                this.setData({
                    historyShops: data || [],
                    loading: false
                });
            })
            .catch(err => {
                this.setData({ loading: false });
                wx.showToast({ title: err.message || '加载失败', icon: 'none' });
            });
    },

    // 日期选择
    onStartDateChange(e) {
        this.setData({ startDate: e.detail.value });
    },

    onEndDateChange(e) {
        this.setData({ endDate: e.detail.value });
    },

    // 筛选历史账单
    filterHistory() {
        this.loadHistoryShops();
    },

    // 手动生成账单
    generateBill(e) {
        const shop = e.currentTarget.dataset.shop;

        wx.showModal({
            title: '确认生成账单',
            content: `确定为店铺"${shop.name}"生成今日账单吗？`,
            success: (res) => {
                if (res.confirm) {
                    this.doGenerateBill(shop);
                }
            }
        });
    },

    doGenerateBill(shop) {
        wx.showLoading({ title: '生成中...' });

        app.request({
            url: '/branch/bills/generate',
            method: 'POST',
            data: { shopId: shop.id }
        })
            .then(data => {
                wx.hideLoading();
                wx.showModal({
                    title: '生成成功',
                    content: `账单号: ${data.billNo}\n金额: ¥${data.totalAmount}\n订单数: ${data.orderCount}`,
                    showCancel: false,
                    success: () => {
                        this.loadUnGeneratedShops();
                    }
                });
            })
            .catch(err => {
                wx.hideLoading();
                wx.showToast({ title: err.message || '生成失败', icon: 'none' });
            });
    },

    // 查看店铺历史账单
    viewShopBills(e) {
        const shop = e.currentTarget.dataset.shop;

        this.setData({
            currentShop: shop,
            showShopBills: true,
            loading: true
        });

        app.request({
            url: `/branch/bills/shop/${shop.shopId}`,
            method: 'GET'
        })
            .then(data => {
                this.setData({
                    currentShopBills: data || [],
                    loading: false
                });
            })
            .catch(err => {
                this.setData({ loading: false });
                wx.showToast({ title: err.message || '加载失败', icon: 'none' });
            });
    },

    // 关闭店铺账单弹窗
    closeShopBills() {
        this.setData({
            showShopBills: false,
            currentShop: null,
            currentShopBills: []
        });
    },

    // 查看账单详情
    viewBillDetail(e) {
        const bill = e.currentTarget.dataset.bill;

        this.setData({
            showBillDetail: true,
            loading: true
        });

        app.request({
            url: `/branch/bills/${bill.id}`,
            method: 'GET'
        })
            .then(data => {
                this.setData({
                    currentBill: data,
                    loading: false
                });
            })
            .catch(err => {
                this.setData({ loading: false });
                wx.showToast({ title: err.message || '加载失败', icon: 'none' });
            });
    },

    // 关闭账单详情弹窗
    closeBillDetail() {
        this.setData({
            showBillDetail: false,
            currentBill: null
        });
    },

    // 发送账单通知
    sendBillNotice(e) {
        const bill = e.currentTarget.dataset.bill;

        if (bill.sendStatus === 1) {
            wx.showToast({ title: '账单已发送', icon: 'none' });
            return;
        }

        wx.showModal({
            title: '确认发送',
            content: `确定发送账单通知给店铺吗？`,
            success: (res) => {
                if (res.confirm) {
                    this.doSendBillNotice(bill);
                }
            }
        });
    },

    doSendBillNotice(bill) {
        wx.showLoading({ title: '发送中...' });

        app.request({
            url: `/branch/bills/${bill.id}/send`,
            method: 'POST'
        })
            .then(() => {
                wx.hideLoading();
                wx.showToast({ title: '发送成功', icon: 'success' });
                // 刷新数据
                if (this.data.currentShop) {
                    this.viewShopBills({ currentTarget: { dataset: { shop: this.data.currentShop } } });
                }
            })
            .catch(err => {
                wx.hideLoading();
                wx.showToast({ title: err.message || '发送失败', icon: 'none' });
            });
    },

    // 下载账单
    downloadBill() {
        const bill = this.data.currentBill;
        if (!bill) return;

        // 构建下载内容
        let content = `账单号: ${bill.billNo}\n`;
        content += `店铺: ${bill.shopName}\n`;
        content += `账单日期: ${bill.billDate}\n`;
        content += `总金额: ¥${bill.totalAmount}\n`;
        content += `状态: ${bill.statusText}\n`;
        content += `\n订单明细:\n`;
        content += `----------------------------------------\n`;

        bill.orders.forEach((order, index) => {
            content += `\n订单${index + 1}: ${order.orderNo}\n`;
            content += `收货日期: ${order.deliveryDate || '-'}\n`;
            content += `收货时间: ${order.receivedAt || '-'}\n`;
            content += `订单金额: ¥${order.totalAmount}\n`;
            content += `商品明细:\n`;

            order.items.forEach(item => {
                content += `  - ${item.productName} x ${item.quantity} = ¥${item.amount}\n`;
            });
        });

        content += `\n----------------------------------------\n`;
        content += `生成时间: ${bill.createdAt}\n`;

        // 复制到剪贴板
        wx.setClipboardData({
            data: content,
            success: () => {
                wx.showToast({ title: '已复制到剪贴板', icon: 'success' });
            }
        });
    },

    // 下拉刷新
    onPullDownRefresh() {
        this.loadData();
        wx.stopPullDownRefresh();
    },

    // 格式化金额
    formatAmount(amount) {
        return amount ? Number(amount).toFixed(2) : '0.00';
    }
});