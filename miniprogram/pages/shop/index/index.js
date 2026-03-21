// pages/shop/index/index.js
const app = getApp();

Page({
    data: {
        shopInfo: null,
        categories: [],
        products: [],
        currentCategory: '',
        page: 1,
        size: 10,
        hasMore: true,
        loading: false,
        cart: {}, // {productId: quantity}
        totalCount: 0,
        totalAmount: '0.00',
        showPrice: true, // 是否显示单价
        priceType: 1 // 价格类型：1-老用户价 2-新用户价
    },

    onLoad() {
        // 店铺首页需要登录才能访问（显示个性化价格和购物车）
        // 如果未登录，跳转到欢迎页
        if (!app.checkLogin()) {
            wx.reLaunch({ url: '/pages/welcome/welcome' });
            return;
        }
        this.loadShopInfo();
        this.loadCategories();
        this.loadProducts(true);
    },

    onShow() {
        // 检查登录状态
        if (!app.globalData.token) {
            return;
        }
        // 刷新购物车数据
        this.updateCartSummary();
    },

    // 加载店铺信息（获取价格显示设置）
    loadShopInfo() {
        app.request({ url: '/shop/info' })
            .then(data => {
                if (data) {
                    this.setData({
                        shopInfo: data,
                        showPrice: data.showPrice === 1,
                        priceType: data.priceType || 1
                    });
                }
            })
            .catch(err => {
                console.error('加载店铺信息失败', err);
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

    // 加载商品
    loadProducts(refresh = false) {
        if (this.data.loading) return;
        if (!refresh && !this.data.hasMore) return;

        const page = refresh ? 1 : this.data.page;

        this.setData({ loading: true });

        app.request({
            url: '/shop/products',
            data: {
                page,
                size: this.data.size,
                category: this.data.currentCategory
            }
        })
            .then(data => {
                const products = data.records || [];
                // 根据价格类型设置显示价格
                products.forEach(p => {
                    p.displayPrice = this.data.priceType === 1 ? p.oldPrice : p.newPrice;
                    p.quantity = this.data.cart[p.id] || 0;
                    // 处理图片URL
                    if (p.imageUrl) {
                        p.imageUrl = app.getImageUrl(p.imageUrl);
                    }
                });

                const newProducts = refresh ? products : [...this.data.products, ...products];

                this.setData({
                    products: newProducts,
                    page: page + 1,
                    hasMore: products.length === this.data.size,
                    loading: false
                });

                this.updateCartSummary();
            })
            .catch(err => {
                this.setData({ loading: false });
                wx.showToast({ title: '加载失败', icon: 'none' });
            });
    },

    // 切换分类
    onCategoryChange(e) {
        const category = e.currentTarget.dataset.category;
        this.setData({
            currentCategory: category,
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
        const products = this.data.products.map(p => ({
            ...p,
            quantity: cart[p.id] || 0
        }));
        this.setData({ cart, products });
        this.updateCartSummary();
    },

    // 更新购物车汇总
    updateCartSummary() {
        let totalCount = 0;
        let totalAmount = 0;

        this.data.products.forEach(p => {
            if (this.data.cart[p.id]) {
                totalCount += this.data.cart[p.id];
                totalAmount += this.data.cart[p.id] * parseFloat(p.displayPrice || p.newPrice);
            }
        });

        this.setData({
            totalCount,
            totalAmount: totalAmount.toFixed(2)
        });
    },

    // 加载更多
    loadMore() {
        this.loadProducts();
    },

    // 去预览订单
    goToPreview() {
        const selectedItems = [];
        this.data.products.forEach(p => {
            if (this.data.cart[p.id]) {
                selectedItems.push({
                    productId: p.id,
                    productName: p.name,
                    quantity: this.data.cart[p.id],
                    unitPrice: p.displayPrice || p.newPrice,
                    unit: p.unit,
                    imageUrl: p.imageUrl
                });
            }
        });

        if (selectedItems.length === 0) {
            wx.showToast({ title: '请先选择商品', icon: 'none' });
            return;
        }

        // 保存到全局
        app.globalData.orderItems = selectedItems;
        app.globalData.orderAmount = this.data.totalAmount;

        wx.navigateTo({ url: '/pages/shop/order-preview/order-preview' });
    },

    // 下拉刷新
    onPullDownRefresh() {
        this.setData({
            products: [],
            page: 1,
            hasMore: true
        });
        this.loadProducts(true);
        wx.stopPullDownRefresh();
    },

    // 跳转到购物车
    goToCart() {
        wx.navigateTo({ url: '/pages/shop/cart/cart' });
    },

    // 跳转到订单列表
    goToOrderList() {
        wx.redirectTo({ url: '/pages/shop/order-list/order-list' });
    },

    // 跳转到账单列表
    goToBillList() {
        wx.redirectTo({ url: '/pages/shop/bill-list/bill-list' });
    },

    // 跳转到个人中心
    goToProfile() {
        wx.redirectTo({ url: '/pages/shop/profile/profile' });
    },

    // 图片加载失败处理
    onImageError(e) {
        console.log('图片加载失败', e);
    }
});
