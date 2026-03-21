// pages/shop/browse/browse.js
const app = getApp();

Page({
    data: {
        branchManagerId: null,
        brandName: '',
        categories: [],
        products: [],
        currentCategory: '',
        page: 1,
        size: 10,
        hasMore: true,
        loading: false,
        cart: {}, // 游客购物车 {productId: quantity}
        totalCount: 0,
        totalAmount: '0.00',
        isLoggedIn: false
    },

    onLoad(options) {
        const branchManagerId = options.branchManagerId;
        const brandName = decodeURIComponent(options.brandName || '供应商');

        if (!branchManagerId) {
            wx.showToast({ title: '参数错误', icon: 'none' });
            setTimeout(() => wx.navigateBack(), 1500);
            return;
        }

        // 检查登录状态
        const isLoggedIn = !!app.globalData.token;

        this.setData({
            branchManagerId: parseInt(branchManagerId),
            brandName,
            isLoggedIn
        });

        // 设置页面标题
        wx.setNavigationBarTitle({ title: brandName });

        // 加载数据
        this.loadCategories();
        this.loadProducts(true);
    },

    onShow() {
        // 每次显示时检查登录状态
        const isLoggedIn = !!app.globalData.token;
        if (isLoggedIn !== this.data.isLoggedIn) {
            this.setData({ isLoggedIn });
        }
    },

    // 加载分类
    loadCategories() {
        wx.request({
            url: `${app.globalData.baseUrl}/guest/categories`,
            method: 'GET',
            data: { branchManagerId: this.data.branchManagerId },
            success: (res) => {
                if (res.data.code === 200) {
                    this.setData({ categories: res.data.data || [] });
                }
            },
            fail: (err) => {
                console.error('加载分类失败', err);
            }
        });
    },

    // 加载商品
    loadProducts(refresh = false) {
        if (this.data.loading) return;
        if (!refresh && !this.data.hasMore) return;

        const page = refresh ? 1 : this.data.page;

        this.setData({ loading: true });

        wx.request({
            url: `${app.globalData.baseUrl}/guest/products`,
            method: 'GET',
            data: {
                branchManagerId: this.data.branchManagerId,
                page,
                size: this.data.size,
                // 只有选择了具体分类时才传递category参数
                ...(this.data.currentCategory ? { category: this.data.currentCategory } : {})
            },
            success: (res) => {
                if (res.data.code === 200) {
                    const data = res.data.data;
                    const products = (data.records || []).map(p => ({
                        ...p,
                        quantity: this.data.cart[p.id] || 0,
                        imageUrl: app.getImageUrl(p.imageUrl)
                    }));

                    const newProducts = refresh ? products : [...this.data.products, ...products];

                    this.setData({
                        products: newProducts,
                        page: page + 1,
                        hasMore: products.length === this.data.size,
                        loading: false
                    });

                    this.updateCartSummary();
                } else {
                    this.setData({ loading: false });
                    wx.showToast({ title: res.data.message || '加载失败', icon: 'none' });
                }
            },
            fail: () => {
                this.setData({ loading: false });
                wx.showToast({ title: '网络错误', icon: 'none' });
            }
        });
    },

    // 切换分类
    onCategoryChange(e) {
        const category = e.currentTarget.dataset.category;
        // 如果点击的是"全部"，将currentCategory设为空字符串
        this.setData({
            currentCategory: category || '',
            products: [],
            page: 1,
            hasMore: true
        });
        this.loadProducts(true);
    },

    // 增加数量
    onPlus(e) {
        const id = e.currentTarget.dataset.id;
        const cart = { ...this.data.cart };
        cart[id] = (cart[id] || 0) + 1;
        this.updateCart(cart);
    },

    // 减少数量
    onMinus(e) {
        const id = e.currentTarget.dataset.id;
        const cart = { ...this.data.cart };
        if (cart[id] > 0) {
            cart[id] -= 1;
            if (cart[id] === 0) {
                delete cart[id];
            }
        }
        this.updateCart(cart);
    },

    // 输入数量
    onInputQuantity(e) {
        const id = e.currentTarget.dataset.id;
        const value = parseFloat(e.detail.value) || 0;
        const cart = { ...this.data.cart };

        if (value > 0) {
            cart[id] = value;
        } else {
            delete cart[id];
        }
        this.updateCart(cart);
    },

    // 更新购物车
    updateCart(cart) {
        // 更新商品数量显示
        const products = this.data.products.map(p => ({
            ...p,
            quantity: cart[p.id] || 0
        }));

        this.setData({ cart, products });
        this.updateCartSummary();

        // 保存到本地存储（游客模式）
        this.saveCartToStorage(cart);
    },

    // 更新购物车汇总
    updateCartSummary() {
        const { cart, products } = this.data;
        let totalCount = 0;
        let totalAmount = 0;

        products.forEach(p => {
            const qty = cart[p.id] || 0;
            if (qty > 0) {
                totalCount += qty;
                totalAmount += qty * parseFloat(p.price);
            }
        });

        this.setData({
            totalCount,
            totalAmount: totalAmount.toFixed(2)
        });
    },

    // 保存购物车到本地存储
    saveCartToStorage(cart) {
        const cartData = {
            branchManagerId: this.data.branchManagerId,
            brandName: this.data.brandName,
            items: [],
            updateTime: Date.now()
        };

        // 转换为数组格式
        this.data.products.forEach(p => {
            const qty = cart[p.id] || 0;
            if (qty > 0) {
                cartData.items.push({
                    productId: p.id,
                    productName: p.name,
                    quantity: qty,
                    unitPrice: p.price,
                    unit: p.unit,
                    imageUrl: p.imageUrl
                });
            }
        });

        wx.setStorageSync('guestCart', cartData);
    },

    // 去结算
    goToCheckout() {
        if (this.data.totalCount === 0) {
            wx.showToast({ title: '请先选择商品', icon: 'none' });
            return;
        }

        // 检查登录状态
        if (!app.globalData.token) {
            wx.showModal({
                title: '提示',
                content: '下单功能需要登录后使用。\n\n登录后您可以：\n· 提交订单\n· 查看订单状态\n· 接收配送通知',
                confirmText: '去登录',
                cancelText: '取消',
                success: (res) => {
                    if (res.confirm) {
                        wx.navigateTo({
                            url: '/pages/login/login'
                        });
                    }
                }
            });
            return;
        }

        // 已登录，准备订单数据并跳转
        const cartItems = [];
        this.data.products.forEach(p => {
            const qty = this.data.cart[p.id] || 0;
            if (qty > 0) {
                cartItems.push({
                    productId: p.id,
                    productName: p.name,
                    quantity: qty,
                    unitPrice: p.price,
                    unit: p.unit,
                    imageUrl: p.imageUrl
                });
            }
        });

        app.globalData.cartItems = cartItems;
        app.globalData.orderAmount = this.data.totalAmount;

        wx.navigateTo({
            url: '/pages/shop/order-preview/order-preview'
        });
    },

    // 去登录
    goToLogin() {
        wx.navigateTo({
            url: '/pages/login/login'
        });
    },

    // 下拉刷新
    onPullDownRefresh() {
        this.loadProducts(true);
        wx.stopPullDownRefresh();
    },

    // 触底加载更多
    onReachBottom() {
        this.loadProducts(false);
    }
});
