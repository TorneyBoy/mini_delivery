// pages/shop/order-edit/order-edit.js
const app = getApp();

Page({
    data: {
        orderId: null,
        order: null,
        orderItems: [],
        totalAmount: '0.00',
        deliveryDate: '',
        minDate: '',
        maxDate: '',
        dateRange: [],
        loading: false,
        submitting: false,
        products: [], // 可选商品列表
        showAddProduct: false,
        categories: [],
        currentCategory: '',
        categoryProducts: [],
        canModify: true,
        modifyDeadline: ''
    },

    onLoad(options) {
        const orderId = options.id;
        if (orderId) {
            this.setData({ orderId });
            this.loadOrderDetail(orderId);
            this.loadProducts();
            this.loadCategories();
        }
    },

    // 加载订单详情
    loadOrderDetail(id) {
        this.setData({ loading: true });

        app.request({ url: `/shop/orders/${id}` })
            .then(data => {
                // 检查是否可以修改
                const canModify = this.checkCanModify(data);

                if (!canModify) {
                    wx.showModal({
                        title: '提示',
                        content: '该订单已超过修改时限，无法修改',
                        showCancel: false,
                        success: () => {
                            wx.navigateBack();
                        }
                    });
                    return;
                }

                // 计算可选日期范围
                const now = new Date();
                const hour = now.getHours();
                const today = new Date(now);
                const todayStr = today.toISOString().split('T')[0];
                const deliveryDate = data.deliveryDate;

                // 生成日期范围
                const dateRange = [];

                // 如果是今日订单，凌晨2点前可选今天
                if (deliveryDate === todayStr && hour < 2) {
                    // 可选今天和未来7天
                    for (let i = 0; i <= 7; i++) {
                        const date = new Date(now);
                        date.setDate(date.getDate() + i);
                        dateRange.push({
                            value: date.toISOString().split('T')[0],
                            label: this.formatDateLabel(date)
                        });
                    }
                } else if (deliveryDate > todayStr) {
                    // 预定订单：可选今天（如果凌晨2点前）或未来日期
                    let startDay = 0;
                    if (hour >= 2) {
                        startDay = 1;
                    }
                    for (let i = startDay; i <= 7; i++) {
                        const date = new Date(now);
                        date.setDate(date.getDate() + i);
                        dateRange.push({
                            value: date.toISOString().split('T')[0],
                            label: this.formatDateLabel(date)
                        });
                    }
                }

                // 找到当前日期在范围中的索引
                let dateIndex = dateRange.findIndex(d => d.value === deliveryDate);
                if (dateIndex === -1 && dateRange.length > 0) {
                    dateIndex = 0;
                }

                // 转换订单商品格式
                const orderItems = data.items.map(item => ({
                    productId: item.productId,
                    productName: item.productName,
                    quantity: parseFloat(item.quantity),
                    unitPrice: parseFloat(item.unitPrice),
                    subtotal: parseFloat(item.subtotal),
                    unit: item.unit || '斤'
                }));

                // 计算总金额
                let totalAmount = 0;
                orderItems.forEach(item => {
                    totalAmount += item.quantity * item.unitPrice;
                });

                // 计算修改截止时间
                let modifyDeadline = '';
                if (deliveryDate === todayStr) {
                    modifyDeadline = '今日凌晨2:00前可修改';
                } else if (deliveryDate > todayStr) {
                    modifyDeadline = '预定订单可随时修改';
                }

                this.setData({
                    order: data,
                    orderItems,
                    totalAmount: totalAmount.toFixed(2),
                    deliveryDate,
                    dateRange,
                    dateIndex: dateIndex >= 0 ? dateIndex : 0,
                    loading: false,
                    canModify,
                    modifyDeadline
                });
            })
            .catch(err => {
                this.setData({ loading: false });
                wx.showToast({ title: err.message || '加载失败', icon: 'none' });
            });
    },

    // 检查是否可以修改订单
    checkCanModify(order) {
        const now = new Date();
        const today = now.toISOString().split('T')[0];
        const hour = now.getHours();
        const deliveryDate = order.deliveryDate;

        // 预定订单（收货日期在今天之后）：可修改
        if (deliveryDate > today) {
            return true;
        }

        // 今日订单：凌晨2点前可修改
        if (deliveryDate === today && hour < 2) {
            return true;
        }

        return false;
    },

    // 格式化日期标签
    formatDateLabel(date) {
        const today = new Date();
        const tomorrow = new Date(today);
        tomorrow.setDate(tomorrow.getDate() + 1);

        const dateStr = date.toISOString().split('T')[0];
        const todayStr = today.toISOString().split('T')[0];
        const tomorrowStr = tomorrow.toISOString().split('T')[0];

        if (dateStr === todayStr) {
            return '今天';
        } else if (dateStr === tomorrowStr) {
            return '明天';
        } else {
            const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
            return `${date.getMonth() + 1}月${date.getDate()}日 ${weekDays[date.getDay()]}`;
        }
    },

    // 加载可选商品列表
    loadProducts() {
        app.request({
            url: '/shop/products',
            data: { page: 1, size: 100 }
        })
            .then(data => {
                const products = (data.records || []).map(p => ({
                    ...p,
                    displayPrice: p.newPrice || p.oldPrice
                }));
                this.setData({ products });
            })
            .catch(err => {
                console.error('加载商品失败', err);
            });
    },

    // 加载分类
    loadCategories() {
        app.request({ url: '/shop/categories' })
            .then(data => {
                this.setData({ categories: data || [] });
            })
            .catch(err => {
                console.error('加载分类失败', err);
            });
    },

    // 选择日期
    onDateChange(e) {
        const index = e.detail.value;
        this.setData({
            deliveryDate: this.data.dateRange[index].value,
            dateIndex: index
        });
    },

    // 修改商品数量
    onQuantityChange(e) {
        const { index, type } = e.currentTarget.dataset;
        const orderItems = [...this.data.orderItems];
        const item = orderItems[index];

        if (type === 'plus') {
            item.quantity = parseFloat((item.quantity + 1).toFixed(2));
        } else if (type === 'minus' && item.quantity > 0) {
            item.quantity = parseFloat((item.quantity - 1).toFixed(2));
        }

        item.subtotal = parseFloat((item.quantity * item.unitPrice).toFixed(2));

        // 重新计算总价
        let totalAmount = 0;
        orderItems.forEach(item => {
            totalAmount += item.quantity * item.unitPrice;
        });

        this.setData({
            orderItems,
            totalAmount: totalAmount.toFixed(2)
        });
    },

    // 直接输入数量
    onInputQuantity(e) {
        const index = e.currentTarget.dataset.index;
        const value = parseFloat(e.detail.value) || 0;
        const orderItems = [...this.data.orderItems];
        const item = orderItems[index];

        item.quantity = parseFloat(value.toFixed(2));
        item.subtotal = parseFloat((item.quantity * item.unitPrice).toFixed(2));

        // 重新计算总价
        let totalAmount = 0;
        orderItems.forEach(item => {
            totalAmount += item.quantity * item.unitPrice;
        });

        this.setData({
            orderItems,
            totalAmount: totalAmount.toFixed(2)
        });
    },

    // 删除商品
    onRemoveItem(e) {
        const index = e.currentTarget.dataset.index;
        const orderItems = [...this.data.orderItems];
        orderItems.splice(index, 1);

        // 重新计算总价
        let totalAmount = 0;
        orderItems.forEach(item => {
            totalAmount += item.quantity * item.unitPrice;
        });

        this.setData({
            orderItems,
            totalAmount: totalAmount.toFixed(2)
        });

        if (orderItems.length === 0) {
            wx.showToast({ title: '请至少保留一件商品', icon: 'none' });
        }
    },

    // 显示添加商品弹窗
    showAddProductModal() {
        const categoryProducts = this.data.currentCategory
            ? this.data.products.filter(p => p.category === this.data.currentCategory)
            : this.data.products;

        this.setData({
            showAddProduct: true,
            categoryProducts
        });
    },

    // 隐藏添加商品弹窗
    hideAddProductModal() {
        this.setData({ showAddProduct: false });
    },

    // 切换分类
    onCategoryChange(e) {
        const category = e.currentTarget.dataset.category;
        const categoryProducts = category
            ? this.data.products.filter(p => p.category === category)
            : this.data.products;

        this.setData({
            currentCategory: category,
            categoryProducts
        });
    },

    // 添加商品到订单
    onAddProduct(e) {
        const productId = e.currentTarget.dataset.id;
        const product = this.data.products.find(p => p.id === productId);

        if (!product) return;

        // 检查是否已存在
        const existingIndex = this.data.orderItems.findIndex(item => item.productId === productId);

        if (existingIndex >= 0) {
            // 已存在，增加数量
            const orderItems = [...this.data.orderItems];
            orderItems[existingIndex].quantity = parseFloat((orderItems[existingIndex].quantity + 1).toFixed(2));
            orderItems[existingIndex].subtotal = parseFloat((orderItems[existingIndex].quantity * orderItems[existingIndex].unitPrice).toFixed(2));

            let totalAmount = 0;
            orderItems.forEach(item => {
                totalAmount += item.quantity * item.unitPrice;
            });

            this.setData({
                orderItems,
                totalAmount: totalAmount.toFixed(2)
            });

            wx.showToast({ title: '已增加数量', icon: 'success' });
        } else {
            // 不存在，添加新商品
            const newItem = {
                productId: product.id,
                productName: product.name,
                quantity: 1,
                unitPrice: parseFloat(product.displayPrice || product.newPrice),
                subtotal: parseFloat(product.displayPrice || product.newPrice),
                unit: product.unit || '斤'
            };

            const orderItems = [...this.data.orderItems, newItem];

            let totalAmount = 0;
            orderItems.forEach(item => {
                totalAmount += item.quantity * item.unitPrice;
            });

            this.setData({
                orderItems,
                totalAmount: totalAmount.toFixed(2)
            });

            wx.showToast({ title: '已添加商品', icon: 'success' });
        }
    },

    // 提交修改
    onSubmit() {
        if (this.data.orderItems.length === 0) {
            wx.showToast({ title: '请至少选择一件商品', icon: 'none' });
            return;
        }

        if (!this.data.deliveryDate) {
            wx.showToast({ title: '请选择收货日期', icon: 'none' });
            return;
        }

        wx.showModal({
            title: '确认修改',
            content: '确定要修改订单吗？',
            success: (res) => {
                if (res.confirm) {
                    this.doSubmit();
                }
            }
        });
    },

    // 执行提交
    doSubmit() {
        this.setData({ submitting: true });

        // 构建订单数据
        const orderData = {
            deliveryDate: this.data.deliveryDate,
            items: this.data.orderItems.map(item => ({
                productId: item.productId,
                quantity: item.quantity
            }))
        };

        app.request({
            url: `/shop/orders/${this.data.orderId}`,
            method: 'PUT',
            data: orderData
        })
            .then(data => {
                this.setData({ submitting: false });
                wx.showToast({ title: '修改成功', icon: 'success' });

                setTimeout(() => {
                    wx.navigateBack();
                }, 1500);
            })
            .catch(err => {
                this.setData({ submitting: false });
                wx.showToast({ title: err.message || '修改失败', icon: 'none' });
            });
    },

    // 阻止事件冒泡
    stopPropagation() { }
});
