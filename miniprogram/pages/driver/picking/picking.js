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
    showOrderManage: false
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
