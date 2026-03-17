// pages/driver/picking/picking.js
const app = getApp();

Page({
  data: {
    pickingList: null,
    loading: false,
    completing: false,
    pendingCount: 0,
    completedCount: 0,
    progressPercent: 0,
    selectedOrders: [],
    showOrderManage: false,
    // 修改订单相关
    showModifyModal: false,
    showProductSelect: false,
    orderDetails: [],
    availableProducts: [],
    selectedOrderIds: [],
    modifyItems: [],
    currentModifyItem: null,
    modifyLoading: false
  },

  onLoad() {
    this.loadPickingList();
    this.loadSelectedOrders();
  },

  onShow() {
    this.loadPickingList();
    this.loadSelectedOrders();
  },

  onPullDownRefresh() {
    Promise.all([this.loadPickingList(), this.loadSelectedOrders()]).then(() => {
      wx.stopPullDownRefresh();
    });
  },

  // 加载拣货清单
  loadPickingList() {
    this.setData({ loading: true });

    return app.request({ url: '/driver/picking-list' })
      .then(data => {
        const items = data && data.items ? data.items : [];
        const pendingCount = items.filter(i => i.status === 0).length;
        const completedCount = items.filter(i => i.status === 1).length;
        const progressPercent = items.length > 0 ? Math.round((completedCount / items.length) * 100) : 0;
        this.setData({
          pickingList: data,
          loading: false,
          pendingCount: pendingCount,
          completedCount: completedCount,
          progressPercent: data && data.allCompleted ? 100 : progressPercent
        });
      })
      .catch(err => {
        this.setData({ loading: false });
        wx.showToast({ title: '加载失败', icon: 'none' });
      });
  },

  // 加载已选订单
  loadSelectedOrders() {
    return app.request({ url: '/driver/selected-orders' })
      .then(data => {
        this.setData({
          selectedOrders: data || []
        });
      })
      .catch(err => {
        console.error('加载已选订单失败', err);
      });
  },

  // 打开/关闭订单管理弹窗
  onToggleOrderManage() {
    this.setData({
      showOrderManage: !this.data.showOrderManage
    });
  },

  // 关闭订单管理弹窗
  onCloseOrderManage() {
    this.setData({
      showOrderManage: false
    });
  },

  // 阻止事件冒泡
  stopPropagation() { },

  // 移除订单
  onRemoveOrder(e) {
    const { id, name } = e.currentTarget.dataset;

    wx.showModal({
      title: '确认移除',
      content: `确定要移除「${name || '该订单'}」吗？移除后订单将返回待分配状态。`,
      success: (res) => {
        if (res.confirm) {
          this.removeOrder(id);
        }
      }
    });
  },

  // 执行移除订单
  removeOrder(orderId) {
    app.request({
      url: `/driver/selected-orders/${orderId}`,
      method: 'DELETE'
    })
      .then(() => {
        wx.showToast({ title: '移除成功', icon: 'success' });
        // 重新加载数据
        this.loadPickingList();
        this.loadSelectedOrders();
      })
      .catch(err => {
        wx.showToast({ title: err || '移除失败', icon: 'none' });
      });
  },

  // 完成拣货
  onCompletePicking(e) {
    const { id, name } = e.currentTarget.dataset;

    wx.showModal({
      title: '确认拣货',
      content: `确定已完成「${name}」的拣货吗？`,
      success: (res) => {
        if (res.confirm) {
          this.submitCompletePicking(id);
        }
      }
    });
  },

  // 提交完成拣货
  submitCompletePicking(pickingItemId) {
    this.setData({ completing: true });

    app.request({
      url: `/driver/picking-list/${pickingItemId}/complete`,
      method: 'PUT'
    })
      .then(() => {
        wx.showToast({ title: '拣货完成', icon: 'success' });
        this.loadPickingList();
      })
      .catch(err => {
        wx.showToast({ title: err || '操作失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ completing: false });
      });
  },

  // 一键完成全部拣货
  onCompleteAll() {
    const { pickingList } = this.data;
    if (!pickingList || !pickingList.items || pickingList.items.length === 0) {
      wx.showToast({ title: '暂无拣货任务', icon: 'none' });
      return;
    }

    const pendingItems = pickingList.items.filter(item => item.status === 0);
    if (pendingItems.length === 0) {
      wx.showToast({ title: '已全部完成', icon: 'none' });
      return;
    }

    wx.showModal({
      title: '确认完成',
      content: `确定将剩余 ${pendingItems.length} 项全部标记为拣货完成吗？`,
      success: (res) => {
        if (res.confirm) {
          this.completeAllItems(pendingItems);
        }
      }
    });
  },

  // 逐个完成拣货项
  completeAllItems(items) {
    let completed = 0;
    let failed = 0;
    const total = items.length;

    const completeNext = (index) => {
      if (index >= total) {
        if (failed === 0) {
          wx.showToast({ title: '全部完成', icon: 'success' });
        } else {
          wx.showToast({
            title: `${completed}项完成，${failed}项失败`,
            icon: 'none'
          });
        }
        this.loadPickingList();
        return;
      }

      app.request({
        url: `/driver/picking-list/${items[index].id}/complete`,
        method: 'PUT'
      })
        .then(() => {
          completed++;
          completeNext(index + 1);
        })
        .catch(() => {
          failed++;
          completeNext(index + 1);
        });
    };

    completeNext(0);
  },

  // ==================== 修改订单功能 ====================

  // 打开修改订单弹窗
  onOpenModifyModal() {
    this.setData({ loading: true });

    // 加载订单详情和可用商品
    Promise.all([
      app.request({ url: '/driver/selected-orders-detail' }),
      app.request({ url: '/driver/available-products' })
    ])
      .then(([orderDetails, availableProducts]) => {
        // 默认选中所有订单
        const selectedOrderIds = orderDetails.map(o => o.id);

        this.setData({
          showModifyModal: true,
          orderDetails: orderDetails || [],
          availableProducts: availableProducts || [],
          selectedOrderIds: selectedOrderIds,
          modifyItems: [],
          loading: false
        });
      })
      .catch(err => {
        this.setData({ loading: false });
        wx.showToast({ title: err || '加载数据失败', icon: 'none' });
      });
  },

  // 关闭修改订单弹窗
  onCloseModifyModal() {
    this.setData({
      showModifyModal: false,
      modifyItems: [],
      currentModifyItem: null
    });
  },

  // 选择/取消选择订单
  onToggleOrderSelect(e) {
    const { id } = e.currentTarget.dataset;
    const { selectedOrderIds } = this.data;

    // 确保 id 是数字类型
    const orderId = typeof id === 'string' ? parseInt(id) : id;

    const index = selectedOrderIds.findIndex(oid => oid === orderId || oid === id);
    if (index > -1) {
      selectedOrderIds.splice(index, 1);
    } else {
      selectedOrderIds.push(orderId);
    }

    this.setData({ selectedOrderIds: [...selectedOrderIds] });
  },

  // 全选/取消全选订单
  onToggleSelectAll() {
    const { orderDetails, selectedOrderIds } = this.data;

    if (selectedOrderIds.length === orderDetails.length) {
      // 取消全选
      this.setData({ selectedOrderIds: [] });
    } else {
      // 全选
      this.setData({ selectedOrderIds: orderDetails.map(o => o.id) });
    }
  },

  // 打开商品选择弹窗
  onOpenProductSelect() {
    this.setData({ showProductSelect: true });
  },

  // 关闭商品选择弹窗
  onCloseProductSelect() {
    this.setData({ showProductSelect: false });
  },

  // 选择商品添加
  onSelectProduct(e) {
    const { product } = e.currentTarget.dataset;

    // 检查是否已添加
    const exists = this.data.modifyItems.find(item => item.productId === product.id && item.type === 'add');
    if (exists) {
      wx.showToast({ title: '该商品已添加', icon: 'none' });
      return;
    }

    // 添加到修改列表
    const modifyItem = {
      productId: product.id,
      productName: product.name,
      quantityChange: 1,
      type: 'add',
      unitPrice: product.oldPrice,
      unit: product.unit
    };

    this.setData({
      modifyItems: [...this.data.modifyItems, modifyItem],
      showProductSelect: false
    });
  },

  // 修改商品数量
  onModifyItemQuantity(e) {
    const { index, delta } = e.currentTarget.dataset;
    const { modifyItems } = this.data;

    const item = modifyItems[index];
    const newQuantity = item.quantityChange + delta;

    if (newQuantity <= 0) {
      // 删除该项
      modifyItems.splice(index, 1);
    } else {
      item.quantityChange = newQuantity;
    }

    this.setData({ modifyItems: [...modifyItems] });
  },

  // 删除修改项
  onRemoveModifyItem(e) {
    const { index } = e.currentTarget.dataset;
    const { modifyItems } = this.data;
    modifyItems.splice(index, 1);
    this.setData({ modifyItems: [...modifyItems] });
  },

  // 从订单商品中减少数量（针对特定订单）
  onReduceOrderItemFromOrder(e) {
    const { orderid, productid, productname, currentqty } = e.currentTarget.dataset;
    this._modifyOrderItemQuantity(orderid, productid, productname, -0.5);
  },

  // 从订单商品中增加数量（针对特定订单）
  onIncreaseOrderItemFromOrder(e) {
    const { orderid, productid, productname, currentqty } = e.currentTarget.dataset;
    this._modifyOrderItemQuantity(orderid, productid, productname, 0.5);
  },

  // 输入框修改数量
  onInputOrderItemQuantity(e) {
    const { orderid, productid, productname, oldqty } = e.currentTarget.dataset;
    const newQty = parseFloat(e.detail.value) || 0;
    const oldQty = parseFloat(oldqty) || 0;
    const diff = newQty - oldQty;

    if (diff !== 0) {
      this._modifyOrderItemQuantity(orderid, productid, productname, diff, true);
    }
  },

  // 输入框修改修改项数量
  onInputModifyItemQuantity(e) {
    const { index } = e.currentTarget.dataset;
    const newQty = parseFloat(e.detail.value) || 0;
    const { modifyItems } = this.data;

    if (newQty <= 0) {
      modifyItems.splice(index, 1);
    } else {
      modifyItems[index].quantityChange = newQty;
    }

    this.setData({ modifyItems: [...modifyItems] });
  },

  // 内部方法：修改订单商品数量
  _modifyOrderItemQuantity(orderId, productId, productName, quantityDiff, isAbsolute = false) {
    const { modifyItems, orderDetails } = this.data;

    // 找到订单信息
    const order = orderDetails.find(o => o.id === orderId);
    const shopName = order ? order.shopName : '';

    // 从订单商品中获取单位
    let unit = '斤';
    if (order && order.items) {
      const orderItem = order.items.find(item => item.productId === productId);
      if (orderItem && orderItem.unit) {
        unit = orderItem.unit;
      }
    }

    // 生成唯一key：订单ID_商品ID
    const itemKey = `${orderId}_${productId}`;

    // 查找是否已有该订单该商品的修改记录
    const existingIndex = modifyItems.findIndex(
      item => item.orderId === orderId && item.productId === productId && item.type === 'modify'
    );

    if (existingIndex > -1) {
      if (isAbsolute) {
        // 绝对值设置
        modifyItems[existingIndex].quantityChange = quantityDiff;
      } else {
        // 增量修改
        modifyItems[existingIndex].quantityChange += quantityDiff;
      }

      // 如果变化为0，删除该记录
      if (Math.abs(modifyItems[existingIndex].quantityChange) < 0.001) {
        modifyItems.splice(existingIndex, 1);
      }
    } else {
      // 新增修改记录
      modifyItems.push({
        orderId: orderId,
        productId: productId,
        productName: productName,
        quantityChange: isAbsolute ? quantityDiff : quantityDiff,
        type: 'modify',
        targetShopName: shopName,
        unit: unit
      });
    }

    this.setData({ modifyItems: [...modifyItems] });
  },

  // 从订单商品中减少数量（旧方法，保留兼容）
  onReduceOrderItem(e) {
    const { productid, productname } = e.currentTarget.dataset;
    const { modifyItems, selectedOrderIds } = this.data;

    // 对所有选中的订单减少该商品数量
    selectedOrderIds.forEach(orderId => {
      this._modifyOrderItemQuantity(orderId, productid, productname, -1);
    });
  },

  // 增加订单商品数量（旧方法，保留兼容）
  onIncreaseOrderItem(e) {
    const { productid, productname } = e.currentTarget.dataset;
    const { modifyItems, selectedOrderIds } = this.data;

    // 对所有选中的订单增加该商品数量
    selectedOrderIds.forEach(orderId => {
      this._modifyOrderItemQuantity(orderId, productid, productname, 1);
    });
  },

  // 提交修改
  onSubmitModify() {
    const { modifyItems } = this.data;

    if (modifyItems.length === 0) {
      wx.showToast({ title: '请添加修改内容', icon: 'none' });
      return;
    }

    this.setData({ modifyLoading: true });

    // 按订单分组修改项
    const modificationsByOrder = {};

    modifyItems.forEach(item => {
      if (item.orderId) {
        // 针对特定订单的修改
        if (!modificationsByOrder[item.orderId]) {
          modificationsByOrder[item.orderId] = [];
        }
        modificationsByOrder[item.orderId].push({
          productId: item.productId,
          productName: item.productName,
          quantityChange: item.quantityChange,
          type: item.type,
          unitPrice: item.unitPrice
        });
      } else if (item.type === 'add') {
        // 新增商品：应用到所有选中的订单
        const { selectedOrderIds } = this.data;
        selectedOrderIds.forEach(orderId => {
          if (!modificationsByOrder[orderId]) {
            modificationsByOrder[orderId] = [];
          }
          modificationsByOrder[orderId].push({
            productId: item.productId,
            productName: item.productName,
            quantityChange: item.quantityChange,
            type: item.type,
            unitPrice: item.unitPrice
          });
        });
      }
    });

    // 检查是否有修改内容
    if (Object.keys(modificationsByOrder).length === 0) {
      wx.showToast({ title: '请选择要修改的订单', icon: 'none' });
      this.setData({ modifyLoading: false });
      return;
    }

    // 逐个订单提交修改
    const promises = Object.entries(modificationsByOrder).map(([orderId, modifications]) => {
      return app.request({
        url: '/driver/modify-order-items',
        method: 'POST',
        data: {
          orderIds: [parseInt(orderId)],
          modifications: modifications
        }
      });
    });

    Promise.all(promises)
      .then(() => {
        wx.showToast({ title: '修改成功', icon: 'success' });
        this.onCloseModifyModal();
        // 刷新拣货清单
        this.loadPickingList();
      })
      .catch(err => {
        wx.showToast({ title: err || '修改失败', icon: 'none' });
      })
      .finally(() => {
        this.setData({ modifyLoading: false });
      });
  },

  // 获取拣货进度
  getProgress() {
    const { pickingList } = this.data;
    if (!pickingList || !pickingList.items || pickingList.items.length === 0) {
      return { total: 0, completed: 0, percent: 0 };
    }

    const total = pickingList.items.length;
    const completed = pickingList.items.filter(item => item.status === 1).length;
    const percent = Math.round((completed / total) * 100);

    return { total, completed, percent };
  }
});
