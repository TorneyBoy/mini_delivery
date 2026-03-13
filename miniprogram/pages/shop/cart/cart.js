// pages/shop/cart/cart.js
const app = getApp();

Page({
  data: {
    baseUrl: 'http://localhost:8081', // 后端服务器地址
    cartItems: [],
    totalAmount: '0.00',
    totalCount: 0,
    showPrice: true,
    priceType: 1,
    loading: false
  },

  onLoad() {
    this.loadShopInfo();
  },

  onShow() {
    this.loadCartData();
  },

  // 加载店铺信息
  loadShopInfo() {
    app.request({ url: '/shop/info' })
      .then(data => {
        if (data) {
          this.setData({
            showPrice: data.showPrice === 1,
            priceType: data.priceType || 1
          });
        }
      })
      .catch(err => {
        console.error('加载店铺信息失败', err);
      });
  },

  // 加载购物车数据
  loadCartData() {
    // 从全局获取购物车数据
    const cartItems = app.globalData.cartItems || [];
    let totalAmount = 0;
    let totalCount = 0;

    cartItems.forEach(item => {
      totalAmount += item.quantity * parseFloat(item.unitPrice);
      totalCount += item.quantity;
    });

    this.setData({
      cartItems,
      totalAmount: totalAmount.toFixed(2),
      totalCount
    });
  },

  // 增加数量
  onPlus(e) {
    const index = e.currentTarget.dataset.index;
    const cartItems = [...this.data.cartItems];
    cartItems[index].quantity += 1;
    this.updateCart(cartItems);
  },

  // 减少数量
  onMinus(e) {
    const index = e.currentTarget.dataset.index;
    const cartItems = [...this.data.cartItems];
    if (cartItems[index].quantity > 1) {
      cartItems[index].quantity -= 1;
    } else {
      // 数量为0时删除
      cartItems.splice(index, 1);
    }
    this.updateCart(cartItems);
  },

  // 输入数量
  onInputQuantity(e) {
    const index = e.currentTarget.dataset.index;
    const value = parseFloat(e.detail.value) || 0;
    const cartItems = [...this.data.cartItems];

    if (value > 0) {
      cartItems[index].quantity = value;
    } else {
      cartItems.splice(index, 1);
    }
    this.updateCart(cartItems);
  },

  // 删除商品
  onRemove(e) {
    const index = e.currentTarget.dataset.index;
    const cartItems = [...this.data.cartItems];
    cartItems.splice(index, 1);
    this.updateCart(cartItems);
  },

  // 清空购物车
  onClearCart() {
    wx.showModal({
      title: '提示',
      content: '确定要清空购物车吗？',
      success: (res) => {
        if (res.confirm) {
          this.updateCart([]);
        }
      }
    });
  },

  // 更新购物车
  updateCart(cartItems) {
    let totalAmount = 0;
    let totalCount = 0;

    cartItems.forEach(item => {
      totalAmount += item.quantity * parseFloat(item.unitPrice);
      totalCount += item.quantity;
    });

    // 保存到全局
    app.globalData.cartItems = cartItems;

    this.setData({
      cartItems,
      totalAmount: totalAmount.toFixed(2),
      totalCount
    });
  },

  // 去结算
  goToPreview() {
    if (this.data.cartItems.length === 0) {
      wx.showToast({ title: '购物车为空', icon: 'none' });
      return;
    }

    // 保存到全局
    app.globalData.orderItems = this.data.cartItems.map(item => ({
      productId: item.productId,
      productName: item.productName,
      quantity: item.quantity,
      unitPrice: item.unitPrice,
      unit: item.unit,
      imageUrl: item.imageUrl
    }));
    app.globalData.orderAmount = this.data.totalAmount;

    wx.navigateTo({ url: '/pages/shop/order-preview/order-preview' });
  },

  // 继续购物
  goShopping() {
    wx.switchTab({ url: '/pages/shop/index/index' });
  }
});
